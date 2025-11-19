/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import junit.framework.TestCase
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import java.lang.reflect.Field
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
