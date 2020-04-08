fun <T> runBlocking(<warning descr="[UNUSED_PARAMETER] Parameter 'block' is never used">block</warning>: suspend CoroutineScope.() -> T): T = TODO()

class CoroutineScope

fun test() {
    runBlocking {
        repeat(1) {
            java.lang.Thread.<warning descr="Inappropriate blocking method call">sleep</warning>(2)
        }
    }
}