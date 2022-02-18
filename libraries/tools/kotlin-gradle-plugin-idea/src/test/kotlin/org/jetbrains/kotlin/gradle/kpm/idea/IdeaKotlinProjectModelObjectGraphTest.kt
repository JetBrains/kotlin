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
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.findAnnotation
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
        if (!node.java.isInterface && !Modifier.isAbstract(node.java.modifiers)) {
            val serialVersionUID = assertNotNull(
                node.java.getDeclaredFieldOrNull("serialVersionUID"),
                "Expected $node to declare 'serialVersionUID' field"
            )

            assertTrue(
                Modifier.isStatic(serialVersionUID.modifiers),
                "Expected $node to declare 'serialVersionUID' statically"
            )

            assertTrue(
                serialVersionUID.type.isPrimitive,
                "Expected $node to declare primitive 'serialVersionUID'"
            )

            assertEquals(
                serialVersionUID.type, Long::class.javaPrimitiveType,
                "Expected $node to declare 'serialVersionUID' of type Long"
            )
        }
    }

    @Test
    fun `test - node implementations are marked with InternalKotlinGradlePluginApi when data class`() {
        if (node.isData && node.visibility == PUBLIC) {
            assertTrue(
                node.annotations.any { it.annotationClass.simpleName == "InternalKotlinGradlePluginApi" },
                "Expected $node to be annotated with '@InternalKotlinGradlePluginApi'"
            )
        }
    }

    private fun Class<*>.getDeclaredFieldOrNull(name: String): Field? {
        return try {
            getDeclaredField(name)
        } catch (t: NoSuchFieldException) {
            return null
        }
    }

    companion object {
        private val reflections = Reflections("org.jetbrains.kotlin")

        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun findNodes(): List<Array<Any>> {
            val classes = mutableSetOf<KClass<*>>()
            val resolveQueue = ArrayDeque<KClass<*>>(listOf(IdeaKotlinProjectModel::class))

            while (resolveQueue.isNotEmpty()) {
                val next = resolveQueue.removeFirst()

                /* Model gets replaced by other class */
                val writeReplacedModelAnnotation = next.findAnnotation<WriteReplacedModel>()
                if (writeReplacedModelAnnotation != null) {
                    resolveQueue.add(writeReplacedModelAnnotation.replacedBy)
                    continue
                }

                if (!classes.add(next)) continue
                next.resolveReachableClasses().forEach { child ->
                    resolveQueue.add(child)
                    if (child.java.isInterface || Modifier.isAbstract(child.java.modifiers)) {
                        val subtypes = reflections.getSubTypesOf(child.java).map { it.kotlin }
                        assertTrue(subtypes.isNotEmpty(), "Missing implementations for $child")
                        resolveQueue.addAll(subtypes)
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
