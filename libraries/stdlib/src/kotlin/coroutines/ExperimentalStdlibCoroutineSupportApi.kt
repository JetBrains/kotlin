package kotlin.coroutines

import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation marks the experimental additions to the standard library API related to coroutines.
 *
 * > Beware using the annotated API especially if you're developing a library, since your library might become binary incompatible
 * with the future versions of the standard library.
 *
 * Any usage of a declaration annotated with `@ExperimentalStdlibCoroutineSupportApi` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalStdlibCoroutineSupportApi::class)`,
 * or by using the compiler argument `-opt-in=kotlin.coroutines.ExperimentalStdlibCoroutineSupportApi`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(
    CLASS,
    ANNOTATION_CLASS,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPEALIAS
)
@MustBeDocumented
@SinceKotlin("2.4")
public annotation class ExperimentalStdlibCoroutineSupportApi
