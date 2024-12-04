/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import junit.framework.TestCase
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.load.java.JvmAbi
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import kotlin.reflect.jvm.javaField

class ReflectionCodeSanityTest : TestCase() {
    private lateinit var classLoader: ClassLoader

    override fun setUp() {
        super.setUp()
        classLoader = ForTestCompileRuntime.runtimeAndReflectJarClassLoader()
    }

    override fun tearDown() {
        ReflectionCodeSanityTest::classLoader.javaField!!.set(this, null)
        super.tearDown()
    }

    private fun loadClass(name: String): Class<*> =
        classLoader.loadClass("kotlin.reflect.jvm.internal.$name")

    private fun collectClassesWithSupers(vararg names: String): Set<Class<*>> {
        val result = linkedSetOf<Class<*>>()
        fun addClassToCheck(klass: Class<*>) {
            if (result.add(klass)) {
                klass.superclass?.let(::addClassToCheck)
            }
        }

        for (name in names) {
            addClassToCheck(loadClass(name))
        }

        return result
    }

    fun testNoDelegatedPropertiesInKClassAndKProperties() {
        val badMembers = linkedSetOf<Member>()
        for (klass in collectClassesWithSupers(
            "KClassImpl",
            "KMutableProperty0Impl",
            "KMutableProperty1Impl",
            "KMutableProperty2Impl"
        )) {
            badMembers.addAll(klass.declaredFields.filter { it.name.endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX) })
            badMembers.addAll(klass.declaredMethods.filter { it.name.endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX) })
        }

        if (badMembers.isNotEmpty()) {
            fail(
                "The members listed below appear to be delegates for properties.\n" +
                        "It's highly not recommended to use property delegates in reflection.jvm because a KProperty instance\n" +
                        "is created for each delegated property and that makes the initialization sequence of reflection\n" +
                        "implementation classes unpredictable and leads to a deadlock or ExceptionInInitializerError.\n\n" +
                        "Please un-delegate the corresponding properties:\n\n" +
                        badMembers.joinToString("\n")
            )
        }
    }

    fun testMaxAllowedFields() {
        // The following classes are instantiated a lot in Kotlin applications, and thus they should be optimized as good as possible.
        // This test checks that these classes have not more fields than a predefined small number, which can usually be calculated as
        // the number of constructor parameters (number of objects needed to initialize an instance) + 1 for 'data', the reflection cache.
        val classesWithMaxAllowedFields = linkedMapOf(
            "KClassImpl" to 2,   // jClass, data
            "KPackageImpl" to 3  // jClass, moduleName, data
        )

        val badClasses = linkedMapOf<Class<*>, Collection<Field>>()
        for ((className, maxAllowedFields) in classesWithMaxAllowedFields) {
            val klass = loadClass(className)
            val fields = generateSequence(klass) { it.superclass }
                .flatMap { it.declaredFields.asSequence() }
                .filterNot { Modifier.isStatic(it.modifiers) }
                .toList()
            if (fields.size > maxAllowedFields) {
                badClasses[klass] = fields
            }
        }

        if (badClasses.isNotEmpty()) {
            fail("Some classes in reflection.jvm contain more fields than it is allowed. Please optimize storage in these classes:\n\n" +
                         badClasses.entries.joinToString("\n") { entry ->
                             val (klass, fields) = entry
                             "$klass has ${fields.size} fields but max allowed = ${classesWithMaxAllowedFields[klass.simpleName]}:\n" +
                                     fields.joinToString("\n") { "    $it" }
                         })
        }
    }
}
