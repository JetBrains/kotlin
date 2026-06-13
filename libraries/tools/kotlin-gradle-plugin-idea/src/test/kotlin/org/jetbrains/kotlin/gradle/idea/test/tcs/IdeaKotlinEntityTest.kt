/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.tcs

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinEntity
import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.displayName
import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.getAllKotlinClasses
import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.ideaTcsPackage
import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.ideaTcsReflections
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass
import kotlin.test.fail
import java.util.stream.Stream

class IdeaKotlinEntityTest {

    @ParameterizedTest(name = "{1}")
    @MethodSource("findClasses")
    fun `test - node is marked as IdeaKotlinEntity`(node: KClass<*>, clazzName: String) {
        val entityAnnotations = node.findIdeaKotlinEntityAnnotations()

        if (entityAnnotations.isEmpty())
            fail("Expected class $clazzName to be marked with any ${IdeaKotlinEntity::class.java.simpleName} annotation")

        if (entityAnnotations.size > 1)
            fail("Conflicting ${IdeaKotlinEntity::class.java.simpleName} annotations on $clazzName ($entityAnnotations)")
    }


    companion object {

        @JvmStatic
        fun findClasses(): Stream<Arguments> {
            return ideaTcsReflections.getAllKotlinClasses()
                .filter { !it.java.isAnnotation }
                .filter { !it.isCompanion }
                .filter { it.qualifiedName.orEmpty().startsWith(ideaTcsPackage) }
                .map { clazz -> Arguments.of(clazz, checkNotNull(clazz.displayName())) }
                .stream()
        }
    }
}
