/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.ThrowableRunnable
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.TestDisposable
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptDiagnosticsMessageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.createCompilationContextFromEnvironment
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.getScriptKtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.updateWithBaseCompilerArguments
import org.jetbrains.kotlin.scripting.resolve.InvalidScriptResolverAnnotation
import org.jetbrains.kotlin.scripting.resolve.getScriptCollectedData
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.RunAll
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.jvm

private const val testDataPath = "plugins/scripting/scripting-compiler/testData/compiler/constructAnnotations"

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
private annotation class TestAnnotation(vararg val options: String)

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
private annotation class AnnotationWithVarArgAndArray(vararg val options: String, val moreOptions: Array<String>)

class ConstructAnnotationTest : TestCase() {
    private val testRootDisposable: Disposable = TestDisposable("${ConstructAnnotationTest::class.simpleName}.testRootDisposable")

    override fun tearDown() {
        RunAll(
            ThrowableRunnable { Disposer.dispose(testRootDisposable) },
            ThrowableRunnable { super.tearDown() },
        )
    }

    fun testAnnotationEmptyVarArg() {
        val annotations = annotations("TestAnnotationEmptyVarArg.kts", TestAnnotation::class)
            .valueOrThrow()
            .filterIsInstance(TestAnnotation::class.java)

        assertEquals(annotations.count(), 1)
        assert(annotations.first().options.isEmpty())
    }

    fun testBasicVarArgTestAnnotation() {
        val annotations = annotations("SimpleTestAnnotation.kts", TestAnnotation::class)
            .valueOrThrow()
            .filterIsInstance(TestAnnotation::class.java)

        assertEquals(annotations.count(), 1)
        assertEquals(annotations.first().options.toList(), listOf("option"))
    }

    fun testAnnotationWithArrayLiteral() {
        val annotations = annotations("TestAnnotationWithArrayLiteral.kts", TestAnnotation::class)
            .valueOrThrow()
            .filterIsInstance(TestAnnotation::class.java)

        assertEquals(annotations.count(), 1)
        assertEquals(annotations.first().options.toList(), listOf("option"))
    }

    fun testAnnotationWithArrayOfFunction() {
        val annotations = annotations("TestAnnotationWithArrayOfFunction.kts", TestAnnotation::class)
            .valueOrThrow()
            .filterIsInstance(TestAnnotation::class.java)

        assertEquals(annotations.count(), 1)
        assertEquals(annotations.first().options.toList(), listOf("option"))
    }

    fun testAnnotationWithEmptyArrayFunction() {
        val annotations = annotations("TestAnnotationWithEmptyArrayFunction.kts", TestAnnotation::class)
            .valueOrThrow()
            .filterIsInstance(TestAnnotation::class.java)

        assertEquals(annotations.count(), 1)
        assert(annotations.first().options.isEmpty())
    }

    fun testArrayAfterVarArgInAnnotation() {
        val annotations = annotations("TestAnnotationWithVarArgAndArray.kts", AnnotationWithVarArgAndArray::class)
            .valueOrThrow()
            .filterIsInstance(AnnotationWithVarArgAndArray::class.java)

        assertEquals(annotations.count(), 1)
        assertEquals(annotations.first().options.toList(), listOf("option"))
        assertEquals(annotations.first().moreOptions.toList(), listOf("otherOption"))
    }

    private fun annotations(filename: String, vararg classes: KClass<out Annotation>): ResultWithDiagnostics<List<Annotation>> {
        val file = File(testDataPath, filename)
        val compilationConfiguration = KotlinTestUtils.newConfiguration(ConfigurationKind.NO_KOTLIN_REFLECT, TestJdkKind.MOCK_JDK).apply {
            updateWithBaseCompilerArguments()
            addKotlinSourceRoot(file.path)
            loadScriptingPlugin(this)
        }
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

        val messageCollector = ScriptDiagnosticsMessageCollector(null)
        val environment = KotlinCoreEnvironment.createForTests(
            testRootDisposable, compilationConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val context = createCompilationContextFromEnvironment(configuration, environment, messageCollector)
        val source = file.toScriptSource()
        val ktFile = getScriptKtFile(
            source,
            configuration,
            context.environment.project,
            messageCollector
        ).valueOr { return it }

        if (messageCollector.hasErrors()) {
            return makeFailureResult(messageCollector.diagnostics)
        }

        val data = getScriptCollectedData(ktFile, configuration, environment.project, null)
        val annotations = data[ScriptCollectedData.foundAnnotations] ?: emptyList()

        annotations
            .filterIsInstance(InvalidScriptResolverAnnotation::class.java)
            .takeIf { it.isNotEmpty() }
            ?.let { invalid ->
                val reports = invalid.map { "Failed to resolve annotation of type ${it.name} due to ${it.error}".asErrorDiagnostics() }
                return makeFailureResult(reports)
            }

        return annotations.asSuccess()
    }

}