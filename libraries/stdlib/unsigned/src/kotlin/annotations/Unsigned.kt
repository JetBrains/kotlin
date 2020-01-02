/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package kotlin

import kotlin.annotation.AnnotationTarget.*
import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

/**
 * Marks the API that is dependent on the experimental unsigned types, including those types themselves.
 *
 * Usages of such API will be reported as warnings unless an explicit opt-in with
 * the [UseExperimental] annotation, e.g. `@UseExperimental(ExperimentalUnsignedTypes::class)`,
 * or with the `-Xuse-experimental=kotlin.ExperimentalUnsignedTypes` compiler option is given.
 *
 * It's recommended to propagate the experimental status to the API that depends on unsigned types by annotating it with this annotation.
 */
@Experimental(level = Experimental.Level.WARNING)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@MustBeDocumented
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
@RequireKotlin("1.2.50", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
public annotation class ExperimentalUnsignedTypes
