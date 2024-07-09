// RUN_PIPELINE_TILL: BACKEND

// MODULE: dep2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTargetMarker

@ComposableTargetMarker("UI")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class UiComposable

@Composable @UiComposable fun Ui() {}

@Composable fun UiContent(content: @Composable @UiComposable () -> Unit) {
    content()
}


// MODULE: dep1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTargetMarker

@ComposableTargetMarker("Vector")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class VectorComposable

@Composable @VectorComposable fun Vector() {}

@Composable fun VectorContent(content: @Composable @VectorComposable () -> Unit) {
    content()
}

// MODULE: main(dep1, dep2)
import androidx.compose.runtime.Composable

@Composable fun App() {
    Ui()
    <!COMPOSE_APPLIER_CALL_MISMATCH!>Vector<!>()
}

@Composable fun VecInUi() {
    UiContent {
        <!COMPOSE_APPLIER_CALL_MISMATCH!>VectorContent<!> {
            <!COMPOSE_APPLIER_CALL_MISMATCH!>Ui<!>()
        }
    }
}

@Composable fun UiInVec() {
    VectorContent {
        <!COMPOSE_APPLIER_CALL_MISMATCH!>UiContent<!> {
            <!COMPOSE_APPLIER_CALL_MISMATCH!>Ui<!>()
        }
    }
}
