/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.runtime

import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation marks the Kotlin/Native standard library API that tweaks
 * or otherwise accesses the Kotlin runtime behavior.
 *
 * The API marked with this annotation is considered unstable and is **not** intended to become stable in the future.
 * Behavior of such an API may be changed or the API may be removed completely in any further release.
 *
 * Any usage of a declaration annotated with `@NativeRuntimeApi` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(NativeRuntimeApi::class)`,
 * or by using the compiler argument `-opt-in=kotlin.native.runtime.NativeRuntimeApi`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
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
@SinceKotlin("1.9")
public annotation class NativeRuntimeApi