// RUN_PIPELINE_TILL: BACKEND

// MODULE: dep2

import androidx.compose.runtime.*

@ComposableTargetMarker(description = "U")
@Target(AnnotationTarget.TYPE)
annotation class UComposable()

@ComposableTargetMarker(description = "V")
@Target(AnnotationTarget.TYPE)
annotation class VComposable()

@ComposableTargetMarker(description = "W")
@Target(AnnotationTarget.TYPE)
annotation class WComposable()

@Composable
fun UVWWrapper(content: @Composable @UComposable @VComposable @WComposable () -> Unit) {
    content()
}

// MODULE: dep1

import androidx.compose.runtime.*

@ComposableTargetMarker(description = "X")
@Target(AnnotationTarget.TYPE)
annotation class XComposable()

@ComposableTargetMarker(description = "Y")
@Target(AnnotationTarget.TYPE)
annotation class YComposable()

@ComposableTargetMarker(description = "Z")
@Target(AnnotationTarget.TYPE)
annotation class ZComposable()

@Composable
fun XYZWrapper(content: @Composable @XComposable @YComposable @ZComposable () -> Unit) {
    content()
}

// MODULE: main(dep1, dep2)

import androidx.compose.runtime.Composable

@Composable fun XYZInUVW() {
    UVWWrapper {
        <!COMPOSE_APPLIER_CALL_MISMATCH!>XYZWrapper<!> {}
    }
}

@Composable fun UVWInXYZ() {
    XYZWrapper {
        <!COMPOSE_APPLIER_CALL_MISMATCH!>UVWWrapper<!> {}
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, functionalType, lambdaLiteral, stringLiteral */
