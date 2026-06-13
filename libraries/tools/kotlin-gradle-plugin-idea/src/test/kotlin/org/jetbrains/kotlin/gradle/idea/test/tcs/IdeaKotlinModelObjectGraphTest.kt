/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.tcs

import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.displayName
import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.getAllKotlinClasses
import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.ideaTcsReflections
import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.kotlinReflections
import org.jetbrains.kotlin.tooling.core.AbstractExtras
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.lang.reflect.Modifier
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.test.*

class IdeaKotlinModelObjectGraphTest {

    @ParameterizedTest(name = "{1}")
    @MethodSource("findClasses")
    fun `test - node is sealed`(node: KClass<*>, @Suppress("UNUSED_PARAMETER") clazzName: String) {
        if (node.java.isInterface || node.isAbstract) {
            assertTrue(node.isSealed, "Expected $clazzName to be sealed")
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("findClasses")
    fun `test - node implements Serializable`(node: KClass<*>, clazzName: String) {
        assertTrue(
            Serializable::class.java.isAssignableFrom(node.java),
            "Expected clazz $clazzName to implement ${Serializable::class.qualifiedName}`"
        )
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("findClasses")
    fun `test - node implementations contain serialVersionUID`(node: KClass<*>, @Suppress("UNUSED_PARAMETER") clazzName: String) {
        if (!node.java.isInterface && !Modifier.isAbstract(node.java.modifiers)) {
            assertNodeContainsSerialVersionUID(node)
        }
    }

    companion object {
        private val ignoredNodes = setOf(
            /*
             Extras interface and AbstractExtras are okay for now:
             Let's check known implementations for correctness
            */
            Extras::class, MutableExtras::class, AbstractExtras::class
        )

        @JvmStatic
        fun findClasses(): Stream<Arguments> {
            val classes = mutableSetOf<KClass<*>>()

            val resolveQueue = ArrayDeque<KClass<*>>()

            resolveQueue += ideaTcsReflections.getAllKotlinClasses()
                .filter { it.isIdeaKotlinModel }

            while (resolveQueue.isNotEmpty()) {
                val next = resolveQueue.removeFirst()
                if (!classes.add(next)) continue

                next.resolveReachableClasses().forEach { child ->
                    resolveQueue.add(child)
                    if (child !in ignoredNodes && (child.java.isInterface || Modifier.isAbstract(child.java.modifiers))) {
                        val subtypes = kotlinReflections.getSubTypesOf(child.java).map { it.kotlin }
                        assertTrue(subtypes.isNotEmpty(), "Missing implementations for $child")
                        resolveQueue.addAll(subtypes)
                    }
                }
            }

            return classes
                .filter { it !in ignoredNodes }
                .map { clazz -> Arguments.of(clazz, checkNotNull(clazz.displayName())) }
                .stream()
        }

        private fun KClass<*>.resolveReachableClasses(): Set<KClass<*>> {
            return this.memberProperties
                .map { member -> member.returnType }
                .flatMap { type -> setOf(type) + type.arguments.mapNotNull { it.type } }
                .mapNotNull { type -> type.classifier as? KClass<*> }
                .filter { clazz -> clazz.java.name.startsWith("org.jetbrains") }
                .toSet()
        }
    }
}
