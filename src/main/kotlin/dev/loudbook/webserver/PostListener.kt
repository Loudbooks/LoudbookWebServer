package dev.loudbook.webserver

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path

class PostListener(private val root: Path) : HttpHandler {
    private val token: String = FileInputStream("./token.txt").bufferedReader().readLine()

    override fun handle(exchange: HttpExchange?) {
        exchange ?: return

        println("Request method: ${exchange.requestMethod}")

        if (!exchange.requestMethod.equals("POST")) return

        val filename: String = exchange.requestHeaders.getFirst("filename")
        val providedToken: String = exchange.requestHeaders.getFirst("token")

        if (providedToken != token) {
            exchange.sendResponseHeaders(401, 0)
            exchange.responseBody.use { os -> os.write("Unauthorized".encodeToByteArray()) }
            return
        }

        val path = root.resolve(filename)

        Files.newOutputStream(path).use { os -> exchange.requestBody.transferTo(os) }

        val response = "File uploaded successfully: $filename"

        println(response)

        exchange.sendResponseHeaders(200, response.encodeToByteArray().size.toLong())
        exchange.responseBody.use { os -> os.write(response.encodeToByteArray()) }
    }
}