/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode

import kotlin.annotation.AnnotationTarget.*

/**
 * Marks the experimental Unicode CodePoint API.
 *
 * Usages of such API will be reported as errors unless an explicit opt-in with
 * the [OptIn] annotation, e.g. `@OptIn(ExperimentalUnicodeApi::class)`,
 * or with the `-opt-in=kotlin.ExperimentalUnicodeApi` compiler option is given.
 *
 * It's recommended to propagate the experimental status to the API that depends on Unicode CodePoint API by annotating it with this annotation.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@MustBeDocumented
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalUnicodeApi


