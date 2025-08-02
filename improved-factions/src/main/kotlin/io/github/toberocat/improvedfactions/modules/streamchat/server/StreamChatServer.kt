
package io.github.toberocat.improvedfactions.modules.streamchat.server

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.modules.streamchat.auth.TokenManager
import io.github.toberocat.improvedfactions.modules.streamchat.message.ChatMessage
import org.bukkit.ChatColor
import java.time.Instant
import java.io.*
import java.net.*
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.*
import kotlin.concurrent.thread

class StreamChatServer(private val plugin: ImprovedFactionsPlugin) {
    private var serverSocket: ServerSocket? = null
    private val port = 8090
    private val tokenManager = TokenManager()
    private val activeConnections = ConcurrentHashMap<String, WebSocketConnection>()
    private val executorService = Executors.newCachedThreadPool()
    private var isRunning = false
    private var mainThread: Thread? = null
    private val healthCheckExecutor = Executors.newSingleThreadScheduledExecutor()
    private val playerConnections = ConcurrentHashMap<UUID, WebSocketConnection>()

    companion object {
        private const val PING_INTERVAL = 15000L // 15 segundos
        private const val CONNECTION_TIMEOUT = 30000 // 30 segundos
        private const val SOCKET_READ_TIMEOUT = 45000 // 45 segundos
        private const val MAX_MESSAGE_SIZE = 1024 * 64 // 64KB
    }

    data class WebSocketConnection(
        val socket: Socket,
        val userId: String,
        @Volatile var lastActivity: Long = System.currentTimeMillis(),
        @Volatile var isAlive: Boolean = true,
        val outputLock: Any = Object()
    )

    fun generateUserToken(userId: String): String = tokenManager.generateToken(userId)

    fun start() {
        if (isRunning) return

        try {
            serverSocket = ServerSocket(port).apply {
                reuseAddress = true
                soTimeout = 1000 // 1 segundo timeout para accept()
            }
            isRunning = true
            plugin.logger.info("Chat server iniciado en puerto $port")

            mainThread = thread(start = true) {
                while (isRunning && !Thread.currentThread().isInterrupted) {
                    try {
                        val client = serverSocket?.accept()
                        if (client != null) {
                            client.soTimeout = SOCKET_READ_TIMEOUT
                            executorService.submit { handleClient(client) }
                        }
                    } catch (e: SocketTimeoutException) {
                        // Timeout normal, continuar
                    } catch (e: Exception) {
                        if (isRunning) {
                            plugin.logger.warning("Error aceptando cliente: ${e.message}")
                        }
                    }
                }
            }

            startHealthChecks()
        } catch (e: Exception) {
            plugin.logger.severe("Error iniciando servidor de chat: ${e.message}")
            stop()
        }
    }

