/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation

fun KClass<*>.findIdeaKotlinEntityAnnotations(): List<Annotation> {
    return (annotations + allSuperclasses.flatMap { it.annotations }).filter { annotation ->
        annotation.annotationClass.findAnnotation<IdeaKotlinEntity>() != null
    }
}

val KClass<*>.isIdeaKotlinModel get() = findIdeaKotlinEntityAnnotations().singleOrNull() is IdeaKotlinModel

val KClass<*>.isIdeaKotlinService get() = findIdeaKotlinEntityAnnotations().singleOrNull() is IdeaKotlinService
