// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// MODULE: Main
// FILE: sequences.kt

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object Element1
object Element2
object Element3

object CurrentSubject {
    private val flow: MutableStateFlow<Any> = MutableStateFlow(Element1)

    public val value: Flow<Any> get() = flow

    public fun update(value: Any) {
        flow.value = value
    }
}

fun testRegular(): Flow<Any> = flowOf(Element1, Element2, Element3)

fun testEmpty(): Flow<Any> = flowOf()

fun testFailing(): Flow<Any> = flow {
    emit(Element1)
    emit(Element2)
    error("Flow has Failed")
}

fun testDiscarding(): Flow<Any> = flow {
    emit(Element1)
    emit(Element2)
    emit(Element3)
    error("Flow has to be discarded")
}
