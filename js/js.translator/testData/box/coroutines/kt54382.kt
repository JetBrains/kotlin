// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^^ JS target doesn't support both function and suspend function types as supertypes

typealias AsyncFun = suspend () -> Unit
val actions = arrayListOf<AsyncFun>()

private class SyncFunAdapter(private val target: () -> Unit) : AsyncFun {
    override fun equals(other: Any?): Boolean {
        return if (other is SyncFunAdapter) {
            target == other.target
        } else {
            target == other
        }
    }

    override fun hashCode() = target.hashCode()

    override suspend fun invoke() {
        target()
    }
}

fun box(): String {
    val x = SyncFunAdapter { }
    actions.add(x)
    if (!actions.remove(x)) return "FAIL 1"

    return "OK"
}