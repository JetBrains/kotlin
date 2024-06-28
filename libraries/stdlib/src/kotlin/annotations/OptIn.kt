/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

/**
 * Signals that the annotated annotation class is a marker of an API that requires an explicit opt-in.
 *
 * Call sites of any declaration that is either annotated with such a marker or mentions in its signature any
 * other declaration that requires opt-in should opt in to the API either by using [OptIn],
 * or by being annotated with that marker themselves, effectively causing further propagation of the opt-in requirement.
 *
 * The intended uses of opt-in markers include, but are not limited to the following:
 * - Experimental API for public preview that might change its semantics or affect binary compatibility.
 * - Internal declarations that should not be used outside the declaring library, but are `public` for technical reasons.
 * - Fragile or delicate API that needs a lot of expertise to use and thus require an explicit opt-in.
 *
 * ## Contagiousness
 *
 * When a declaration is marked with an opt-in requirement, it is considered to be contagious, meaning that all its uses
 * or mentions in other declarations will require an explicit opt-in.
 * A rule of thumb for propagating is the following: if the marked declaration ceases to exist, only
 * the places with explicit opt-in (or the corresponding warning) will break. This rule does not imply transitivity,
 * e.g. the propagation does not propagate opt-in through inlining, making it the responsibility `inline` function author
 * to mark it properly.
 *
 * ### Type scopes
 *
 * A type is considered requiring opt-in if it is marked with an opt-in marker, or the outer declaration (class or interface) requires opt-in.
 * Any use of any declaration that mentions such type in its signature will require an explicit opt-in, even if it is not used
 * directly on the call site, and even if such declarations do not require opt-in directly.
 *
 * For example, consider the following declarations that are marked with non-propagating opt-in:
 * ```
 * @UnstableApi
 * class Unstable
 *
 * @OptIn(UnstableApi::class)
 * fun foo(): Unstable = Unstable()
 *
 * @OptIn(UnstableApi::class)
 * fun bar(arg: Unstable = Unstable()) {}
 *
 * @OptIn(UnstableApi::class)
 * fun Unstable?.baz() {}
 * ```
 * and their respective call sites:
 * ```
 * fun outerFun() {
 *     val s = foo()
 *     bar()
 *     null.baz()
 * }
 * ```
 * Even though call sites do not mention `Unstable` type directly, the corresponding opt-in warning or error will be triggered
 * in each call site due to propagation contagiousness. Note that the propagation is not transitive, i.e. calls to `outerFun`
 * itself would not trigger any further opt-in requirements.
 *
 * ### Lexical scopes
 *
 * If a type requires an opt-in, such requirement is propagated to its lexical scope and all its nested declarations.
 * For example, for the following scope:
 * ```
 * @UnstableApi
 * class Unstable {
 *     fun memberFun() = ...
 *
 *     class NestedClass {
 *         fun nestedFun() = ...
 *     }
 * }
 * ```
 *
 * Any use of `Unstable`, `NestedClass`, or their member functions will require an explicit opt-in.
 *
 * ### Overridden declarations
 *
 * Opt-in markers are also propagated through the inheritance and interface implementation.
 * If the base declaration requires an opt-in, overriding it requires either an explicit opt-in or
 * propagating the opt-in requirement.
 *
 * See also [Kotlin language documentation](https://kotlinlang.org/docs/opt-in-requirements.html) for more information.
 *
 * @property message message to be reported on usages of API without an explicit opt-in, or empty string for the default message.
 *                   The default message is: "This declaration is experimental and its usage should be marked with 'Marker'
 *                   or '@OptIn(Marker::class)'", where `Marker` is the opt-in requirement marker.
 * @property level specifies how usages of API without an explicit opt-in are reported in code.
 */
@Target(ANNOTATION_CLASS)
@Retention(BINARY)
@SinceKotlin("1.3")
public annotation class RequiresOptIn(
    val message: String = "",
    val level: Level = Level.ERROR
) {
    /**
     * Severity of the diagnostic that should be reported on usages which did not explicitly opted into
     * the API either by using [OptIn] or by being annotated with the corresponding marker annotation.
     */
    public enum class Level {
        /** Specifies that a warning should be reported on incorrect usages of this API. */
        WARNING,

        /** Specifies that a compilation error should be reported on incorrect usages of this API. */
        ERROR,
    }
}

/**
 * Allows to use the API denoted by the given markers in the annotated file, declaration, or expression.
 * If a declaration is annotated with [OptIn], its usages are **not** required to opt in to that API.
 *
 * [markerClass] specifies marker annotations that require explicit opt-in. The marker annotation is
 * not required to be itself marked with [RequiresOptIn] to enable gradual migration of API from requiring opt-in to the regular one,
 * yet declaring such `OptIn` yields a compilation warning.
 *
 * See also [Kotlin language documentation](https://kotlinlang.org/docs/opt-in-requirements.html) for more information.
 *
 * @property markerClass specifies marker annotations that require explicit opt-in.
 * @see RequiresOptIn for a detailed description of opt-in semantics and propagation rules.
 */
@Target(
    CLASS, PROPERTY, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, EXPRESSION, FILE, TYPEALIAS
)
@Retention(SOURCE)
@SinceKotlin("1.3")
public annotation class OptIn(
    vararg val markerClass: KClass<out Annotation>
)

/**
 * This annotation marks the experimental preview of the language feature [SubclassOptInRequired].
 *
 * > Note that this API is in a preview state and has a chance of being changed in the future.
 * Do not use it if you develop a library since your library can become source incompatible
 * with the future versions of Kotlin.
 */
@Target(CLASS)
@Retention(BINARY)
@SinceKotlin("1.8")
@RequiresOptIn
public annotation class ExperimentalSubclassOptIn

/**
 * Annotation that marks open for subclassing classes and interfaces, and makes implementation
 * and extension of such declarations as requiring an explicit opt-in.
 *
 * When applied, any attempt to subclass the target declaration will trigger an opt-in
 * with the corresponding level and message.
 *
 * The intended uses of subclass opt-in markers include, but are not limited to the following API:
 * - Stable to use, but unstable to implement due to its further evolution.
 * - Stable to use, but closed for 3rd-part implementations due to internal or technical reasons.
 * - Stable to use, but delicate or fragile to implement.
 * - Stable to use, but with a contract that may be weakened in the future in a backwards-incompatible
 *   manner for external implementations.
 *
 * Contrary to regular [RequiresOptIn], there are three ways to opt-in into the subclassing requirement:
 * - Annotate declaration with the marker annotation, making it propagating.
 * - Annotate declaration with [OptIn] in order to opt in into the provided guarantees in a non-propagating manner.
 * - Annotate declaration with [SubclassOptInRequired] with the same marker class, making it further propagating only for subclassing.
 *
 * Uses of this annotation are limited to open and abstract classes, and non-`fun` interfaces.
 * Any other uses allowed by `CLASS` annotation target yield a compilation error.
 *
 * @property markerClass specifies marker annotation that require explicit opt-in.
 * @see RequiresOptIn for a detailed description of opt-in semantics and propagation rules.
 */
@Target(CLASS)
@Retention(BINARY)
@SinceKotlin("1.8")
@ExperimentalSubclassOptIn
public annotation class SubclassOptInRequired(
    val markerClass: KClass<out Annotation>
)
