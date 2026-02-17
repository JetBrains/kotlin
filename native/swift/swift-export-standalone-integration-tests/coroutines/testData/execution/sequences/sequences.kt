// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// MODULE: Main
// FILE: sequences.kt

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

abstract class Elem

object Element1: Elem()
object Element2: Elem()
object Element3: Elem()

object CurrentSubject {
    private val flow: MutableStateFlow<Elem> = MutableStateFlow(Element1)

    public val value: Flow<Elem> get() = flow

    public fun update(value: Elem) {
        flow.value = value
    }
}

fun testRegular(): Flow<Elem> = flowOf(Element1, Element2, Element3)

fun testEmpty(): Flow<Elem> = flowOf()

fun testString(): Flow<String> = flowOf("hello", "any", "world")

fun testList(): Flow<List<Int>> = flowOf(listOf(1), listOf(2), listOf(3))

fun testPrimitive(): Flow<UInt> = flowOf(1u, 2u, 3u)

fun testFailing(): Flow<Elem> = flow {
    emit(Element1)
    emit(Element2)
    error("Flow has Failed")
}

fun testDiscarding(): Flow<Elem> = flow {
    emit(Element1)
    emit(Element2)
    emit(Element3)
    error("Flow has to be discarded")
}
