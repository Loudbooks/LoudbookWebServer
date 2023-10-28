package dev.loudbook.webserver

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.SimpleFileServer
import java.net.InetSocketAddress
import java.nio.file.Path


class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val address = InetSocketAddress("0.0.0.0", 8081)

            val root: Path = Path.of("/home/loudbook/")

            SimpleFileServer.createFileServer(address, root, SimpleFileServer.OutputLevel.VERBOSE).start()

            val uploadAddress = InetSocketAddress("0.0.0.0", 8082)

            val httpServer = HttpServer.create(uploadAddress, 0)
            httpServer.createContext("/mcredirect", MinecraftRedirectListener(root))
            httpServer.createContext("/upload", PostListener(root))

            httpServer.start()

            println("Server started on port 8081 and 8082")
        }
    }
}