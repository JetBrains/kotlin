
import javax.script.*

inline fun<T> foo(body: () -> T): T = body()

fun main() {
    val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")!!
    scriptEngine.eval("println(foo { \"OK\" })")
}
