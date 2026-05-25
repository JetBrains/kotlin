// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

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

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "An M Composable")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class MComposable()

@Composable @NComposable fun N() { }
@Composable @MComposable fun M() { }

@Composable fun NWrapper(content: @NComposable @Composable () -> Unit) {
     content()
}

@Composable fun MWrapper(content: @MComposable @Composable () -> Unit) {
     content()
}

@Composable fun T() {
    MWrapper {
        <!COMPOSE_APPLIER_CALL_MISMATCH!>NWrapper<!> {
            <!COMPOSE_APPLIER_CALL_MISMATCH!>N<!>()
        }
    }
}
