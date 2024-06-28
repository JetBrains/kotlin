// KJS_WITH_FULL_RUNTIME
import kotlin.js.Promise

public class MyPromise(executor: ((Unit) -> Unit, (Throwable) -> Unit) -> Unit) : Promise<Unit>(executor)

fun box() = "OK"
