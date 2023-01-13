/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.tcs

import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.displayName
import org.jetbrains.kotlin.gradle.idea.test.tcs.ReflectionTestUtils.getAllKotlinClasses
import org.jetbrains.kotlin.tooling.core.Extras
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.fail

@RunWith(Parameterized::class)
class IdeaKotlinExtraTest(private val node: KClass<*>, private val clazzName: String) {

    @Test
    fun `test - node is data class`() {
        if (!node.isData) fail("Expected $clazzName to be marked as data class")
    }

    @Test
    fun `test - node is Serializable`() {
        if (!node.isSubclassOf(Serializable::class)) fail("Expected $clazzName to implement ${Serializable::class}")
    }

    @Test
    fun `test - contains serialVersionUID`() {
        assertNodeContainsSerialVersionUID(node)
    }

    @Test
    fun `test - all members properties are nullable`() {
        node.memberProperties.forEach { member ->
            if (!member.returnType.isMarkedNullable) fail("Expected $clazzName.${member.name} to be marked nullable")
        }
    }

    @Test
    fun `test - node has companion with key`() {
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
        @Parameterized.Parameters(name = "{1}")
        fun findClasses(): List<Array<Any>> {
            return ReflectionTestUtils.ideaTcsReflections.getAllKotlinClasses()
                .filter { it.isIdeaKotlinExtra }
                .map { arrayOf(it, it.displayName()) }
        }
    }
}
