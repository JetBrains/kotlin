/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.annotation.AnnotationTarget.*
import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

/**
 * The experimental multiplatform support API marker.
 *
 * Any usage of a declaration annotated with `@ExperimentalMultiplatform` must be accepted either by
 * annotating that usage with the [UseExperimental] annotation, e.g. `@UseExperimental(ExperimentalMultiplatform::class)`,
 * or by using the compiler argument `-Xuse-experimental=kotlin.ExperimentalMultiplatform`.
 */
@Experimental
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
@Retention(AnnotationRetention.BINARY)
@RequireKotlin("1.2.50", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
public annotation class ExperimentalMultiplatform

/**
 * Marks an expected annotation class that it isn't required to have actual counterparts in all platforms.
 *
 * This annotation is only applicable to `expect` annotation classes in multi-platform projects and marks that class as "optional".
 * Optional expected class is allowed to have no corresponding actual class on the platform. Optional annotations can only be used
 * to annotate something, not as types in signatures. If an optional annotation has no corresponding actual class on a platform,
 * the annotation entries where it's used are simply erased when compiling code on that platform.
 *
 * Note: this annotation is experimental, see [ExperimentalMultiplatform] on how to opt-in for it.
 */
@Target(ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@ExperimentalMultiplatform
@RequireKotlin("1.2.50", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
public annotation class OptionalExpectation
