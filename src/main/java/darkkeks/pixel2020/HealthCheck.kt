package darkkeks.pixel2020

import java.util.concurrent.ConcurrentHashMap

class HealthCheck {

    private val toConfirm: MutableMap<Pixel, Credentials> = ConcurrentHashMap()

    fun onPixel(pixel: Pixel) {
        toConfirm.remove(pixel)
    }

    fun onPlace(pixel: Pixel, credentials: Credentials) {
        toConfirm[pixel] = credentials
    }

    fun checkHealth(pixel: Pixel): Boolean {
        val credentials = toConfirm.remove(pixel)
        if (credentials != null) {
            println("Unhealthy account! $credentials")
            return false
        }
        return true
    }

}
