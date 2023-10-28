package dev.loudbook.webserver

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class MinecraftRedirectListener(private val root: Path) : HttpHandler {
    private val links = mutableMapOf<String, String>()
    private val authenticator = Authenticator()

    init {
        updateLinks()
    }

    override fun handle(exchange: HttpExchange?) {
        exchange ?: return

        if (!exchange.requestMethod.equals("POST")) {
            val redirectUrl = links[exchange.requestURI.toString().replace("/mcredirect", "")] ?: run {

                val invalidResponse ="""
                <head>
                <title>Loudbook's Redirects</title>
                <meta content="Invalid Redirect" property="og:title" />
                <meta content="Please ensure you are using the right key." property="og:description" />
                <meta content="#FF0000" data-react-helmet="true" name="theme-color" />
                </head>
                <body>This redirect is invalid!</body>
            """.trimIndent()

                exchange.responseHeaders.set("Content-Type", "text/html")

                exchange.sendResponseHeaders(200, invalidResponse.length.toLong()) // We send 200 to force discord to show an embed.
                exchange.responseBody.use { os -> os.write(invalidResponse.encodeToByteArray()) }

                return
            }

            val redirectResponse = """
                <head>
                <title>Loudbook's Redirects</title>
                <meta content="Loudbook's Redirects" property="og:title" />
                <meta content="This redirects to $redirectUrl" property="og:description" />
                <meta content="$redirectUrl" property="og:url" />
                <meta content="$redirectUrl" property="og:image" />
                <meta content="#2e3035" data-react-helmet="true" name="theme-color" />
                <body>Rerouting to <a href="$redirectUrl">$redirectUrl</a></body>
                </head>
            """.trimIndent()

            exchange.responseHeaders.set("Content-Type", "text/html")
            exchange.responseHeaders.set("Location", redirectUrl)
            exchange.sendResponseHeaders(302, redirectResponse.length.toLong())

            val os = exchange.responseBody
            os.write(redirectResponse.toByteArray())

            os.close()
            exchange.close()
        } else {
            try {
                println("Received upload request from ${exchange.remoteAddress}")

                if (!authenticator.authenticate(exchange)) return

                val filename = "links.txt"
                val path = root.resolve("mc/").resolve(filename)

                if (path.exists()) {
                    path.toFile().delete()

                    println("Deleted old links.txt file")
                }

                Files.newOutputStream(path).use { os ->
                    exchange.requestBody.transferTo(os)
                }

                val response = "File uploaded successfully: $filename"

                exchange.sendResponseHeaders(200, response.encodeToByteArray().size.toLong())
                exchange.responseBody.use { os -> os.write(response.encodeToByteArray()) }

                updateLinks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateLinks() {
        val fileContents = Files.readAllLines(root.resolve("mc/").resolve("links.txt"))

        for (fileContent in fileContents) {
            val key = fileContent.split("\":\"")[0].replace("\"", "").trim()
            val value = fileContent.split("\":\"")[1].replace("\"", "").trim()

            links[key] = value
            println("Loaded link: $key -> $value")
        }
    }
}