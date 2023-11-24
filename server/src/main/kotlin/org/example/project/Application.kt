package org.example.project

import Greeting
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun main() {
    runBlocking {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind("10.0.2.15", 9002)
        println("Server is listening at ${serverSocket.localAddress}")
        val clients = mutableSetOf<io.ktor.network.sockets.Socket>()

        while (true) {
            val socket = serverSocket.accept()
            clients.add(socket)
            launch {
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                try {
                    while (true) {
                        val line = receiveChannel.readUTF8Line() ?: break
                        clients.forEach {
                            it.openWriteChannel(autoFlush = true).writeStringUtf8("$line\n")
                        }
                    }
                } finally {
                    clients.remove(socket)
                    withContext(Dispatchers.IO) {
                        socket.close()
                    }
                }
            }
        }
    }
}


fun Application.module() {
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
    }
}
