package darkkeks.pixel2020

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.StringWriter
import javax.script.ScriptEngineManager
import javax.script.ScriptException

inline fun <reified T> createLogger(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}


private const val PREFIX = """window = {
    Math: Math,
    parseInt: parseInt,
    location: { 
        host: "https://vk.com" 
    },
    WebSocket: {kek:1}
};"""


fun evaluateJS(script: String): String? {
    val engine = ScriptEngineManager().getEngineByName("javascript")
    val context = engine.context
    val writer = StringWriter()
    context.writer = writer

    try {
        val obj = engine.eval(PREFIX + script)
        return if (obj is Double) {
            obj.toInt().toString()
        } else obj.toString()
    } catch (e: ScriptException) {
        throw IllegalStateException(script)
    }
}
