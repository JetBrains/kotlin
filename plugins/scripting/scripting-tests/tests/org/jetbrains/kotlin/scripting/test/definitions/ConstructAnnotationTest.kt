/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.definitions

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.useFir
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.PsiScriptAnnotationsCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptDiagnosticsMessageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmK2CompilerIsolated
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.createCompilationContextFromEnvironment
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.getScriptKtFile
import org.jetbrains.kotlin.scripting.resolve.InvalidScriptResolverAnnotation
import org.jetbrains.kotlin.scripting.test.TestDisposable
import org.jetbrains.kotlin.scripting.test.updateWithBaseCompilerArguments
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.RunAll
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.util.classpathFromClass
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val testDataPath = "plugins/scripting/scripting-tests/testData/definitions/constructAnnotations"

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class TestAnnotation(vararg val options: String)

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class AnnotationWithVarArgAndArray(vararg val options: String, val moreOptions: Array<String>)

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class AnnotationWithPrimitiveArrays(val ints: IntArray, vararg val flags: Boolean)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class AnnotationWithDefault(val value: String = "default")

class ConstructAnnotationTest {
    private val testRootDisposable: Disposable = TestDisposable("${ConstructAnnotationTest::class.simpleName}.testRootDisposable")

    @AfterTest
    fun tearDown() {
        RunAll(
            { Disposer.dispose(testRootDisposable) },
        )
    }

    @Test
    fun testAnnotationEmptyVarArg() {
        val annotations = annotations("TestAnnotationEmptyVarArg.kts", TestAnnotation::class)
            .valueOrThrow()
            .filterIsInstance<TestAnnotation>()

        assertEquals(1, annotations.count())
        assert(annotations.first().options.isEmpty())
    }

    @Test
    fun testBasicVarArgTestAnnotation() {
        val annotations = annotations("SimpleTestAnnotation.kts", TestAnnotation::class)
            .valueOrThrow()
            .filterIsInstance<TestAnnotation>()

        assertEquals(1, annotations.count())
        assertEquals(listOf("option"), annotations.first().options.toList())
    }

    @Test
    fun testAnnotationWithArrayLiteral() {
        val annotations = annotations("TestAnnotationWithArrayLiteral.kts", TestAnnotation::class)
            .valueOrThrow()
            .filterIsInstance<TestAnnotation>()

        assertEquals(1, annotations.count())
        assertEquals(listOf("option"), annotations.first().options.toList())
    }

    @Test
    fun testAnnotationWithArrayOfFunction() {
        val annotations = annotations("TestAnnotationWithArrayOfFunction.kts", TestAnnotation::class)
            .valueOrThrow()
            .filterIsInstance<TestAnnotation>()

        assertEquals(1, annotations.count())
        assertEquals(listOf("option"), annotations.first().options.toList())
    }

    @Test
    fun testAnnotationWithEmptyArrayFunction() {
        val annotations = annotations("TestAnnotationWithEmptyArrayFunction.kts", TestAnnotation::class)
            .valueOrThrow()
            .filterIsInstance<TestAnnotation>()

        assertEquals(1, annotations.count())
        assert(annotations.first().options.isEmpty())
    }

    @Test
    fun testArrayAfterVarArgInAnnotation() {
        val annotations = annotations("TestAnnotationWithVarArgAndArray.kts", AnnotationWithVarArgAndArray::class)
            .valueOrThrow()
            .filterIsInstance<AnnotationWithVarArgAndArray>()

        assertEquals(1, annotations.count())
        assertEquals(listOf("option"), annotations.first().options.toList())
        assertEquals(listOf("otherOption"), annotations.first().moreOptions.toList())
    }

    @Test
    fun testPrimitiveArraysInAnnotation() {
        val annotations = annotations("TestAnnotationWithPrimitiveArrays.kts", AnnotationWithPrimitiveArrays::class)
            .valueOrThrow()
            .filterIsInstance<AnnotationWithPrimitiveArrays>()

        assertEquals(2, annotations.count())
        assertEquals(listOf(1, 2), annotations[0].ints.toList())
        assertEquals(listOf(true, false), annotations[0].flags.toList())
        assertEquals(listOf(3), annotations[1].ints.toList())
        assertEquals(listOf(true), annotations[1].flags.toList())
    }

    @Test
    fun testUnknownNamedArgumentIsNotReplacedByDefault() {
        val result = annotations("TestAnnotationWithUnknownArgument.kts", AnnotationWithDefault::class)

        assertIs<ResultWithDiagnostics.Failure>(result)
        val messages = result.reports.map { it.message }
        assertTrue(messages.any { "Error resolving annotation" in it && "typo" in it }, messages.toString())
    }

