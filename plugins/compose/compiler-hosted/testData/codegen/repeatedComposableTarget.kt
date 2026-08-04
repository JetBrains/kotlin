// DUMP_IR

// MODULE: main
import androidx.compose.runtime.*

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "A W Composable")
@Target(AnnotationTarget.FUNCTION)
annotation class WComposable

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "An X Composable")
@Target(AnnotationTarget.FUNCTION)
annotation class XComposable

@Composable @WComposable @XComposable fun WX() { }

@Composable
fun CallWX() {
    WX()
}
