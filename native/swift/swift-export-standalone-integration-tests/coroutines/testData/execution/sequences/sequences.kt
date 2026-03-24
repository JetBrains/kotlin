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

class CurrentSubject {
    public val mutableStateFlow: MutableStateFlow<Elem> = MutableStateFlow(Element1)

    public val stateFlow: StateFlow<Elem> get() = mutableStateFlow

    public val mutableSharedFlow: MutableSharedFlow<Elem> = MutableSharedFlow(3)

    public val sharedFlow: SharedFlow<Elem> get() = mutableSharedFlow
}

fun testRegular(): Flow<Elem> = flowOf(Element1, Element2, Element3)

fun testNullable(): Flow<Elem?> = flowOf(Element1, null, Element2, null, Element3)

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

class TrackedFlow {

    private val _count = MutableStateFlow(0)
    public val count: Int get() = _count.value

    public val flow: Flow<Elem> = MutableStateFlow(Element1).onStart {
        _count.update { it + 1 }
    }.onCompletion {
        _count.update { it - 1 }
    }
}

suspend fun testCollect(flow: Flow<Elem>, count: Int): List<Elem> = flow.take(count).toList()

fun testUpdateValue(flow: MutableStateFlow<Elem>, value: Elem) {
    flow.value = value
}