    @Test
    fun testInvalidAnnotationIsPassedToHandlersOnLightTreeAndPsi() {
        val psiAnnotations = collectAnnotationsViaPsi(
            invalidAnnotationScript, invalidAnnotationScriptConfiguration { it.compilationConfiguration.asSuccess() }
        ).valueOrThrow()
        val lightTreeAnnotations = mutableListOf<Annotation>()
        val result = compileViaLightTree(
            invalidAnnotationScript,
            invalidAnnotationScriptConfiguration { context ->
                lightTreeAnnotations.addAll(context.collectedData?.get(ScriptCollectedData.collectedAnnotations).orEmpty().map { it.annotation })
                context.compilationConfiguration.asSuccess()
            }
        )

        for (annotations in listOf(psiAnnotations, lightTreeAnnotations)) {
            assertEquals(listOf("option"), annotations.filterIsInstance<TestAnnotation>().single().options.toList())
            val invalid = annotations.filterIsInstance<InvalidScriptResolverAnnotation>().single()
            assertEquals("AnnotationWithDefault", invalid.name)
            assertTrue("typo" in invalid.error?.message.orEmpty(), invalid.error?.message)
        }
        assertIs<ResultWithDiagnostics.Failure>(result)
        val errors = result.reports.filter { it.severity == ScriptDiagnostic.Severity.ERROR }.map { it.message }
        assertTrue(errors.any { "Unable to construct the annotation AnnotationWithDefault" in it && "typo" in it }, errors.toString())
        assertTrue(result.reports.none { it.severity == ScriptDiagnostic.Severity.WARNING && it.message in errors }, result.reports.toString())
    }

    @Test
    fun testInvalidAnnotationHandledByRefinementIsNotReportedAgain() {
        val result = compileViaLightTree(
            invalidAnnotationScript,
            invalidAnnotationScriptConfiguration { makeFailureResult("Invalid annotation handled".asErrorDiagnostics()) }
        )

        assertIs<ResultWithDiagnostics.Failure>(result)
        val errors = result.reports.filter { it.severity == ScriptDiagnostic.Severity.ERROR }.map { it.message }
        assertEquals(listOf("Invalid annotation handled"), errors)
    }

    private val invalidAnnotationScript =
        "@file:TestAnnotation(\"option\")\n@file:AnnotationWithDefault(typo = \"requested\")\n".toScriptSource("invalidAnnotation.kts")

    private fun invalidAnnotationScriptConfiguration(handler: RefineScriptCompilationConfigurationHandler) =
        ScriptCompilationConfiguration {
            defaultImports(TestAnnotation::class, AnnotationWithDefault::class)
            dependencies(JvmDependency(classpathFromClass(TestAnnotation::class).orEmpty()))
            refineConfiguration {
                onAnnotations(TestAnnotation::class, AnnotationWithDefault::class) { handler(it) }
            }
        }

    private fun compileViaLightTree(script: SourceCode, configuration: ScriptCompilationConfiguration): ResultWithDiagnostics<*> =
        ScriptJvmK2CompilerIsolated(defaultJvmScriptingHostConfiguration).compile(script, configuration)

    private fun collectAnnotationsViaPsi(
        source: SourceCode,
        configuration: ScriptCompilationConfiguration,
        classpath: List<File> = classpathFromClass(TestAnnotation::class).orEmpty(),
    ): ResultWithDiagnostics<List<Annotation>> {
        val compilationConfiguration = KotlinTestUtils.newConfiguration(ConfigurationKind.NO_KOTLIN_REFLECT, TestJdkKind.MOCK_JDK).apply {
            useFir = true
            updateWithBaseCompilerArguments()
            // the annotations are resolved against the compilation classpath, so the annotation classes should be on it
            addJvmClasspathRoots(classpath)
            loadScriptingPlugin(this, testRootDisposable)
        }
        val messageCollector = ScriptDiagnosticsMessageCollector(null)
        @OptIn(CoreEnvironmentDeprecation::class)
        val environment = KotlinCoreEnvironment.createForTests(
            testRootDisposable, compilationConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val context = createCompilationContextFromEnvironment(configuration, environment, messageCollector)
        val ktFile = getScriptKtFile(source, configuration, context.environment.project, messageCollector).valueOr { return it }
        if (messageCollector.hasErrors()) {
            return makeFailureResult(messageCollector.diagnostics)
        }
        return PsiScriptAnnotationsCollector { compilationConfiguration.jvmClasspathRoots }
            .collectAnnotations(ktFile, configuration, defaultJvmScriptingHostConfiguration)
            .onSuccess { data -> data[ScriptCollectedData.collectedAnnotations].orEmpty().map { it.annotation }.asSuccess() }
    }

    private fun annotations(filename: String, vararg classes: KClass<out Annotation>): ResultWithDiagnostics<List<Annotation>> {
        val file = ForTestCompileRuntime.transformTestDataPath(testDataPath + File.separator + filename)
        val configuration = ScriptCompilationConfiguration {
            defaultImports(*classes)
            jvm {
                refineConfiguration {
                    onAnnotations(*classes) {
                        it.compilationConfiguration.asSuccess()
                    }
                }
            }
        }
        val annotations = collectAnnotationsViaPsi(
            file.toScriptSource(), configuration, classes.flatMap { classpathFromClass(it).orEmpty() }
        ).valueOr { return it }

        annotations
            .filterIsInstance<InvalidScriptResolverAnnotation>()
            .takeIf { it.isNotEmpty() }
            ?.let { invalid ->
                val reports = invalid.map { "Failed to resolve annotation of type ${it.name} due to ${it.error}".asErrorDiagnostics() }
                return makeFailureResult(reports)
            }

        return annotations.asSuccess()
    }

}
