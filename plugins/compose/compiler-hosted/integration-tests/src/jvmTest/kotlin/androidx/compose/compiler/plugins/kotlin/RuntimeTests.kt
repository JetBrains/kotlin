/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.Suite
import org.junit.runners.model.FrameworkMethod
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test

private const val RUNTIME_TEST_ROOT = "plugins/compose/compiler-hosted/runtime-tests/src"

/**
 * Takes Compose tests from runtime-tests module and runs them on compiler + plugin built from source.
 */
@RunWith(RuntimeTests.RuntimeTestRunner::class)
class RuntimeTests {
    class RuntimeTestRunner(cls: Class<*>) : Suite(
        cls,
        createRuntimeRunners(cls)
    )
}

private fun createRuntimeRunners(cls: Class<*>): List<Runner> {
    AbstractCompilerTest.setSystemProperties()
    val compilers = listOf(
        RuntimeTestCompiler(useFir = false, sourceInformation = false),
        RuntimeTestCompiler(useFir = false, sourceInformation = true),
        RuntimeTestCompiler(useFir = true, sourceInformation = false),
        RuntimeTestCompiler(useFir = true, sourceInformation = true)
    )

    return compilers.flatMap { compiler ->
        val classes = compiler.compileRuntimeClasses()
        compiler.disposeTestRootDisposable()
        classes.map { FirVariantRunner(it, compiler.description) }
    }
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
    private val sourceInformation: Boolean
) : AbstractCodegenTest(useFir) {
    val description: String = "[k${if (useFir) "1" else "2"}][source=$sourceInformation]"

    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, sourceInformation)
        put(ComposeConfiguration.TRACE_MARKERS_ENABLED_KEY, sourceInformation)
    }

    fun compileRuntimeClasses() =
        compileRuntimeTestClasses(
            runtimeTestSourceRoot,
            runtimeTestFiles.filter { !it.isDirectory && it.absolutePath.startsWith(runtimeTestSourceRoot.commonSourceRoot()) },
            runtimeTestFiles.filter { !it.isDirectory && it.absolutePath.startsWith(runtimeTestSourceRoot.jvmSourceRoot()) }
        )

    private fun compileRuntimeTestClasses(sourceRoot: File, commonSources: List<File>, jvmSources: List<File>): List<Class<*>> {
        val compiledClassesLoader = createClassLoader(
            commonSourceFiles = commonSources.map { it.toSourceFile(sourceRoot.commonSourceRoot()) },
            platformSourceFiles = jvmSources.map { it.toSourceFile(sourceRoot.jvmSourceRoot()) }
        )

        val classes = compiledClassesLoader.allGeneratedFiles.mapNotNull { generatedFile ->
            if (generatedFile.relativePath.endsWith(".class")) {
                val className = generatedFile.relativePath.removeSuffix(".class").replace('/', '.')
                compiledClassesLoader.loadClass(className)
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
