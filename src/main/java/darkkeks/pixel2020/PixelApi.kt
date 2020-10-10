package darkkeks.pixel2020

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.awt.Color
import java.awt.image.BufferedImage
import java.net.URI
import java.net.URISyntaxException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture


const val FIELD_HEIGHT = 400
const val FIELD_WIDTH = 1590

const val PIXEL_COUNT = FIELD_HEIGHT * FIELD_WIDTH

fun checkRange(x: Int, y: Int) = x >= 0 && y >= 0 && x < FIELD_WIDTH && y < FIELD_HEIGHT

object Colors {
    val PALETTE = listOf(
        "#FFFFFF", "#C2C2C2", "#858585", "#474747", "#000000", "#3AAFFF", "#71AAEB", "#4a76a8", "#074BF3",
        "#5E30EB", "#FF6C5B", "#FE2500", "#FF218B", "#99244F", "#4D2C9C", "#FFCF4A", "#FEB43F", "#FE8648",
        "#FF5B36", "#DA5100", "#94E044", "#5CBF0D", "#C3D117", "#FCC700", "#D38301"
    )

    val RGB_MAP: List<Color>
    val COLOR_TO_ID: Map<Color, Int>
    val RGB_TO_COLOR: Map<Int, Color>

    init {
        RGB_MAP = PALETTE.map { color -> hex2rgb(color) }
        COLOR_TO_ID = RGB_MAP
            .mapIndexed { index, color -> color to index }
            .toMap()
        RGB_TO_COLOR = RGB_MAP
            .map { color -> color.rgb to color }
            .toMap()
    }

    private fun hex2rgb(colorStr: String) = Color(
        Integer.valueOf(colorStr.substring(1, 3), 16),
        Integer.valueOf(colorStr.substring(3, 5), 16),
        Integer.valueOf(colorStr.substring(5, 7), 16)
    )

    fun charToColor(c: Char): Color {
        return if (c in '0'..'9') {
            RGB_MAP[c - '0']
        } else {
            RGB_MAP[c - 'a' + 10]
        }
    }
}

data class Credentials(val signature: String)

data class ConnectionData(val wsUrl: String, val dataUrl: String)

class PixelApi(private val loginSignature: String, private val client: HttpClient) {

    private fun <T> makeRequest(
        endpoint: String,
        handler: BodyHandler<T>
    ): CompletableFuture<HttpResponse<T>> {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI(API_URL + endpoint))
                .header("X-vk-sign", loginSignature)
                .build()
            client.sendAsync(request, handler)
        } catch (e: URISyntaxException) {
            throw RuntimeException("Can't make api request", e)
        }
    }

    fun start(): CompletableFuture<ConnectionData> {
        return makeRequest(
            "/start", HttpResponse.BodyHandlers.ofString()
        ).thenApply { response ->
            println(response.body())
            val result = JsonParser.parseString(response.body()).asJsonObject
            if (result.has("error")) {
                throw ApiException(result)
            }
            result["response"].asJsonObject
        }.thenApply { data ->
            val wsUrl = data["url"].asString
            val dataUrl = data["data"].asString
            ConnectionData(wsUrl, dataUrl)
        }
    }

    fun data(): CompletableFuture<BufferedImage> {
        val hour = LocalDateTime.now().hour
        val minute = LocalDateTime.now().minute
        return makeRequest(
            "/data" + String.format("?ts=%d-%d", hour, minute),
            HttpResponse.BodyHandlers.ofString()
        ).thenApply { response: HttpResponse<String> ->
            val imageData = response.body().substring(0, PIXEL_COUNT)
            val frozen: String = response.body().substring(PIXEL_COUNT)
            val result = BufferedImage(FIELD_WIDTH, FIELD_HEIGHT, BufferedImage.TYPE_INT_RGB)
            for (i in 0 until PIXEL_COUNT) {
                val x: Int = i % FIELD_WIDTH
                val y: Int = i / FIELD_WIDTH
                result.setRGB(x, y, Colors.charToColor(imageData[i]).rgb)
            }
            result
        }
    }

    companion object {
        const val API_URL = "https://pixel-dev.w84.vkforms.ru/api"
    }
}


class ApiException(private val obj: JsonObject) : RuntimeException("Api exception occurred") {
    override fun toString() = obj.toString()
}
