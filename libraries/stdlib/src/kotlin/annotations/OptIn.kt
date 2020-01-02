/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.*
import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind
import kotlin.reflect.KClass

/**
 * Signals that the annotated annotation class is a marker of an API that requires an explicit opt-in.
 *
 * Call sites of any declaration annotated with that marker should opt-in to the API either by using [OptIn],
 * or by being annotated with that marker themselves, effectively causing further propagation of the opt-in requirement.
 *
 * This class requires opt-in itself and can only be used with the compiler argument `-Xopt-in=kotlin.RequiresOptIn`.
 */
@Target(ANNOTATION_CLASS)
@Retention(BINARY)
@SinceKotlin("1.3")
@RequireKotlin("1.3.70", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
public annotation class RequiresOptIn(val level: Level = Level.ERROR) {
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
 * If a declaration is annotated with [OptIn], its usages are **not** required to opt-in to that API.
 *
 * This class requires opt-in itself and can only be used with the compiler argument `-Xopt-in=kotlin.RequiresOptIn`.
 */
@Target(
    CLASS, PROPERTY, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, EXPRESSION, FILE, TYPEALIAS
)
@Retention(SOURCE)
@SinceKotlin("1.3")
@RequireKotlin("1.3.70", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
public annotation class OptIn(
    vararg val markerClass: KClass<out Annotation>
)
