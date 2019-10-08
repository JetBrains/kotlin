/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation marks the experimental preview of the standard library API for measuring time and working with durations.
 *
 * > Note that this API is in a preview state and has a very high chance of being changed in the future.
 * Do not use it if you develop a library since your library will become binary incompatible
 * with the future versions of the standard library.
 *
 * Any usage of a declaration annotated with `@ExperimentalTime` must be accepted either by
 * annotating that usage with the [UseExperimental] annotation, e.g. `@UseExperimental(ExperimentalTime::class)`,
 * or by using the compiler argument `-Xuse-experimental=kotlin.time.ExperimentalTime`.
 */
@Experimental(level = Experimental.Level.ERROR)
@MustBeDocumented
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
@SinceKotlin("1.3")
public annotation class ExperimentalTime