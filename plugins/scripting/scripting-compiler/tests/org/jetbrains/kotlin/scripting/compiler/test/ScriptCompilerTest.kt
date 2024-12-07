/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.compiler.plugin.getBaseCompilerArgumentsFromProperty
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerIsolated
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMembers
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class ScriptCompilerTest : TestCase() {

    fun testCompilationWithRefinementError() {
        val res = compile("nonsense".toScriptSource()) {
            refineConfiguration {
                beforeCompiling {
                    ResultWithDiagnostics.Failure("err13".asErrorDiagnostics())
                }
            }
        }

        assertTrue(res is ResultWithDiagnostics.Failure)
        assertTrue(res.reports.any { it.message == "err13" })
        assertTrue(res.reports.none { it.message.contains("nonsense") })
    }

    fun testDeprecationAnnotation() {
        val res = compile("""
            @Deprecated("BECAUSE")
            fun deprecatedFunction() {}
            deprecatedFunction()
        """.trimIndent().toScriptSource()) {}

        assertTrue(res is ResultWithDiagnostics.Success)
        assertTrue(res.reports.any { it.message.contains("deprecatedFunction(): Unit' is deprecated. BECAUSE") })
    }

    fun testSimpleVarAccess() {
        val res = compileToClass(
            """
                val x = 2
                val y = x
            """.trimIndent().toScriptSource()
        )

        val kclass = res.valueOrThrow().java
        val scriptInstance = kclass.constructors.first().newInstance()
        assertNotNull(scriptInstance)
    }

    fun testLambdaWithProperty() {
        val versionProperties = java.util.Properties()
        "".reader().use { propInput ->
            versionProperties.load(propInput)
        }
        val res = compileToClass(
            """
                val versionProperties = java.util.Properties()
                "".reader().use { propInput ->
                    val x = 1
                    x.toString()
                    versionProperties.load(propInput)
                }
            """.trimIndent().toScriptSource()
        )

        val kclass = res.valueOrThrow().java
        val scriptInstance = kclass.constructors.first().newInstance()
        assertNotNull(scriptInstance)
    }

    fun testTypeAliases() {
        val res = compileToClass(
            """
                class Clazz
                typealias Tazz = List<Clazz>
                val x: Tazz = listOf()
                x
            """.trimIndent().toScriptSource()
        )

        val kclass = res.valueOrThrow()
        val nestedClasses = kclass.nestedClasses.toList()

        assertEquals(1, nestedClasses.size)
        assertEquals("Clazz", nestedClasses[0].simpleName)
    }

    fun testDestructingDeclarations() {
        val res = compileToClass(
            """
                val c = 3
                val (a, b) = 1 to 2
                val (_, d, _) = listOf('1', '2', '3')
            """.trimIndent().toScriptSource()
        )

        val kClass = res.valueOrThrow()
        val members = kClass.declaredMembers
        val scriptInstance = kClass.java.constructors.first().newInstance()
        val namesToMembers = members.associateBy { it.name }

        fun prop(name: String) = namesToMembers[name]!! as KProperty<*>
        fun propValue(name: String) = prop(name).call(scriptInstance)
        fun propType(name: String) = prop(name).returnType.classifier as KClass<*>

        assertEquals(1, propValue("a"))
        assertEquals(Int::class, propType("b"))
        assertEquals(3, propValue("c"))
        assertEquals(Char::class, propType("d"))
        assertNull(namesToMembers["_"])
    }

    fun testSimpleReflection() {
        val res = compileToClass(
            """
                val c = 3
                class A {
                    class B
                }
                val a = A()
            """.trimIndent().toScriptSource()
        )

        val kClass = res.valueOrThrow()
        val members = kClass.declaredMembers
        assertEquals(2, members.size)
        val nestedClasses = kClass.nestedClasses
        assertEquals(1, nestedClasses.size)
    }

    fun compile(
        script: SourceCode,
        cfgBody: ScriptCompilationConfiguration.Builder.() -> Unit
    ): ResultWithDiagnostics<CompiledScript> {
        val compilationConfiguration = ScriptCompilationConfiguration {
            cfgBody()
            getBaseCompilerArgumentsFromProperty()?.let {
                compilerOptions.append(it)
            }
        }
        val compiler = ScriptJvmCompilerIsolated(defaultJvmScriptingHostConfiguration)
        return compiler.compile(script, compilationConfiguration)
    }

    fun compileToClass(
        script: SourceCode,
        evaluationConfiguration: ScriptEvaluationConfiguration = ScriptEvaluationConfiguration(),
        cfgBody: ScriptCompilationConfiguration.Builder.() -> Unit = {},
    ): ResultWithDiagnostics<KClass<*>> {
        val result = compile(script, cfgBody)
        if (result is ResultWithDiagnostics.Failure) return result
        return runBlocking { result.valueOrThrow().getClass(evaluationConfiguration) }
    }
}