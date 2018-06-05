/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.*
import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind
import kotlin.reflect.KClass

/**
 * Signals that the annotated annotation class is a marker of an experimental API. Any declaration annotated with that marker is thus
 * considered an experimental declaration and its call sites should accept the experimental aspect of it either by using [UseExperimental],
 * or by being annotated with that marker themselves, effectively causing further propagation of that experimental aspect.
 *
 * This class is experimental itself and can only be used with the compiler argument `-Xuse-experimental=kotlin.Experimental`.
 */
@Target(ANNOTATION_CLASS)
@Retention(BINARY)
@SinceKotlin("1.2")
@RequireKotlin("1.2.50", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@Suppress("ANNOTATION_CLASS_MEMBER")
annotation class Experimental(val level: Level = Level.ERROR) {
    /**
     * Severity of the diagnostic that should be reported on usages of experimental API which did not explicitly accept the experimental aspect
     * of that API either by using [UseExperimental] or by being annotated with the corresponding marker annotation.
     */
    enum class Level {
        /** Specifies that a warning should be reported on incorrect usages of this experimental API. */
        WARNING,
        /** Specifies that an error should be reported on incorrect usages of this experimental API. */
        ERROR,
    }

    @Deprecated("This enum is going to be removed and should no longer be used.")
    public enum class Impact {
        COMPILATION,
        LINKAGE,
        RUNTIME,
    }
}

/**
 * Allows to use experimental API denoted by the given markers in the annotated file, declaration, or expression.
 * If a declaration is annotated with [UseExperimental], its usages are **not** required to opt-in to that experimental API.
 *
 * This class is experimental itself and can only be used with the compiler argument `-Xuse-experimental=kotlin.Experimental`.
 */
@Target(
    CLASS, PROPERTY, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, EXPRESSION, FILE, TYPEALIAS
)
@Retention(SOURCE)
@SinceKotlin("1.2")
@RequireKotlin("1.2.50", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
annotation class UseExperimental(
    vararg val markerClass: KClass<out Annotation>
)


@Target(CLASS, PROPERTY, CONSTRUCTOR, FUNCTION, TYPEALIAS)
@Retention(BINARY)
internal annotation class WasExperimental(
    vararg val markerClass: KClass<out Annotation>
)
