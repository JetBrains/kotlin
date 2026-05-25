@file:NComposable

import androidx.compose.runtime.*

@ComposableTargetMarker(description = "An N Composable")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class NComposable()

@Composable fun N() { }
