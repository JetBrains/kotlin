/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.reflections.Reflections
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
@RunWith(Parameterized::class)
class IdeaKotlinProjectModelObjectGraphTest(private val node: KClass<*>, @Suppress("unused_parameter") clazzName: String) {

    @Test
    fun `test - node implements Serializable`() {
        assertTrue(
            java.io.Serializable::class.java.isAssignableFrom(node.java),
            "Expected node ${node.simpleName} to implement `Serializable`"
        )
    }

    @Test
    fun `test - node implementations contain serialVersionUID`() {
        val reflections = Reflections("org.jetbrains.kotlin")
        reflections.getSubTypesOf(node.java).forEach { subtype ->
            if (!subtype.isInterface && !Modifier.isAbstract(subtype.modifiers)) {
                assertNodeImplementationDefinesSerialVersionUID(subtype)
            }
        }
    }

    private fun assertNodeImplementationDefinesSerialVersionUID(implementationClass: Class<*>) {
        val serialVersionUID = assertNotNull(
            implementationClass.getDeclaredFieldOrNull("serialVersionUID"),
            "Expected $implementationClass to declare 'serialVersionUID' field"
        )

        assertTrue(
            Modifier.isStatic(serialVersionUID.modifiers),
            "Expected $implementationClass to declare 'serialVersionUID' statically"
        )

        assertTrue(
            serialVersionUID.type.isPrimitive,
            "Expected $implementationClass to declare primitive 'serialVersionUID'"
        )

        assertEquals(
            serialVersionUID.type, Long::class.javaPrimitiveType,
            "Expected $implementationClass to declare 'serialVersionUID' of type Long"
        )
    }

    private fun Class<*>.getDeclaredFieldOrNull(name: String): Field? {
        return try {
            getDeclaredField(name)
        } catch (t: NoSuchFieldException) {
            return null
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun findNodes(): List<Array<Any>> {
            val classes = mutableSetOf<KClass<*>>(IdeaKotlinProjectModel::class)
            val resolveQueue = ArrayDeque(classes)

            while (resolveQueue.isNotEmpty()) {
                val children = resolveQueue.removeFirst().resolveReachableClasses()
                children.forEach { child ->
                    if (classes.add(child)) {
                        resolveQueue.add(child)
                    }
                }
            }

            return classes.map { clazz -> arrayOf(clazz, checkNotNull(clazz.simpleName)) }
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
