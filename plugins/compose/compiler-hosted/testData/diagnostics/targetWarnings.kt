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

// MODULE: main(dep1, dep2)
import androidx.compose.runtime.Composable

@Composable fun VectorContent(content: @Composable @VectorComposable () -> Unit) {
    content()
}

@Composable fun UiContent(content: @Composable @UiComposable () -> Unit) {
    content()
}

@Composable fun App() {
    Ui()
    <!COMPOSE_APPLIER_CALL_MISMATCH!>Vector<!>()
}

@Composable fun VecInUi() {
    UiContent {
        VectorContent {
            <!COMPOSE_APPLIER_CALL_MISMATCH!>Ui<!>()
        }
    }
}

@Composable fun UiInVec() {
    VectorContent {
        UiContent {
            Ui()
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, functionalType, lambdaLiteral, stringLiteral */
