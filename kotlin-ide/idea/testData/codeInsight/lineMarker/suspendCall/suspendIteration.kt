fun coroutine(block: suspend () -> Unit) {}

class SIter {
    operator fun iterator() = this
    suspend operator fun hasNext(): Boolean = false
    suspend operator fun next(): Int = 0
    suspend fun test() {}
}

fun foo() {
    val iter = SIter()
    coroutine {
        iter.<lineMarker descr="Suspend function call">test</lineMarker>() // this line is marked (LINE1)
        for (x in <lineMarker descr="Suspending iteration">iter</lineMarker>) // this line is not (LINE2)
            println(x)
    }
}