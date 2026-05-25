// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTarget
import androidx.compose.runtime.ComposableTargetMarker

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "N")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class NComposable()

@Composable @ComposableTarget("M") fun M() {}

@Composable
@NComposable
fun AssumesN() {
    <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
}
