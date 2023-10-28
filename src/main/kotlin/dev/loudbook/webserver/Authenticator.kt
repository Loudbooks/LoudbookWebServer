package dev.loudbook.webserver

import com.sun.net.httpserver.HttpExchange
import java.io.FileInputStream

object Authenticator {
    private val token: String = FileInputStream("./token.txt").bufferedReader().readLine()

    fun authenticate(exchange: HttpExchange): Boolean {
        val providedToken: String = exchange.requestHeaders.getFirst("token")

        if (providedToken != token) {
            exchange.sendResponseHeaders(401, 0)
            exchange.responseBody.use { os -> os.write("Unauthorized".encodeToByteArray()) }
            return false
        }

        return true
    }
}