// KIND: STANDALONE
// MODULE: SealedSuspend
// FILE: sealedSuspend.kt

import kotlinx.coroutines.delay

// A `suspend fun` returning a sealed type: it is exported as `async throws`, and the awaited value
// still has to be matchable with the generated accessor.

sealed class LoadState {
    data object Loading : LoadState()

    data class Success(val data: String) : LoadState()

    data class Failure(val reason: String) : LoadState()
}

suspend fun loadSuccess(): LoadState {
    delay(1)
    return LoadState.Success("loaded")
}

suspend fun loadFailure(): LoadState {
    delay(1)
    return LoadState.Failure("boom")
}

suspend fun loadNullable(empty: Boolean): LoadState? {
    delay(1)
    return if (empty) null else LoadState.Loading
}

suspend fun consume(state: LoadState): String {
    delay(1)
    return "consumed:$state"
}
