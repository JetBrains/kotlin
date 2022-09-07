/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.reflections.Reflections
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.test.fail

val reflections = Reflections("org.jetbrains")

fun assertAllImplementationsAlsoImplement(
    baseInterface: KClass<*>,
    requiredInterface: KClass<*>
) {
    reflections.getSubTypesOf(baseInterface.java)
        .filter { subtype -> !subtype.isInterface }
        .forEach { implementation ->
            if (!implementation.kotlin.isSubclassOf(requiredInterface)) {
                fail("${implementation.kotlin} does not implement $requiredInterface")
            }
        }
}
