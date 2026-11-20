// KIND: STANDALONE
// MODULE: UnboundGenerics
// FILE: Flow.kt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

// Every flow element is unboxed through `__createBridgeable`, then cast to the Swift element type.

sealed class SealedBase
class SealedLeaf(val id: Int) : SealedBase()

fun flowOfAny(): Flow<Any> = flowOf("a", 1, listOf(2))
fun <T> flowOfRepeatedT(x: T): Flow<T> = flowOf(x, x)
fun flowListStr(): Flow<List<String>> = flowOf(listOf("a", "b"))
fun flowOfStringSet(): Flow<Set<String>> = flowOf(setOf("a", "b"))
fun flowOfStringToIntMap(): Flow<Map<String, Int>> = flowOf(mapOf("a" to 1))
fun flowSealed(): Flow<SealedBase> = flowOf(SealedLeaf(3))
fun flowNothing(): Flow<Nothing> = emptyFlow()

// FILE: StateFlow.kt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// `StateFlow.value` and `replayCache` are unboxed through `__createBridgeable` too.

private val state = MutableStateFlow("init")
fun stateFlow(): StateFlow<String> = state
fun setState(value: String) { state.value = value }
fun stateNothing(): StateFlow<Nothing?> = MutableStateFlow(null)
