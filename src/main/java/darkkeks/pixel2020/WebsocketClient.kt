package darkkeks.pixel2020

import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.Future
import javax.websocket.*

interface MessageHandler {
    fun handleMessage(message: String)
    fun handleBinaryMessage(buffer: ByteArray)
    fun onClose()
}

@ClientEndpoint
class WebsocketClient(private val endpoint: String, private val handler: MessageHandler) {

    val logger = createLogger<WebsocketClient>()

    private var session: Session? = null

    fun connect() {
        val container = ContainerProvider.getWebSocketContainer()
        session = container.connectToServer(this, URI(endpoint))
    }

    @OnOpen
    fun onOpen(userSession: Session?) {
        logger.info("On open")
        this.session = userSession
    }

    @OnClose
    fun onClose(userSession: Session, reason: CloseReason) {
        logger.info("On close")
        handler.onClose()
        this.session = null
    }

    @OnMessage
    fun onMessage(message: String) {
        handler.handleMessage(message)
    }

    @OnMessage
    fun onBinaryMessage(b: ByteArray) {
        handler.handleBinaryMessage(b)
    }

    fun sendBinary(buffer: ByteBuffer): Future<Void> {
        val s = session ?: throw IllegalStateException("No session")
        return s.asyncRemote.sendBinary(buffer)
    }

    fun close() {
        session?.close()
    }
}
