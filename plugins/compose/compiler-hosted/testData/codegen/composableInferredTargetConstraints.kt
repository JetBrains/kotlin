// DUMP_IR

// MODULE: main
import androidx.compose.runtime.*

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "A W Composable")
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
)
annotation class WComposable

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "An X Composable")
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
)
annotation class XComposable

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "A Y Composable")
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
)
annotation class YComposable

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "A Z Composable")
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
)
annotation class ZComposable

@Composable @WComposable @XComposable fun WX() { }

@Composable
fun CallWX(content: @Composable @YComposable @ZComposable () -> Unit):
        @Composable @WComposable @XComposable (
            content: @Composable @YComposable @ZComposable () -> Unit,
        ) -> Unit
{
    WX()
    return {}
}

@Composable
fun CallContent(content: @Composable @WComposable @XComposable () -> Unit):
        @Composable @ComposableOpenTarget(1) @YComposable @ZComposable (
            content: @Composable @ComposableOpenTarget(1) @YComposable @ZComposable () -> Unit,
        ) -> Unit
{
    content()
    return {}
}
