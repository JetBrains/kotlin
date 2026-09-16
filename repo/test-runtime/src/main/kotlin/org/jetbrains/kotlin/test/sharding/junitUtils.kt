/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

internal fun Class<*>.hasBeforeAllAnnotation(): Boolean {
    return hasBeforeAllClassValue[this]
}

internal fun Class<*>.hasAfterAllAnnotation(): Boolean {
    return hasAfterAllClassValue[this]
}

/**
 * Checks the class and its superclasses for a method with the given annotation.
 * ClassValue caches the answer per class, so tests in the same class do not repeat the reflection work.
 */
private class HasMethodWithAnnotationClassValue(val annotationClass: Class<out Annotation>) : ClassValue<Boolean>() {

    override fun computeValue(type: Class<*>): Boolean {
        if (type.superclass?.let { superclass ->
                this[superclass]
            } == true) return true

        if (type.enclosingClass?.let { enclosingClass ->
                this[enclosingClass]
            } == true) return true

        if (type.interfaces.orEmpty().any { superInterface ->
                this[superInterface.superclass]
            }) return true

        if (type.declaredMethods.any { method ->
                method.isAnnotationPresent(annotationClass)
            }) return true

        return false
    }
}

/**
 * Can be used to query if a class contains any [BeforeAll] annotation
 */
private val hasBeforeAllClassValue = HasMethodWithAnnotationClassValue(BeforeAll::class.java)

/**
 * Can be used to query if a class contains any [AfterAll] annotation
 */
private val hasAfterAllClassValue = HasMethodWithAnnotationClassValue(AfterAll::class.java)
