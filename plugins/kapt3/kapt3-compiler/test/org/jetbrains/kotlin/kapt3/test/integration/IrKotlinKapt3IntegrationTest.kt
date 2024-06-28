/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.integration

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertNotNull
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.fail
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

class IrKotlinKapt3IntegrationTest(private val testInfo: TestInfo) {
    private companion object {
        val TEST_DATA_DIR = File("plugins/kapt3/kapt3-compiler/testData/kotlinRunner")
    }

    private fun test(
        name: String,
        vararg supportedAnnotations: String,
        options: Map<String, String> = emptyMap(),
        expectFailure: Boolean = false,
        additionalPluginExtension: IrGenerationExtension? = null,
        process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment, Kapt3ExtensionForTests) -> Unit
    ) {
        val file = File(TEST_DATA_DIR, "$name.kt")
        AbstractKotlinKapt3IntegrationTestRunner(
            options,
            supportedAnnotations.toList(),
            additionalPluginExtension,
            process
        ).apply {
            initTestInfo(testInfo)
            try {
                runTest(file.absolutePath)
                if (expectFailure) throw AssertionError("Expected compilation to fail, but it didn't.")
            } catch (ex: CompilationErrorException) {
                if (!expectFailure) throw ex
            }
        }
    }

    @Test
    fun testSimple() = test("Simple", "test.MyAnnotation") { set, roundEnv, _, _ ->
        assertEquals(1, set.size)
        val annotatedElements = roundEnv.getElementsAnnotatedWith(set.single())
        assertEquals(1, annotatedElements.size)
        assertEquals("myMethod", annotatedElements.single().simpleName.toString())
    }

    @Test
    fun testComments() = test("Simple", "test.MyAnnotation") { _, _, env, _ ->
        fun commentOf(className: String) = env.elementUtils.getDocComment(env.elementUtils.getTypeElement(className))

        assertTrue(commentOf("test.Simple") == " KDoc comment.\n")
        assertTrue(commentOf("test.EnumClass") == null) // simple comment - not saved
        assertTrue(commentOf("test.MyAnnotation") == null) // multiline comment - not saved
    }

    @Test
    fun testParameterNames() {
        test("DefaultParameterValues", "test.Anno") { set, roundEnv, _, _ ->
            val user = roundEnv.getElementsAnnotatedWith(set.single()).single() as TypeElement
            val nameField = user.enclosedElements.filterIsInstance<VariableElement>().single()
            assertEquals("John", nameField.constantValue)
        }
    }

    @Test
    fun testSimpleStubsAndIncrementalData() = bindingsTest("Simple") { stubsOutputDir, incrementalDataOutputDir, bindings ->
        assertTrue(File(stubsOutputDir, "error/NonExistentClass.java").exists())
        assertTrue(File(stubsOutputDir, "test/Simple.java").exists())
        assertTrue(File(stubsOutputDir, "test/EnumClass.java").exists())

        assertTrue(File(incrementalDataOutputDir, "test/Simple.class").exists())
        assertTrue(File(incrementalDataOutputDir, "test/EnumClass.class").exists())

        assertTrue(bindings.any { it.key == "test/Simple" && it.value.name == "test/Simple.java" })
        assertTrue(bindings.any { it.key == "test/EnumClass" && it.value.name == "test/EnumClass.java" })
    }

    @Test
    fun testStubsAndIncrementalDataForNestedClasses() {
        bindingsTest("NestedClasses") { stubsOutputDir, incrementalDataOutputDir, bindings ->
            assertTrue(File(stubsOutputDir, "test/Simple.java").exists())
            assertTrue(!File(stubsOutputDir, "test/Simple/InnerClass.java").exists())

            assertTrue(File(incrementalDataOutputDir, "test/Simple.class").exists())
            assertTrue(File(incrementalDataOutputDir, "test/Simple\$Companion.class").exists())
            assertTrue(File(incrementalDataOutputDir, "test/Simple\$InnerClass.class").exists())
            assertTrue(File(incrementalDataOutputDir, "test/Simple\$NestedClass.class").exists())
            assertTrue(File(incrementalDataOutputDir, "test/Simple\$NestedClass\$NestedNestedClass.class").exists())

            assertTrue(bindings.any { it.key == "test/Simple" && it.value.name == "test/Simple.java" })
            assertTrue(bindings.none { it.key.contains("Companion") })
            assertTrue(bindings.none { it.key.contains("InnerClass") })
        }
    }

    @Test
    fun testErrorLocationMapping() {
        val diagnostics = diagnosticsTest("ErrorLocationMapping", "MyAnnotation") { _, _, processingEnv ->
            val subject = processingEnv.elementUtils.getTypeElement("Subject")
            assertNotNull(subject)
            processingEnv.messager.printMessage(
                Diagnostic.Kind.NOTE,
                "note on class",
                subject
            )
            // report error on the field as well
            val field = ElementFilter.fieldsIn(
                subject.enclosedElements
            ).firstOrNull {
                it.simpleName.toString() == "field"
            }
            processingEnv.messager.printMessage(
                Diagnostic.Kind.NOTE,
                "note on field",
                field
            )
        }
        diagnostics.assertContainsDiagnostic("ErrorLocationMapping.kt:1: Note: note on class")
        diagnostics.assertContainsDiagnostic("ErrorLocationMapping.kt:5: Note: note on field")
    }

    private fun List<LoggingMessageCollector.Message>.assertContainsDiagnostic(
        message: String,
        severity: CompilerMessageSeverity? = null
    ) {
        assertTrue(
            any { msg ->
                (severity?.let { it == msg.severity } ?: true) && msg.message.contains(message)
            }
        ) {
            """
            |Didn't find expected diagnostic message.
            |Expected: $message
            |Severity: ${severity ?: "ANY"}
            |Diagnostics:
            |${this.joinToString("\n") { "${it.severity}: ${it.message}" }}
            """.trimMargin()
        }
    }

    @Suppress("SameParameterValue")
    private fun diagnosticsTest(
        name: String,
        vararg supportedAnnotations: String,
        expectFailure: Boolean = false,
        process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment) -> Unit
    ): List<LoggingMessageCollector.Message> {
        lateinit var messageCollector: LoggingMessageCollector
        test(
            name = name,
            supportedAnnotations = supportedAnnotations,
            expectFailure = expectFailure,
        ) { typeElements, roundEnv, processingEnv, kaptExtension ->
            messageCollector = kaptExtension.messageCollector
            process(typeElements, roundEnv, processingEnv)
        }
        return messageCollector.messages
    }


    private fun bindingsTest(name: String, test: (File, File, Map<String, KaptJavaFileObject>) -> Unit) {
        test(name, "test.MyAnnotation") { _, _, _, kaptExtension->
            val stubsOutputDir = kaptExtension.options.stubsOutputDir
            val incrementalDataOutputDir = kaptExtension.options.incrementalDataOutputDir

            val bindings = kaptExtension.savedBindings!!

            test(stubsOutputDir, incrementalDataOutputDir!!, bindings)
        }
    }

    @Test
    fun testOptions() = test(
        "Simple", "test.MyAnnotation",
        options = mapOf("firstKey" to "firstValue", "secondKey" to "")
    ) { _, _, env, _ ->
        val options = env.options
        assertEquals("firstValue", options["firstKey"])
        assertTrue("secondKey" in options)
    }

    @Test
    fun testOverloads() = test("Overloads", "test.MyAnnotation") { set, roundEnv, _, _ ->
        assertEquals(1, set.size)
        val annotatedElements = roundEnv.getElementsAnnotatedWith(set.single())
        assertEquals(1, annotatedElements.size)
        val constructors = annotatedElements
            .first()
            .enclosedElements
            .filter { it.kind == ElementKind.CONSTRUCTOR }
            .map { it as ExecutableElement }
            .sortedBy { it.parameters.size }
        assertEquals(2, constructors.size)
        assertEquals(2, constructors[0].parameters.size)
        assertEquals(3, constructors[1].parameters.size)
        assertEquals("int", constructors[0].parameters[0].asType().toString())
        assertEquals("long", constructors[0].parameters[1].asType().toString())
        assertEquals("int", constructors[1].parameters[0].asType().toString())
        assertEquals("long", constructors[1].parameters[1].asType().toString())
        assertEquals("java.lang.String", constructors[1].parameters[2].asType().toString())
        assertEquals("someInt", constructors[0].parameters[0].simpleName.toString())
        assertEquals("someLong", constructors[0].parameters[1].simpleName.toString())
        assertEquals("someInt", constructors[1].parameters[0].simpleName.toString())
        assertEquals("someLong", constructors[1].parameters[1].simpleName.toString())
        assertEquals("someString", constructors[1].parameters[2].simpleName.toString())
    }

    @Test
    fun testLog() {
        val diagnostics = diagnosticsTest(
            name = "Log",
            supportedAnnotations = arrayOf("*"),
            expectFailure = true
        ) { _, _, env ->
            env.messager.printMessage(Diagnostic.Kind.ERROR, "a error from processor")
            env.messager.printMessage(Diagnostic.Kind.WARNING, "a warning from processor")
            env.messager.printMessage(Diagnostic.Kind.NOTE, "a note from processor")
        }
        diagnostics.assertContainsDiagnostic("error: a error from processor", CompilerMessageSeverity.ERROR)
        diagnostics.assertContainsDiagnostic("warning: a warning from processor", CompilerMessageSeverity.STRONG_WARNING)
        diagnostics.assertContainsDiagnostic("Note: a note from processor", CompilerMessageSeverity.INFO)
    }

    @Test
    fun testKt54245() = test(
        "Simple", "test.MyAnnotation",
        additionalPluginExtension = object : IrGenerationExtension {
            override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
                fail("IR generation extensions should not be run in kapt mode.")
            }
        }
    ) { _, _, _, _ -> }
}
