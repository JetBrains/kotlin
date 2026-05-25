private fun contentOf(
    loading: @Composable ((State.Loading) -> Unit)?,
    success: @Composable ((State.Success) -> Unit)?,
    error: @Composable ((State.Error) -> Unit)?,
): @Composable () -> Unit {
    return if (loading != null || success != null || error != null) {
        {
            var draw = true
            when (val state = getState()) {
                is State.Loading -> if (loading != null) loading(state).also { draw = false }
                is State.Success -> if (success != null) success(state).also { draw = false }
                is State.Error -> if (error != null) error(state).also { draw = false }
                is State.Empty -> {} // Skipped if rendering on the main thread.
            }
            if (draw) SubcomposeAsyncImageContent()
        }
    } else {
        { SubcomposeAsyncImageContent() }
    }
}

fun used(x: Any?) {}
