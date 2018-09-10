/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt3.test

import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.Test
import org.junit.runner.JUnitCore
import org.junit.runner.Request
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class KotlinKapt3IntegrationTests : AbstractKotlinKapt3IntegrationTest(), Java9TestLauncher {
    override fun test(
            name: String,
            vararg supportedAnnotations: String,
            options: Map<String, String>,
            process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment) -> Unit
    ) {
        super.test(name, *supportedAnnotations, options = options, process = process)

        doTestWithJdk9(SingleJUnitTestRunner::class.java,
                       KotlinKapt3IntegrationTests::class.java.name + "#test" + getTestName(false))
    }

    @Test
    fun testSimple() = test("Simple", "test.MyAnnotation") { set, roundEnv, _ ->
        assertEquals(1, set.size)
        val annotatedElements = roundEnv.getElementsAnnotatedWith(set.single())
        assertEquals(1, annotatedElements.size)
        assertEquals("myMethod", annotatedElements.single().simpleName.toString())
    }

    @Test
    fun testSuperclass() = test("Superclass", "test.MyAnnotation") { set, roundEnv, env ->
        if (set.size == 1) {
            val annotatedElements = roundEnv.getElementsAnnotatedWith(set.single())
            assertEquals(1, annotatedElements.size)
            val typeElement = annotatedElements.single() as TypeElement
            assertEquals("FooBar", typeElement.superclass.toString())
            env.filer.createSourceFile("test.FooBar").openWriter().use { writer ->
                writer.write("package test;")
                writer.write("public class FooBar {}")
            }
        }
    }

    @Test
    fun testComments() = test("Simple", "test.MyAnnotation") { set, roundEnv, env ->
        fun commentOf(className: String) = env.elementUtils.getDocComment(env.elementUtils.getTypeElement(className))

        assert(commentOf("test.Simple") == " * KDoc comment.\n")
        assert(commentOf("test.EnumClass") == null) // simple comment - not saved
        assert(commentOf("test.MyAnnotation") == null) // multiline comment - not saved
    }

    @Test
    fun testSimpleStubsAndIncrementalData() = bindingsTest("Simple") { stubsOutputDir, incrementalDataOutputDir, bindings ->
        assert(File(stubsOutputDir, "error/NonExistentClass.java").exists())
        assert(File(stubsOutputDir, "test/Simple.java").exists())
        assert(File(stubsOutputDir, "test/EnumClass.java").exists())

        assert(File(incrementalDataOutputDir, "test/Simple.class").exists())
        assert(File(incrementalDataOutputDir, "test/EnumClass.class").exists())

        assert(bindings.any { it.key == "test/Simple" && it.value.name == "test/Simple.java" })
        assert(bindings.any { it.key == "test/EnumClass" && it.value.name == "test/EnumClass.java" })
    }

    @Test
    fun testStubsAndIncrementalDataForNestedClasses() = bindingsTest("NestedClasses") { stubsOutputDir, incrementalDataOutputDir, bindings ->
        assert(File(stubsOutputDir, "test/Simple.java").exists())
        assert(!File(stubsOutputDir, "test/Simple/InnerClass.java").exists())

        assert(File(incrementalDataOutputDir, "test/Simple.class").exists())
        assert(File(incrementalDataOutputDir, "test/Simple\$Companion.class").exists())
        assert(File(incrementalDataOutputDir, "test/Simple\$InnerClass.class").exists())
        assert(File(incrementalDataOutputDir, "test/Simple\$NestedClass.class").exists())
        assert(File(incrementalDataOutputDir, "test/Simple\$NestedClass\$NestedNestedClass.class").exists())

        assert(bindings.any { it.key == "test/Simple" && it.value.name == "test/Simple.java" })
        assert(bindings.none { it.key.contains("Companion") })
        assert(bindings.none { it.key.contains("InnerClass") })
    }

    private fun bindingsTest(name: String, test: (File, File, Map<String, KaptJavaFileObject>) -> Unit) {
        test(name, "test.MyAnnotation") { _, _, _ ->
            val kaptExtension = AnalysisHandlerExtension.getInstances(myEnvironment.project).firstIsInstance<Kapt3ExtensionForTests>()

            val stubsOutputDir = kaptExtension.paths.stubsOutputDir
            val incrementalDataOutputDir = kaptExtension.paths.incrementalDataOutputDir

            val bindings = kaptExtension.savedBindings!!

            test(stubsOutputDir, incrementalDataOutputDir!!, bindings)
        }
    }

    @Test
    fun testOptions() = test(
            "Simple", "test.MyAnnotation",
            options = mapOf("firstKey" to "firstValue", "secondKey" to "")
    ) { _, _, env ->
        val options = env.options
        assertEquals("firstValue", options["firstKey"])
        assertTrue("secondKey" in options)
    }

    @Test
    fun testOverloads() = test("Overloads", "test.MyAnnotation") { set, roundEnv, _ ->
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
}

internal class SingleJUnitTestRunner {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val (className, methodName) = args.single().split('#')
            val request = Request.method(Class.forName(className), methodName)
            val result = JUnitCore().run(request)
            System.exit(if (result.wasSuccessful()) 0 else 1)
        }
    }
}