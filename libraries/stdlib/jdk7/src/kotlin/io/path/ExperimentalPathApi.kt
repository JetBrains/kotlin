/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.path

import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation marks the extensions and top-level functions for working with [java.nio.file.Path] considered experimental.
 *
 * > Beware using the annotated API especially if you're developing a library, since your library might become binary incompatible
 * with the future versions of the standard library.
 *
 * Any usage of a declaration annotated with `@ExperimentalPathApi` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalPathApi::class)`,
 * or by using the compiler argument `-Xopt-in=kotlin.io.path.ExperimentalPathApi`.
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
@SinceKotlin("1.4")
public annotation class ExperimentalPathApi
