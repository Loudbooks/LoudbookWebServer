package dev.loudbook.webserver

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class MinecraftRedirectListener(private val root: Path) : HttpHandler {
    private val links = mutableMapOf<String, String>()

    init {
        updateLinks()
    }

    override fun handle(exchange: HttpExchange?) {
        exchange ?: return

        if (!exchange.requestMethod.equals("POST")) {
            val redirectUrl = links[exchange.requestURI.toString().replace("/mcredirect", "")] ?: return

            val redirectResponse =
                "<html><body>Redirecting to <a href=\"$redirectUrl\">$redirectUrl</a></body></html>"

            exchange.responseHeaders.set("Location", redirectUrl)
            exchange.sendResponseHeaders(302, redirectResponse.length.toLong())

            val os = exchange.responseBody
            os.write(redirectResponse.toByteArray())

            os.close()
            exchange.close()
        } else {
            try {
                println("Received upload request from ${exchange.remoteAddress}")

                if (!Authenticator.authenticate(exchange)) {
                    println("Rejected upload attempt from ${exchange.remoteAddress}")
                    return
                }

                val filename = "links.txt"

                val path = root.resolve("mc/").resolve(filename)

                if (path.exists()) {
                    path.toFile().delete()

                    println("Deleted old links.txt file")
                }

                Files.newOutputStream(path).use { os ->
                    exchange.requestBody.transferTo(os)
                    updateLinks()
                }

                val response = "File uploaded successfully: $filename"

                exchange.sendResponseHeaders(200, response.encodeToByteArray().size.toLong())
                exchange.responseBody.use { os -> os.write(response.encodeToByteArray()) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateLinks() {
        val fileContents = Files.readAllLines(root.resolve("mc/").resolve("links.txt"))

        for (fileContent in fileContents) {
            val key = fileContent.split("\":\"")[0].replace("\"", "")
            val value = fileContent.split("\":\"")[1].replace("\"", "")

            links[key] = value
            println("Loaded link: $key -> $value")
        }
    }
}