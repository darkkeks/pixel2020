package darkkeks.pixel2020

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.net.http.HttpClient
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class BotHandler(
    val credentials: Credentials, httpClient: HttpClient
) : MessageHandler {

    private val logger = createLogger<BotHandler>()

    val pixelApi = PixelApi(credentials.signature, httpClient)
    private var client: WebsocketClient? = null

    private var ttl = 0
    private var wait = 0

    var pixelHandler: ((Pixel) -> Unit)? = null
    var closeHandler: (() -> Unit)? = null

    val canPlace: Boolean get() {
        println(wait)
        return wait <= 0
    }

    fun tick() {
        if (wait > 0) {
            wait -= 1000
        }
    }

    fun start(): CompletableFuture<ConnectionData> {
        return pixelApi.start()
    }

    fun connect(): CompletableFuture<Void> {
        return start().thenAccept { data ->
            logger.info("Start received")
            client = WebsocketClient(data.wsUrl + credentials.signature, this)
            client?.connect()
        }
    }

    fun sendPixel(pixel: Pixel): Future<Void> {
        if (!canPlace) error("Wait > 0: $wait")

        logger.info("Sending pixel x={}, y={}", pixel.x, pixel.y)

        val buffer = ByteBuffer.allocate(4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(pixel.pack())
        buffer.flip()

        wait = ttl

        val c = client ?: error("No client ?!")
        return c.sendBinary(buffer)
    }

    override fun handleMessage(message: String) {
        logger.info("Handling message {}", message)
        when (message) {
            "DOUBLE_CONNECT" -> {
                logger.info("Double connect. Reconnecting")
                client?.close()
            }
            "restart" -> {
                logger.info("Server asked for a restart :)")
                client?.close()
            }
            else -> {
                handleMessage(JsonParser.parseString(message));
            }
        }
    }

    private fun handleMessage(message: JsonElement) {
        logger.info("Handling message {}", message)
        val obj = message.asJsonObject

        val value = obj.get("v")
        val type = obj.get("t").asInt
        when (type) {
            2 -> {
                val result = value.asJsonObject
                if (result.has("wait")) {
                    wait = result["wait"].asInt
                }
                if (result.has("ttl")) {
                    ttl = result["ttl"].asInt
                }
            }
            3 -> {
                logger.info("Server asked for a restart :)")
                client?.close()
            }
            12 -> {
                value.asJsonArray.forEach {
                    handleMessage(it)
                }
            }
            else -> {
                logger.warn("Unknown message {}", message)
            }
        }
    }

    override fun handleBinaryMessage(buffer: ByteArray) {
        val message = ByteBuffer.wrap(buffer)
        message.order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until buffer.size / 3 / 4) {
            val pixel = unpack(message.int, message.int, message.int)
            pixelHandler?.invoke(pixel)
        }
    }

    override fun onClose() {
        closeHandler?.invoke()
    }
}
