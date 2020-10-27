package codegen.basics.k42000_1

import kotlin.test.*

@Test
fun runTest() {
    assertTrue(Reproducer().repro() > 0)
}

// Based on https://youtrack.jetbrains.com/issue/KT-42000#focus=Comments-27-4404934.0-0

val Int.isEven get() = this % 2 == 0

inline operator fun <reified T : Number> T.plus(other: T): T = when (T::class) {
    Double::class -> (this as Double) + (other as Double)
    Int::class -> (this as Int) + (other as Int)
    Long::class -> (this as Long) + (other as Long)
    else -> TODO()
} as T

inline fun <reified T : Number> Collection<T>.median(): Double {
    val sorted = this.sortedBy {
        it.toDouble()
    }

    return if (size.isEven || size == 1) {
        sorted[size / 2]
    } else {
        sorted[size / 2] + sorted[size / 2 + 1]
    }.toDouble()
}

class Reproducer {
    private var someListOfLongs = mutableListOf<Long>(1L)

    fun repro() = someListOfLongs.median()
}
