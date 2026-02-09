/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.generateTestCaseWithSingleFile
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CInteropCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import java.io.File

internal data class TestKlibModule(
    val name: String,
    val dependencyNames: List<String>,
    val kind: Kind,
) {
    constructor(name: String, vararg dependencyNames: String, kind: Kind = Kind.REGULAR) : this(
        name,
        dependencyNames.asList(),
        kind
    )

    lateinit var dependencies: List<TestKlibModule>
    lateinit var sourceFile: File

    fun initDependencies(resolveDependency: (String) -> TestKlibModule) {
        dependencies = dependencyNames.map(resolveDependency)
    }

    enum class Kind {
        REGULAR,
        CINTEROP,
    }
}

context(testRunner: AbstractNativeSimpleTest)
internal fun createModules(vararg modules: TestKlibModule): List<TestKlibModule> {
    val mapping: Map<String, TestKlibModule> = modules.groupBy(TestKlibModule::name).mapValues {
        it.value.singleOrNull() ?: error("Duplicated modules: ${it.value}")
    }

    modules.forEach { it.initDependencies(mapping::getValue) }

    val generatedSourcesDir = testRunner.buildDir.resolve("generated-sources")
    generatedSourcesDir.mkdirs()

    modules.forEach { module ->
        when (module.kind) {
            TestKlibModule.Kind.REGULAR -> {
                module.sourceFile = generatedSourcesDir.resolve(module.name + ".kt")
                module.sourceFile.writeText(buildString {
                    appendLine("package ${module.name}")
                    appendLine()
                    appendLine("fun ${module.name}(indent: Int) {")
                    appendLine("    repeat(indent) { print(\"  \") }")
                    appendLine("    println(\"${module.name}\")")
                    module.dependencyNames.forEach { dependencyName ->
                        appendLine("    $dependencyName.$dependencyName(indent + 1)")
                    }
                    appendLine("}")
                })
            }
            TestKlibModule.Kind.CINTEROP -> {
                module.sourceFile = generatedSourcesDir.resolve(module.name + ".def")
                module.sourceFile.writeText(buildString {
                    appendLine("---")
                    appendLine("#include <stdio.h>")
                    appendLine("static void ${module.name}(int indent) {")
                    appendLine("    for (int i = 0; i < indent; i++) printf(\" \");")
                    appendLine("    printf(\"%s\\n\", \"${module.name}\");")
                    appendLine("}")
                })
            }
        }
    }

    return modules.asList()
}

context(testRunner: AbstractNativeSimpleTest)
internal fun List<TestKlibModule>.compileModules(
    produceUnpackedKlibs: Boolean,
    useLibraryNamesInCliArguments: Boolean,
    extraCmdLineParams: List<String> = emptyList(),
    transform: ((module: TestKlibModule, successKlib: TestCompilationResult.Success<out KLIB>) -> Unit)? = null
) {
    val klibFilesDir = testRunner.buildDir.resolve(
        listOf(
            "klib-files",
            if (produceUnpackedKlibs) "unpacked" else "packed",
            if (useLibraryNamesInCliArguments) "names" else "paths",
            if (transform != null) "transformed" else "non-transformed"
        ).joinToString(".")
    )
    klibFilesDir.mkdirs()

    fun TestKlibModule.computeArtifactPath(): String {
        val basePath: String = if (useLibraryNamesInCliArguments) name else klibFilesDir.resolve(name).path
        return if (produceUnpackedKlibs) basePath else "$basePath.klib"
    }

    fun doCompileModules() {
        forEach { module ->
            val commonCompilerAndCInteropArgs = buildList {
                if (produceUnpackedKlibs) add("-nopack")
                module.dependencies.forEach { dependency ->
                    add("-l")
                    add(dependency.computeArtifactPath())
                }
            }
            val testCase = testRunner.generateTestCaseWithSingleFile(
                sourceFile = module.sourceFile,
                moduleName = module.name,
                TestCompilerArgs(
                    commonCompilerAndCInteropArgs + extraCmdLineParams,
                    cinteropArgs = commonCompilerAndCInteropArgs,
                )
            )

            val expectedArtifact = KLIB(klibFilesDir.resolve(module.computeArtifactPath()))
            val compilation = when (module.kind) {
                TestKlibModule.Kind.REGULAR -> {
                    LibraryCompilation(
                        settings = testRunner.testRunSettings,
                        freeCompilerArgs = testCase.freeCompilerArgs,
                        sourceModules = testCase.modules,
                        dependencies = emptySet(),
                        expectedArtifact = expectedArtifact
                    )
                }
                TestKlibModule.Kind.CINTEROP -> {
                    check(extraCmdLineParams.isEmpty()) { "extraCmdLineParams are not allowed for cinterop modules" }
                    CInteropCompilation(
                        settings = testRunner.testRunSettings,
                        freeCompilerArgs = testCase.freeCompilerArgs,
                        defFile = module.sourceFile,
                        sources = emptyList(),
                        dependencies = emptySet(),
                        expectedArtifact = expectedArtifact
                    )
                }
            }

            val success = compilation.result.assertSuccess()
            transform?.invoke(module, success)
        }
    }

    if (useLibraryNamesInCliArguments)
        runWithCustomWorkingDir(klibFilesDir) { doCompileModules() }
    else
        doCompileModules()
}

internal inline fun runWithCustomWorkingDir(customWorkingDir: File, block: () -> Unit) {
    val previousWorkingDir: String = System.getProperty(USER_DIR)
    try {
        System.setProperty(USER_DIR, customWorkingDir.absolutePath)
        block()
    } finally {
        System.setProperty(USER_DIR, previousWorkingDir)
    }
}

private const val USER_DIR = "user.dir"
