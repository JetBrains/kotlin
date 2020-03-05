package kotlin

import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation marks the standard library API that is considered experimental and is not subject to the
 * [general compatibility guarantees](https://kotlinlang.org/docs/reference/evolution/components-stability.html) given for the standard library:
 * the behavior of such API may be changed or the API may be removed completely in any further release.
 *
 * > Beware using the annotated API especially if you're developing a library, since your library might become binary incompatible
 * with the future versions of the standard library.
 *
 * Any usage of a declaration annotated with `@ExperimentalStdlibApi` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalStdlibApi::class)`,
 * or by using the compiler argument `-Xopt-in=kotlin.ExperimentalStdlibApi`.
 */
@Suppress("DEPRECATION")
@Experimental(level = Experimental.Level.ERROR)
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
@ExperimentalStdlibApi
@SinceKotlin("1.3")  // TODO: Remove experimental status from itself and advance SinceKotlin to 1.4
public annotation class ExperimentalStdlibApi
