/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.other

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.useFir
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.PsiScriptAnnotationsCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptDiagnosticsMessageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.createCompilationContextFromEnvironment
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.getScriptKtFile
import org.jetbrains.kotlin.scripting.test.TestDisposable
import org.jetbrains.kotlin.scripting.test.definitions.TestAnnotation
import org.jetbrains.kotlin.scripting.test.updateWithBaseCompilerArguments
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.RunAll
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.classpathFromClass
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

private const val testDataPath = "plugins/scripting/scripting-tests/testData/definitions/constructAnnotations"

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class OtherTestAnnotation

/**
 * Covers the host-facing behaviour of the PSI-based annotation collecting (used by the legacy refinement entry points): the classpath
 * of the annotation resolution and skipping the scripts without the accepted annotations.
 */
class PsiScriptAnnotationsCollectorTest {
    private val testRootDisposable: Disposable = TestDisposable("${PsiScriptAnnotationsCollectorTest::class.simpleName}.testRootDisposable")

    @AfterTest
    fun tearDown() {
        RunAll(
            { Disposer.dispose(testRootDisposable) },
        )
    }

    private val baseConfiguration = ScriptCompilationConfiguration {
        defaultImports(TestAnnotation::class)
        refineConfiguration {
            onAnnotations(TestAnnotation::class) {
                it.compilationConfiguration.asSuccess()
            }
        }
    }

    @Test
    fun testAnnotationClassesAreResolvedWithRegularClasspath() {
        val environment = createEnvironment()
        // the annotations are resolved against the classpath of the regular compilation, which is expected to contain the annotation
        val collector = PsiScriptAnnotationsCollector { classpathFromClass(TestAnnotation::class).orEmpty() }
        val configurationWithMissingDependency = ScriptCompilationConfiguration(baseConfiguration) {
            dependencies(JvmDependency(File("someDependency.jar")))
        }
        collector.collect(environment, "SimpleTestAnnotation.kts", baseConfiguration)
        collector.collect(environment, "TestAnnotationEmptyVarArg.kts", baseConfiguration)
        collector.collect(environment, "SimpleTestAnnotation.kts", configurationWithMissingDependency)
    }

    @Test
    fun testAnnotationClassesAreResolvedWithNonNormalizedClasspath() {
        val environment = createEnvironment()
        // the same roots spelled differently, e.g. as the classpath could be passed by a build system
        val regularClasspath = classpathFromClass(TestAnnotation::class).orEmpty().map { entry ->
            val parent = entry.absoluteFile.parentFile
            File(parent, "..${File.separator}${parent.name}${File.separator}.${File.separator}${entry.name}")
        }
        PsiScriptAnnotationsCollector { regularClasspath }.collect(environment, "SimpleTestAnnotation.kts", baseConfiguration)
    }

    @Test
    fun testAnnotationClassesAreResolvedWithoutDeclaredClasspath() {
        val environment = createEnvironment()
        PsiScriptAnnotationsCollector().collect(environment, "SimpleTestAnnotation.kts", baseConfiguration)
    }

    @Test
    fun testNothingIsCreatedWithoutAcceptedAnnotations() {
        val environment = createEnvironment()
        val configuration = ScriptCompilationConfiguration {
            refineConfiguration {
                onAnnotations(OtherTestAnnotation::class) {
                    it.compilationConfiguration.asSuccess()
                }
            }
        }
        var classpathRequested = false
        val collector = PsiScriptAnnotationsCollector {
            classpathRequested = true
            emptyList()
        }
        val annotations = collector.collectAnnotations(
            scriptFile(environment, "SimpleTestAnnotation.kts"), configuration, defaultJvmScriptingHostConfiguration
        ).valueOrThrow()
        assertNull(annotations[ScriptCollectedData.collectedAnnotations])
        assertFalse(classpathRequested, "the classpath is not expected to be requested")
    }

    private fun PsiScriptAnnotationsCollector.collect(
        environment: KotlinCoreEnvironment, fileName: String, configuration: ScriptCompilationConfiguration,
    ) {
        val annotations =
            collectAnnotations(scriptFile(environment, fileName), configuration, defaultJvmScriptingHostConfiguration).valueOrThrow()
        assertEquals(1, annotations[ScriptCollectedData.collectedAnnotations]?.count { it.annotation is TestAnnotation })
    }

    private fun scriptFile(environment: KotlinCoreEnvironment, fileName: String): KtFile {
        val file = ForTestCompileRuntime.transformTestDataPath(testDataPath + File.separator + fileName)
        val messageCollector = ScriptDiagnosticsMessageCollector(null)
        val context = createCompilationContextFromEnvironment(baseConfiguration, environment, messageCollector)
        return getScriptKtFile(file.toScriptSource(), baseConfiguration, context.environment.project, messageCollector).valueOrThrow()
    }

    private fun createEnvironment(): KotlinCoreEnvironment {
        val compilerConfiguration = KotlinTestUtils.newConfiguration(ConfigurationKind.NO_KOTLIN_REFLECT, TestJdkKind.MOCK_JDK).apply {
            useFir = true
            updateWithBaseCompilerArguments()
            // as in a real compilation, the annotation classes are on the compilation classpath
            addJvmClasspathRoots(classpathFromClass(TestAnnotation::class).orEmpty())
            loadScriptingPlugin(this, testRootDisposable)
        }
        @OptIn(CoreEnvironmentDeprecation::class)
        return KotlinCoreEnvironment.createForTests(testRootDisposable, compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }
}
