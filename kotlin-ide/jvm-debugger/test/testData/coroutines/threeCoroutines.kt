package threeCoroutines

import kotlin.random.Random

suspend fun main() {
    sequence {
        yield(239)
        sequence {
            //Breakpoint!
            yield(666)
        }.toList()
    }.toList()
}