    private fun startHealthChecks() {
        healthCheckExecutor.scheduleAtFixedRate({
            try {
                val now = System.currentTimeMillis()
                activeConnections.forEach { (token, conn) ->
                    if (now - conn.lastActivity > CONNECTION_TIMEOUT) {
                        if (conn.isAlive) {
                            try {
                                sendPing(conn)
                            } catch (e: Exception) {
                                closeConnection(token, "Timeout de conexión")
                            }
                        } else {
                            closeConnection(token, "Conexión inactiva")
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("Error en health check: ${e.message}")
            }
        }, PING_INTERVAL, PING_INTERVAL, TimeUnit.MILLISECONDS)
    }

    private fun handleClient(clientSocket: Socket) {
        var token: String? = null
        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val writer = PrintWriter(clientSocket.getOutputStream(), true)

            val firstLine = reader.readLine() ?: return
            token = extractTokenFromUrl(firstLine)

            if (token == null || !tokenManager.validateToken(token)) {
                sendHttpError(writer, 401, "Token inválido o expirado")
                return
            }

            val userId = tokenManager.getUserIdFromToken(token) ?: return
            val playerUUID = UUID.fromString(userId)

            // Cerrar conexiones existentes si hay alguna
            playerConnections[playerUUID]?.let {
                closeConnection(token, "Nueva conexión reemplaza la anterior")
            }

            if (!processWebSocketHandshake(reader, writer)) {
                return
            }

            val connection = WebSocketConnection(clientSocket, userId)
            activeConnections[token] = connection
            playerConnections[playerUUID] = connection

            handleWebSocketConnection(connection, token)

        } catch (e: Exception) {
            plugin.logger.warning("Error en handleClient: ${e.message}")
        } finally {
            token?.let {
                val userId = tokenManager.getUserIdFromToken(it)
                if (userId != null) {
                    playerConnections.remove(UUID.fromString(userId))
                }
                closeConnection(it, "Conexión cerrada")
            }
        }
    }

    // Método para verificar si un jugador tiene una conexión activa
    fun hasActiveConnection(playerUUID: UUID): Boolean {
        return playerConnections.containsKey(playerUUID)
    }

    private fun closeConnection(token: String, reason: String) {
        val connection = activeConnections.remove(token)
        if (connection != null) {
            try {
                connection.isAlive = false
                connection.socket.close()
                plugin.logger.info("Conexión cerrada ($token) - Razón: $reason")
            } catch (e: Exception) {
                plugin.logger.warning("Error al cerrar conexión ($token): ${e.message}")
            }
        }
    }

    private fun handleWebSocketConnection(connection: WebSocketConnection, token: String) {
        try {
            val input = connection.socket.getInputStream()
            val buffer = ByteArray(MAX_MESSAGE_SIZE)

            while (connection.isAlive && !connection.socket.isClosed) {
                try {
                    val read = input.read(buffer)
                    if (read == -1) break

                    connection.lastActivity = System.currentTimeMillis()

                    val message = decodeWebSocketFrame(buffer, read)
                    when {
                        message == "ping" -> sendPong(connection)
                        message?.isNotEmpty() == true -> broadcastMessage(connection, message)
                    }
                } catch (e: SocketTimeoutException) {
                    // Timeout normal, verificar conexión
                    if (System.currentTimeMillis() - connection.lastActivity > CONNECTION_TIMEOUT) {
                        throw IOException("Conexión inactiva")
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error en WebSocket ($token): ${e.message}")
        } finally {
            closeConnection(token, "Conexión WebSocket terminada")
        }
    }

    private fun broadcastMessage(connection: WebSocketConnection, message: String) {
        plugin.server.scheduler.runTask(plugin, Runnable {
            try {
                val player = plugin.server.getPlayer(UUID.fromString(connection.userId))
                if (player != null) {
                    // Intentar parsear como ChatMessage
                    val chatMessage = ChatMessage.fromJson(message)
                    if (chatMessage != null) {
                        // Es un mensaje en formato JSON
                        // Solo enviar al jugador específico
                        player.sendMessage(formatMessage(chatMessage))
                    } else {
                        // Es un mensaje normal
                        val formattedMessage = createChatMessage(player.name, message)
                        // Solo enviar al jugador específico
                        player.sendMessage(formatMessage(formattedMessage))

                        // Enviar el mensaje formateado de vuelta al WebSocket
                        sendMessageToWebSocket(connection, formattedMessage.toJson())
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("Error al enviar mensaje: ${e.message}")
            }
        })
    }

    private fun createChatMessage(username: String, message: String): ChatMessage {
        val badges = mutableListOf<String>()
        val player = plugin.server.getPlayer(username)

        // Determinar color del jugador (por ejemplo, basado en permisos o rango)
        val userColor = when {
            player?.isOp == true -> "#FF5555"
            player?.hasPermission("factions.streamchat.moderator") == true -> "#1475E1"
            else -> "#FFFFFF"
        }

        // Determinar badges
        if (player?.isOp == true) badges.add("admin")
        if (player?.hasPermission("factions.streamchat.moderator") == true) badges.add("moderator")

        return ChatMessage(
            username = username,
            userColor = userColor,
            badges = badges,
            message = message,
            time = Instant.now().toString(),
            type = "message"
        )
    }

    private fun formatMessage(chatMessage: ChatMessage): String {
        val badges = chatMessage.badges.joinToString(" ") { "[$it]" }
        val badgeStr = if (badges.isNotEmpty()) "$badges " else ""

        // Convertir el color hex a color más cercano de Minecraft
        val colorCode = convertHexToMinecraftColor(chatMessage.userColor)

        // Usar el mensaje limpio
        return "$badgeStr$colorCode${chatMessage.username}§r: ${chatMessage.getCleanMessage()}"
    }

    private fun convertHexToMinecraftColor(hexColor: String): String {
        return try {
            val color = hexColor.removePrefix("#")
            val rgb = color.toInt(16)
            val r = (rgb shr 16) and 0xFF
            val g = (rgb shr 8) and 0xFF
            val b = rgb and 0xFF

            // Mapear al color más cercano de Minecraft
            when {
                r > 200 && g < 100 && b < 100 -> "§c" // Rojo
                r > 200 && g > 200 && b < 100 -> "§e" // Amarillo
                r < 100 && g > 200 && b < 100 -> "§a" // Verde
                r < 100 && g > 200 && b > 200 -> "§b" // Aqua
                r < 100 && g < 100 && b > 200 -> "§9" // Azul
                r > 200 && g < 100 && b > 200 -> "§d" // Light Purple
                r > 200 && g > 100 && b < 100 -> "§6" // Gold
                r < 100 && g < 100 && b < 100 -> "§8" // Dark Gray
                r > 200 && g > 200 && b > 200 -> "§f" // White
                else -> "§7" // Gray (default)
            }
        } catch (e: Exception) {
            "§7" // Gray como fallback
        }
    }

    private fun sendMessageToWebSocket(connection: WebSocketConnection, message: String) {
        synchronized(connection.outputLock) {
            try {
                val messageBytes = message.toByteArray()
                val frameBytes = createWebSocketFrame(messageBytes)
                connection.socket.getOutputStream().write(frameBytes)
                connection.socket.getOutputStream().flush()
            } catch (e: Exception) {
                connection.isAlive = false
            }
        }
    }

    private fun createWebSocketFrame(payload: ByteArray): ByteArray {
        val length = payload.size
        val frameBytes = when {
            length <= 125 -> {
                ByteArray(2 + length).apply {
                    this[0] = 0x81.toByte()  // FIN + text frame
                    this[1] = length.toByte() // Length
                    System.arraycopy(payload, 0, this, 2, length)
                }
            }
            length <= 65535 -> {
                ByteArray(4 + length).apply {
                    this[0] = 0x81.toByte()  // FIN + text frame
                    this[1] = 126.toByte()   // Length indicator
                    this[2] = (length shr 8).toByte()  // Length high byte
                    this[3] = (length and 0xFF).toByte()  // Length low byte
                    System.arraycopy(payload, 0, this, 4, length)
                }
            }
            else -> throw IllegalArgumentException("Mensaje demasiado largo")
        }
        return frameBytes
    }


    private fun sendPing(connection: WebSocketConnection) {
        synchronized(connection.outputLock) {
            try {
                val pingFrame = byteArrayOf(
                    0x89.toByte(),
                    0x00.toByte()
                )
                connection.socket.getOutputStream().write(pingFrame)
                connection.socket.getOutputStream().flush()
            } catch (e: Exception) {
                connection.isAlive = false
                throw e
            }
        }
    }

    private fun sendPong(connection: WebSocketConnection) {
        synchronized(connection.outputLock) {
            try {
                val pongFrame = byteArrayOf(
                    0x8A.toByte(),
                    0x00.toByte()
                )
                connection.socket.getOutputStream().write(pongFrame)
                connection.socket.getOutputStream().flush()
            } catch (e: Exception) {
                connection.isAlive = false
            }
        }
    }

    private fun processWebSocketHandshake(reader: BufferedReader, writer: PrintWriter): Boolean {
        try {
            val headers = mutableMapOf<String, String>()
            var line: String?

            while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                val parts = line!!.split(":", limit = 2)
                if (parts.size == 2) {
                    headers[parts[0].trim().lowercase()] = parts[1].trim()
                }
            }

            if (!headers.containsKey("sec-websocket-key")) {
                sendHttpError(writer, 400, "Falta Sec-WebSocket-Key")
                return false
            }

            val acceptKey = generateAcceptKey(headers["sec-websocket-key"]!!)
            writer.print("HTTP/1.1 101 Switching Protocols\r\n")
            writer.print("Upgrade: websocket\r\n")
            writer.print("Connection: Upgrade\r\n")
            writer.print("Sec-WebSocket-Accept: $acceptKey\r\n")
            writer.print("\r\n")
            writer.flush()

            return true
        } catch (e: Exception) {
            plugin.logger.warning("Error en handshake WebSocket: ${e.message}")
            return false
        }
    }

    private fun extractTokenFromUrl(firstLine: String): String? {
        val regex = "token=([^\\s&]+)".toRegex()
        return regex.find(firstLine)?.groupValues?.get(1)
    }

    private fun generateAcceptKey(webSocketKey: String): String {
        val GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val concatenated = webSocketKey + GUID
        val sha1 = MessageDigest.getInstance("SHA-1")
        return Base64.getEncoder().encodeToString(sha1.digest(concatenated.toByteArray()))
    }

    private fun decodeWebSocketFrame(buffer: ByteArray, length: Int): String? {
        if (length < 2) return null

        val isFinalFragment = (buffer[0].toInt() and 0x80) != 0
        val opcode = buffer[0].toInt() and 0x0F
        val isMasked = (buffer[1].toInt() and 0x80) != 0
        var payloadLength = buffer[1].toInt() and 0x7F
        var maskingKeyOffset = 2

        if (!isMasked || opcode != 1) return null

        if (payloadLength == 126) {
            if (length < 4) return null
            payloadLength = ((buffer[2].toInt() and 0xFF) shl 8) or (buffer[3].toInt() and 0xFF)
            maskingKeyOffset = 4
        } else if (payloadLength == 127) {
            return null // No manejamos mensajes tan largos
        }

        if (length < maskingKeyOffset + 4 + payloadLength) return null

        val maskingKey = ByteArray(4)
        System.arraycopy(buffer, maskingKeyOffset, maskingKey, 0, 4)

        val payload = ByteArray(payloadLength)
        for (i in 0 until payloadLength) {
            payload[i] = (buffer[maskingKeyOffset + 4 + i].toInt() xor maskingKey[i % 4].toInt()).toByte()
        }

        return String(payload)
    }

    private fun sendHttpError(writer: PrintWriter, code: Int, message: String) {
        writer.print("HTTP/1.1 $code $message\r\n")
        writer.print("Content-Type: text/plain\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print("$code - $message")
        writer.flush()
    }

    fun stop() {
        isRunning = false
        mainThread?.interrupt()

        // Cerrar todas las conexiones activas
        activeConnections.keys.toList().forEach {
            closeConnection(it, "Servidor detenido")
        }

        // Cerrar executor services
        executorService.shutdown()
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }

        healthCheckExecutor.shutdown()
        try {
            healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            healthCheckExecutor.shutdownNow()
        }

        // Cerrar servidor
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            plugin.logger.warning("Error cerrando servidor: ${e.message}")
        }

        serverSocket = null
    }

    fun disconnectPlayer(playerUUID: UUID) {
        playerConnections[playerUUID]?.let { connection ->
            activeConnections.entries
                .find { it.value == connection }
                ?.key
                ?.let { token ->
                    closeConnection(token, "Desconexión manual del jugador")
                }
            playerConnections.remove(playerUUID)
        }
    }
}