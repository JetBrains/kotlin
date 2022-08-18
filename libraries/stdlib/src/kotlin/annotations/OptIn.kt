/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind
import kotlin.reflect.KClass

/**
 * Signals that the annotated annotation class is a marker of an API that requires an explicit opt-in.
 *
 * Call sites of any declaration annotated with that marker should opt in to the API either by using [OptIn],
 * or by being annotated with that marker themselves, effectively causing further propagation of the opt-in requirement.
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

        /** Specifies that an error should be reported on incorrect usages of this API. */
        ERROR,
    }
}

/**
 * Allows to use the API denoted by the given markers in the annotated file, declaration, or expression.
 * If a declaration is annotated with [OptIn], its usages are **not** required to opt in to that API.
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
 * Opt-in marker annotation which allows to use [SubclassOptInRequired].
 */
@Target(CLASS)
@Retention(BINARY)
@SinceKotlin("1.8")
public annotation class ExperimentalSubclassOptIn

/**
 * Forbids creation of subclasses/sub-interfaces from the annotated class/interface without explicit [OptIn].
 *
 * This annotation is devoted to specific case when we want subclassing to be experimental.
 * In this case we annotated the base class or interface with [SubclassOptInRequired].
 * Without an explicit opt-in we have a compilation error/warning on the subclass/sub-interface after that.
 *
 * There are three ways to negate this error/warning:
 * <ol><li>Annotate subclass or interface with the marker annotation. In this case opt-in is propagated.</li>
 * <li>Annotate subclass or interface with [OptIn]. In this case opt-in isn't propagated.</li>
 * <li>Annotate subclass or interface with [SubclassOptInRequired]. In this case opt-in is propagated to subclasses/interfaces only.</li>
 * </ol>
 *
 * Note that [SubclassOptInRequired] does not negate an opt-in usage error itself.
 *
 * @property markerClass an opt-in marker to be required
 */
@Target(CLASS)
@Retention(BINARY)
@SinceKotlin("1.8")
@ExperimentalSubclassOptIn
public annotation class SubclassOptInRequired(
    val markerClass: KClass<out Annotation>
)
