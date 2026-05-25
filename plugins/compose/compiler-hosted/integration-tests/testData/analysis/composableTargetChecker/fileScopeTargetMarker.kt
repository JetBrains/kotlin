// RUN_PIPELINE_TILL: FRONTEND

@file: NComposable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTarget
import androidx.compose.runtime.ComposableTargetMarker

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "An N Composable")
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
fun AssumesN() {
    <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
}
