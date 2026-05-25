import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibilityScope

@Composable
fun <S> AnimatedContent(
    targetState: S,
    content: @Composable AnimatedVisibilityScope.(targetState: S) -> Unit
) { }
