/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.reflections.Reflections
import kotlin.reflect.KClass

object ReflectionTestUtils {

    const val ideaTcsPackage = "org.jetbrains.kotlin.gradle.idea.tcs"
    val ideaTcsReflections = Reflections(ideaTcsPackage)

    const val kotlinPackage = "org.jetbrains.kotlin"
    val kotlinReflections = Reflections(kotlinPackage)

    fun KClass<*>.displayName() = java.name
        .removePrefix("org.jetbrains.kotlin")
        .removePrefix(".gradle")
        .removePrefix(".idea")
        .removePrefix(".tcs")
        .removePrefix(".")
}
