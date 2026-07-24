/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import com.intellij.util.containers.orNull
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.testFederation.SmokeTest
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import java.io.File

private const val RUNTIME_TEST_ROOT = "plugins/compose/compiler-hosted/runtime-tests/src"

/**
 * Takes Compose tests from runtime-tests module and runs them on compiler + plugin built from source.
 */
@SmokeTest
class RuntimeTestsK2 {
    @TestFactory
    fun runtimeTests(): List<DynamicNode> = createRuntimeTestClasses().map { variant ->
        val description = variant.first
        val classes = variant.second
        dynamicContainer(
            description,
            classes.map { cls ->
                val testOutcomes = executeTestsFromClass(cls)
                val dynamicTests = forwardTestOutcomesAsDynamicTests(testOutcomes)
                dynamicContainer(cls.simpleName, dynamicTests.stream())
            }
        )
    }
}

private fun executeTestsFromClass(testClass: Class<*>): List<TestOutcome> {
    val launcher = LauncherFactory.create()
    val request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(testClass)).build()
    val listener = RecordingExecutionListener()

    launcher.execute(request, listener)

    return listener.testOutcomes
}

private fun forwardTestOutcomesAsDynamicTests(testOutcomes: List<TestOutcome>): List<DynamicTest> =
    testOutcomes.map { (testName, testResult) ->
        dynamicTest(testName) {
            if (testResult.status != SUCCESSFUL) {
                throw testResult.throwable.orNull() ?: AssertionError("Test failed")
            }
        }
    }

private data class TestOutcome(
    val testName: String,
    val testResult: TestExecutionResult,
)

private class RecordingExecutionListener : TestExecutionListener {
    val testOutcomes = mutableListOf<TestOutcome>()

    override fun executionFinished(identifier: TestIdentifier, result: TestExecutionResult) {
        when {
            identifier.isTest -> testOutcomes.add(TestOutcome(identifier.displayName, result))
            result.status != SUCCESSFUL -> testOutcomes.add(TestOutcome("[initialization]", result))
        }
    }
}

private fun createRuntimeTestClasses(): List<Pair<String, List<Class<*>>>> {
    AbstractCompilerTest.setSystemProperties()
    val compilers = mutableListOf(
        RuntimeTestCompiler(sourceInformation = false, optimizeNonSkippingGroups = false),
        RuntimeTestCompiler(sourceInformation = true, optimizeNonSkippingGroups = false),
        RuntimeTestCompiler(sourceInformation = false, optimizeNonSkippingGroups = true),
        RuntimeTestCompiler(sourceInformation = true, optimizeNonSkippingGroups = true)
    )

    val iterator = compilers.iterator()
    val result = mutableListOf<Pair<String, List<Class<*>>>>()
    while (iterator.hasNext()) {
        val compiler = iterator.next()
        val classes = compiler.compileRuntimeClasses()
        val description = compiler.description
        compiler.disposeTestRootDisposable()
        iterator.remove()
        result.add(description to classes)
    }
    return result
}


private val runtimeTestSourceRoot = File(RUNTIME_TEST_ROOT)
private val runtimeTestFiles = runtimeTestSourceRoot.walk().toSet()

private class RuntimeTestCompiler(
    private val sourceInformation: Boolean,
    private val optimizeNonSkippingGroups: Boolean,
) : AbstractCodegenTest() {
    val description: String = "[source=$sourceInformation][groupOptimized=$optimizeNonSkippingGroups]"

    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, sourceInformation)
        put(ComposeConfiguration.TRACE_MARKERS_ENABLED_KEY, sourceInformation)
        if (optimizeNonSkippingGroups) {
            put(
                ComposeConfiguration.FEATURE_FLAGS,
                listOf(
                    FeatureFlag.OptimizeNonSkippingGroups.featureName,
                )
            )
        }
    }

    fun compileRuntimeClasses() =
        compileRuntimeTestClasses(
            runtimeTestSourceRoot,
            runtimeTestFiles.filter {
                !it.isDirectory &&
                        it.absolutePath.startsWith(runtimeTestSourceRoot.commonSourceRoot())
            },
            runtimeTestFiles.filter {
                !it.isDirectory &&
                        it.absolutePath.startsWith(runtimeTestSourceRoot.jvmSourceRoot())
            }
        )

    private fun compileRuntimeTestClasses(sourceRoot: File, commonSources: List<File>, jvmSources: List<File>): List<Class<*>> {
        val generatedClassLoader = createClassLoader(
            commonSourceFiles = commonSources.map { it.toSourceFile(sourceRoot.commonSourceRoot()) },
            platformSourceFiles = jvmSources.map { it.toSourceFile(sourceRoot.jvmSourceRoot()) },
            additionalPaths = listOf(
                Classpath.composeTestUtilsJar(),
                Classpath.kotlinxCoroutinesJar(),
                Classpath.jarFor<kotlinx.coroutines.test.TestDispatcher>(), // kotlinx-coroutines-test
                Classpath.jarFor(kotlin.test.asserter::class.java.canonicalName), // kotlin-test metadata
                Classpath.jarFor<kotlin.test.Asserter>(), // kotlin-test
                Classpath.jarFor<Test>(), // junit
                Classpath.jarFor<SmokeTest>() // test-federation-runtime
            )
        )

        val parent = generatedClassLoader.parent
        val classLoader = object : ClassLoader(parent) {
            fun defineClass(name: String, bytes: ByteArray): Class<*> =
                defineClass(name, bytes, 0, bytes.size).also {
                    loadedClasses += it
                }

            val loadedClasses = mutableListOf<Class<*>>()
        }
        generatedClassLoader.allGeneratedFiles.forEach { generatedFile ->
            if (generatedFile.relativePath.endsWith(".class")) {
                val className = generatedFile.relativePath.removeSuffix(".class").replace('/', '.')
                classLoader.defineClass(className, generatedFile.asByteArray())
            }
        }

        val classes =
            classLoader.loadedClasses.filter { cls ->
                cls.methods.any { m -> m.annotations.any { it.annotationClass == Test::class } }
            }

        return classes
    }
}

private fun File.commonSourceRoot() = "${absolutePath}/commonTest/kotlin"
private fun File.jvmSourceRoot() = "${absolutePath}/jvmTest/kotlin"
private fun File.toSourceFile(sourceRootPath: String) =
    SourceFile(name, readText(), path = absolutePath.removePrefix(sourceRootPath))
