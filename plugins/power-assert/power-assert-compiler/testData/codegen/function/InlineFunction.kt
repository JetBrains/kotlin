// DUMP_KT_IR
// WITH_COROUTINES
import helpers.*

fun box(): String = runAll(
    "test1" to { test1() },
)

suspend fun getFalse() = false

fun test1() {
    runBlocking {
        assert(run { getFalse() })
    }
}
