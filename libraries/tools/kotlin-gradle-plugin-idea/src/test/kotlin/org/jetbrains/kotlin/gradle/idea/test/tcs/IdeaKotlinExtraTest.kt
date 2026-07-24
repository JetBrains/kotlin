/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.tcs

import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.displayName
import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.getAllKotlinClasses
import org.jetbrains.kotlin.tooling.core.Extras
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.test.fail

class IdeaKotlinExtraTest {

    @ParameterizedTest(name = "{1}")
    @MethodSource("findClasses")
    fun `test - node is data class`(node: KClass<*>, clazzName: String) {
        if (!node.isData) fail("Expected $clazzName to be marked as data class")
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("findClasses")
    fun `test - node is Serializable`(node: KClass<*>, clazzName: String) {
        if (!node.isSubclassOf(Serializable::class)) fail("Expected $clazzName to implement ${Serializable::class}")
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("findClasses")
    fun `test - contains serialVersionUID`(node: KClass<*>, @Suppress("UNUSED_PARAMETER") clazzName: String) {
        assertNodeContainsSerialVersionUID(node)
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("findClasses")
    fun `test - all members properties are nullable`(node: KClass<*>, clazzName: String) {
        node.memberProperties.forEach { member ->
            if (!member.returnType.isMarkedNullable) fail("Expected $clazzName.${member.name} to be marked nullable")
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("findClasses")
    fun `test - node has companion with key`(node: KClass<*>, clazzName: String) {
        val companion = node.companionObject ?: fail("Missing companion on $clazzName")
        val keyProperty = companion.memberProperties.find { it.name == "key" } ?: fail("Missing .key on $clazzName.${companion.simpleName}")
        val keyType = keyProperty.returnType

        val isCorrectType = when {
            keyType.classifier != Extras.Key::class -> false
            keyType.arguments.singleOrNull()?.type?.classifier != node -> false
            else -> true
        }

        if (!isCorrectType) fail("Expected key to be type Extras.Key<${node.simpleName}>. Found $keyType")
    }

    companion object {
        @JvmStatic
        fun findClasses(): Stream<Arguments> {
            return ReflectionTestUtils.ideaTcsReflections.getAllKotlinClasses()
                .filter { it.isIdeaKotlinExtra }
                .map { Arguments.of(it, it.displayName()) }
                .stream()
        }
    }
}
