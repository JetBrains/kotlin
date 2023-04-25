/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native

import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation marks the Kotlin/Native standard library API that is considered obsolete and is being phased out.
 *
 * An obsolete API is not recommended to use, and users should migrate from it.
 * In the future the opt-in level of the annotation might be raised to [ERROR][RequiresOptIn.Level.ERROR].
 *
 * Any usage of a declaration annotated with `@ObsoleteNativeApi` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ObsoleteNativeApi::class)`,
 * or by using the compiler argument `-opt-in=kotlin.native.ObsoleteNativeApi`.
 */
@RequiresOptIn(message = "This API is obsolete and subject to removal in a future release.", level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(
        AnnotationTarget.CLASS,
        AnnotationTarget.ANNOTATION_CLASS,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.FIELD,
        AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.TYPEALIAS
)
@MustBeDocumented
@SinceKotlin("1.9")
public annotation class ObsoleteNativeApi