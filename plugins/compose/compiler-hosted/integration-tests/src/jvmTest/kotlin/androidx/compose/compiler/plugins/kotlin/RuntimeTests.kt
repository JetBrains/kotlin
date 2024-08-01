/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import androidx.compose.compiler.plugins.kotlin.lower.fastForEach
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.Suite
import org.junit.runners.model.FrameworkMethod
import java.io.File
import kotlin.test.Ignore

private const val RUNTIME_TEST_ROOT = "plugins/compose/compiler-hosted/runtime-tests/src"

/**
 * Takes Compose tests from runtime-tests module and runs them on compiler + plugin built from source.
 */
@Ignore // The tests cannot be run against jvmstubs
@RunWith(RuntimeTestsK1.RuntimeTestRunner::class)
class RuntimeTestsK1 {
    class RuntimeTestRunner(cls: Class<*>) : Suite(
        cls,
        createRuntimeRunners(useFir = false)
    )
}

@Ignore // The tests cannot be run against jvmstubs
@RunWith(RuntimeTestsK2.RuntimeTestRunner::class)
class RuntimeTestsK2 {
    class RuntimeTestRunner(cls: Class<*>) : Suite(
        cls,
        createRuntimeRunners(useFir = true)
    )
}

private fun createRuntimeRunners(useFir: Boolean): List<Runner> {
    AbstractCompilerTest.setSystemProperties()
    val compilers = mutableListOf(
        RuntimeTestCompiler(useFir, sourceInformation = false, optimizeNonSkippingGroups = false),
        RuntimeTestCompiler(useFir, sourceInformation = true, optimizeNonSkippingGroups = false),
        RuntimeTestCompiler(useFir, sourceInformation = false, optimizeNonSkippingGroups = true),
        RuntimeTestCompiler(useFir, sourceInformation = true, optimizeNonSkippingGroups = true)
    )

    val iterator = compilers.iterator()
    val result = mutableListOf<Runner>()
    while (iterator.hasNext()) {
        val compiler = iterator.next()
        val classes = compiler.compileRuntimeClasses()
        val description = compiler.description
        compiler.disposeTestRootDisposable()
        iterator.remove()
        classes.fastForEach { result.add(FirVariantRunner(it, description)) }
    }
    return result
}

private class FirVariantRunner(private val cls: Class<*>, val type: String) : BlockJUnit4ClassRunner(cls) {
    override fun testName(method: FrameworkMethod): String =
        "${method.name} [${cls.simpleName}]$type"
}

private val runtimeTestSourceRoot = File(RUNTIME_TEST_ROOT)
private val runtimeTestFiles = runtimeTestSourceRoot.walk().toSet()

@Ignore
private class RuntimeTestCompiler(
    useFir: Boolean,
    private val sourceInformation: Boolean,
    private val optimizeNonSkippingGroups: Boolean,
) : AbstractCodegenTest(useFir) {
    val description: String = "[source=$sourceInformation][groupOptimized=$optimizeNonSkippingGroups]"

    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, sourceInformation)
        put(ComposeConfiguration.TRACE_MARKERS_ENABLED_KEY, sourceInformation)
        if (optimizeNonSkippingGroups) {
            put(ComposeConfiguration.FEATURE_FLAGS,
                listOf(
                    FeatureFlag.OptimizeNonSkippingGroups.featureName,
                )
            )
        }
    }

    fun compileRuntimeClasses() =
        compileRuntimeTestClasses(
            runtimeTestSourceRoot,
            runtimeTestFiles.filter { !it.isDirectory && it.absolutePath.startsWith(runtimeTestSourceRoot.commonSourceRoot()) },
            runtimeTestFiles.filter { !it.isDirectory && it.absolutePath.startsWith(runtimeTestSourceRoot.jvmSourceRoot()) }
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
                Classpath.jarFor<Test>() // junit
            )
        )

        val parent = generatedClassLoader.parent
        val classLoader = object : ClassLoader(parent) {
            fun defineClass(name: String, bytes: ByteArray): Class<*> =
                defineClass(name, bytes, 0, bytes.size)
        }
        val classes = generatedClassLoader.allGeneratedFiles.mapNotNull { generatedFile ->
            if (generatedFile.relativePath.endsWith(".class")) {
                val className = generatedFile.relativePath.removeSuffix(".class").replace('/', '.')
                classLoader.defineClass(className, generatedFile.asByteArray())
                    .takeIf { cls ->
                        cls.methods.any { m -> m.annotations.any { it.annotationClass == Test::class } }
                    }
            } else {
                null
            }
        }

        return classes
    }
}

private fun File.commonSourceRoot() = "${absolutePath}/commonTest/kotlin"
private fun File.jvmSourceRoot() = "${absolutePath}/jvmTest/kotlin"
private fun File.toSourceFile(sourceRootPath: String) =
    SourceFile(name, readText(), path = absolutePath.removePrefix(sourceRootPath))
