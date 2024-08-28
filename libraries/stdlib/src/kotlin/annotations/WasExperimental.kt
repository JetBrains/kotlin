/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.internal.Builtin

package kotlin

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass


@Target(CLASS, PROPERTY, CONSTRUCTOR, FUNCTION, TYPEALIAS)
@Retention(BINARY)
internal annotation class WasExperimental(
    vararg val markerClass: KClass<out Annotation>
)
