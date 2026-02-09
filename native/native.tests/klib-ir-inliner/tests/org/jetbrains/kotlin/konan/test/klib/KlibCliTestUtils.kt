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

interface KlibTestSourceModule {
    val name: String
    val kind: Kind
    val dependencies: List<KlibTestSourceModule>
    val sourceFile: File

    enum class Kind {
        REGULAR,
        CINTEROP,
    }
}

class KlibTestSourceModules(
    val modules: List<KlibTestSourceModule>
)

internal interface KlibTestSourceModuleBuilder {
    fun dependsOn(dependencyName: String, vararg otherDependencyNames: String)
    fun sourceFileAddend(sourceFileAddend: String)
}

internal interface KlibTestSourceModulesBuilder {
    fun addRegularModule(name: String, init: KlibTestSourceModuleBuilder.() -> Unit = {})
    fun addCInteropModule(name: String, init: KlibTestSourceModuleBuilder.() -> Unit = {})
}

context(testRunner: AbstractNativeSimpleTest)
internal fun newSourceModules(init: KlibTestSourceModulesBuilder.() -> Unit): KlibTestSourceModules {
    // Private module implementation.
    class ModuleImpl(
        override val name: String,
        override val kind: KlibTestSourceModule.Kind,
        val sourceFileAddend: String,
        val dependencyNames: List<String>,
    ) : KlibTestSourceModule {
        override lateinit var dependencies: List<KlibTestSourceModule>
        override lateinit var sourceFile: File

        override fun toString(): String = "Module \"$name\""
        override fun hashCode() = name.hashCode()
        override fun equals(other: Any?) = (other as? ModuleImpl)?.name == name
    }

    // The list of source modules being built.
    val modules = mutableListOf<ModuleImpl>()

    // The builder for a single source module.
    class ModuleBuilderImpl : KlibTestSourceModuleBuilder {
        var dependencyNames = emptyList<String>()
        var sourceFileAddend = ""

        override fun dependsOn(dependencyName: String, vararg otherDependencyNames: String) {
            dependencyNames = listOf(dependencyName) + otherDependencyNames.toList()
        }

        override fun sourceFileAddend(sourceFileAddend: String) {
            this.sourceFileAddend = sourceFileAddend
        }
    }

    // The builder for all source modules.
    class ModulesBuilderImpl : KlibTestSourceModulesBuilder {
        override fun addRegularModule(name: String, init: KlibTestSourceModuleBuilder.() -> Unit) =
            addModule(name, KlibTestSourceModule.Kind.REGULAR, init)

        override fun addCInteropModule(name: String, init: KlibTestSourceModuleBuilder.() -> Unit): Unit =
            addModule(name, KlibTestSourceModule.Kind.CINTEROP, init)

        fun addModule(name: String, kind: KlibTestSourceModule.Kind, init: KlibTestSourceModuleBuilder.() -> Unit) {
            val builder = ModuleBuilderImpl()
            builder.init()
            modules += ModuleImpl(name, kind, builder.sourceFileAddend, builder.dependencyNames)
        }
    }

    // Build all source modules.
    val builder = ModulesBuilderImpl()
    builder.init()

    val nameToModuleMapping: Map<String, ModuleImpl> = modules.groupBy(KlibTestSourceModule::name).mapValues {
        it.value.singleOrNull() ?: error("Duplicated modules: ${it.value}")
    }

    // Initialize dependencies.
    modules.forEach { it.dependencies = it.dependencyNames.map(nameToModuleMapping::getValue) }

    val generatedSourcesDir = testRunner.buildDir.resolve("generated-sources")
    generatedSourcesDir.mkdirs()

    // Generate sources.
    modules.forEach { module ->
        when (module.kind) {
            KlibTestSourceModule.Kind.REGULAR -> {
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

                    if (module.sourceFileAddend.isNotBlank()) {
                        appendLine()
                        appendLine(module.sourceFileAddend)
                    }
                })
            }

            KlibTestSourceModule.Kind.CINTEROP -> {
                module.sourceFile = generatedSourcesDir.resolve(module.name + ".def")
                module.sourceFile.writeText(buildString {
                    appendLine("---")
                    appendLine("#include <stdio.h>")
                    appendLine("static void ${module.name}(int indent) {")
                    appendLine("    for (int i = 0; i < indent; i++) printf(\" \");")
                    appendLine("    printf(\"%s\\n\", \"${module.name}\");")
                    appendLine("}")

                    if (module.sourceFileAddend.isNotBlank()) {
                        appendLine()
                        appendLine(module.sourceFileAddend)
                    }
                })
            }
        }
    }

    return KlibTestSourceModules(modules)
}

context(testRunner: AbstractNativeSimpleTest)
internal fun KlibTestSourceModules.compileToKlibsViaCli(
    produceUnpackedKlibs: Boolean = true,
    useLibraryNamesInCliArguments: Boolean = false,
    extraCliArgs: List<String> = emptyList(),
    transform: ((module: KlibTestSourceModule, successKlib: TestCompilationResult.Success<out KLIB>) -> Unit)? = null
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

    fun KlibTestSourceModule.computeArtifactPath(): String {
        val basePath: String = if (useLibraryNamesInCliArguments) name else klibFilesDir.resolve(name).path
        return if (produceUnpackedKlibs) basePath else "$basePath.klib"
    }

    fun doCompileModules() {
        modules.forEach { module ->
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
                    commonCompilerAndCInteropArgs + extraCliArgs,
                    cinteropArgs = commonCompilerAndCInteropArgs,
                )
            )

            val expectedArtifact = KLIB(klibFilesDir.resolve(module.computeArtifactPath()))
            val compilation = when (module.kind) {
                KlibTestSourceModule.Kind.REGULAR -> {
                    LibraryCompilation(
                        settings = testRunner.testRunSettings,
                        freeCompilerArgs = testCase.freeCompilerArgs,
                        sourceModules = testCase.modules,
                        dependencies = emptySet(),
                        expectedArtifact = expectedArtifact
                    )
                }

                KlibTestSourceModule.Kind.CINTEROP -> {
                    check(extraCliArgs.isEmpty()) { "extraCmdLineParams are not allowed for cinterop modules" }
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
