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
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.name
import kotlin.io.path.writeText

interface KlibTestSourceModule {
    val name: String
    val kind: Kind
    val dependencies: List<KlibTestSourceModule>

    enum class Kind {
        REGULAR,
        CINTEROP,
    }
}

interface RegularKlibTestSourceModule : KlibTestSourceModule {
    val sourceFile: Path
}

interface CInteropKlibTestSourceModule : KlibTestSourceModule {
    val defFile: Path
    val headerFile: Path
}

class KlibTestSourceModules(
    val modules: List<KlibTestSourceModule>
)

internal interface KlibTestSourceModuleBuilder {
    fun dependsOn(dependencyName: String, vararg otherDependencyNames: String)
}

internal interface RegularKlibTestSourceModuleBuilder : KlibTestSourceModuleBuilder {
    fun sourceFileAddend(sourceFileAddend: String)
}

internal interface CInteropKlibTestSourceModuleBuilder : KlibTestSourceModuleBuilder {
    fun defFileAddend(defFileAddend: String)
    fun headerFileAddend(headerFileAddend: String)
}

internal interface KlibTestSourceModulesBuilder {
    fun addRegularModule(name: String, init: RegularKlibTestSourceModuleBuilder.() -> Unit = {})
    fun addCInteropModule(name: String, init: CInteropKlibTestSourceModuleBuilder.() -> Unit = {})
}

context(testRunner: AbstractNativeSimpleTest)
internal fun newSourceModules(init: KlibTestSourceModulesBuilder.() -> Unit): KlibTestSourceModules {
    // Private module implementation.
    abstract class BaseModuleImpl(
        override val name: String,
        val dependencyNames: List<String>,
    ) : KlibTestSourceModule {
        override lateinit var dependencies: List<KlibTestSourceModule>

        final override fun toString(): String = "Module \"$name\""
        final override fun hashCode() = name.hashCode()
        final override fun equals(other: Any?) = (other as? BaseModuleImpl)?.name == name
    }

    class RegularModuleImpl(
        name: String,
        val sourceFileAddend: String,
        dependencyNames: List<String>,
    ) : RegularKlibTestSourceModule, BaseModuleImpl(name, dependencyNames) {
        override lateinit var sourceFile: Path
        override val kind get() = KlibTestSourceModule.Kind.REGULAR
    }

    class CInteropModuleImpl(
        name: String,
        val defFileAddend: String,
        val headerFileAddend: String,
        dependencyNames: List<String>,
    ) : CInteropKlibTestSourceModule, BaseModuleImpl(name, dependencyNames) {
        override lateinit var defFile: Path
        override lateinit var headerFile: Path
        override val kind get() = KlibTestSourceModule.Kind.CINTEROP
    }

    // The list of source modules being built.
    val modules = mutableListOf<BaseModuleImpl>()

    // The builder for a single source module.
    abstract class ModuleBuilderImpl : KlibTestSourceModuleBuilder {
        var dependencyNames = emptyList<String>()

        override fun dependsOn(dependencyName: String, vararg otherDependencyNames: String) {
            dependencyNames = listOf(dependencyName) + otherDependencyNames.toList()
        }
    }

    class RegularModuleBuilderImpl : RegularKlibTestSourceModuleBuilder, ModuleBuilderImpl() {
        var sourceFileAddend = ""

        override fun sourceFileAddend(sourceFileAddend: String) {
            this.sourceFileAddend = sourceFileAddend
        }
    }

    class CInteropModuleBuilderImpl : CInteropKlibTestSourceModuleBuilder, ModuleBuilderImpl() {
        var defFileAddend = ""
        var headerFileAddend = ""

        override fun defFileAddend(defFileAddend: String) {
            this.defFileAddend = defFileAddend
        }

        override fun headerFileAddend(headerFileAddend: String) {
            this.headerFileAddend = headerFileAddend
        }
    }

    // The builder for all source modules.
    class ModulesBuilderImpl : KlibTestSourceModulesBuilder {
        override fun addRegularModule(name: String, init: RegularKlibTestSourceModuleBuilder.() -> Unit) {
            val builder = RegularModuleBuilderImpl()
            builder.init()
            modules += RegularModuleImpl(name, builder.sourceFileAddend, builder.dependencyNames)
        }

        override fun addCInteropModule(name: String, init: CInteropKlibTestSourceModuleBuilder.() -> Unit) {
            val builder = CInteropModuleBuilderImpl()
            builder.init()
            modules += CInteropModuleImpl(name, builder.defFileAddend, builder.headerFileAddend, builder.dependencyNames)
        }
    }

    // Build all source modules.
    val builder = ModulesBuilderImpl()
    builder.init()

    val nameToModuleMapping: Map<String, BaseModuleImpl> = modules.groupBy(KlibTestSourceModule::name).mapValues {
        it.value.singleOrNull() ?: error("Duplicated modules: ${it.value}")
    }

    // Initialize dependencies.
    modules.forEach { it.dependencies = it.dependencyNames.map(nameToModuleMapping::getValue) }

    val generatedSourcesDir = testRunner.buildDir.resolve("generated-sources").toPath()
    generatedSourcesDir.createDirectories()

    // Generate sources.
    modules.forEach { module ->
        when (module) {
            is RegularModuleImpl -> {
                module.sourceFile = generatedSourcesDir.resolve(module.name + ".kt")
                module.sourceFile.writeText(
                    buildString {
                        appendLine("package ${module.name}")
                        appendLine()
                        appendLine("fun ${module.name}(indent: Int) {")
                        appendLine("    repeat(indent) { print(\"  \") }")
                        appendLine("    println(\"${module.name}\")")
                        module.dependencies.forEach { dependency ->
                            if (dependency.kind == KlibTestSourceModule.Kind.CINTEROP) {
                                appendLine("    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)")
                            }
                            appendLine("    ${dependency.name}.${dependency.name}(indent + 1)")
                        }
                        appendLine("}")

                        if (module.sourceFileAddend.isNotBlank()) {
                            appendLine()
                            appendLine(module.sourceFileAddend)
                        }
                    }
                )
            }

            is CInteropModuleImpl -> {
                val onlyCInteropDependencies = module.dependencies.filterIsInstance<CInteropKlibTestSourceModule>()

                module.headerFile = generatedSourcesDir.resolve(module.name + ".h")
                module.headerFile.writeText(
                    buildString {
                        appendLine("#ifndef __${module.name}__")
                        appendLine("#define __${module.name}__")
                        appendLine()
                        if (onlyCInteropDependencies.isNotEmpty()) {
                            for (dependency in onlyCInteropDependencies) {
                                appendLine("#include \"${dependency.headerFile.name}\"")
                            }
                            appendLine()
                        }
                        appendLine("typedef struct ${module.name}_type { int x; int y; } ${module.name}_type;")
                        appendLine()
                        appendLine("void ${module.name}(int indent) {}")
                        appendLine()
                        if (onlyCInteropDependencies.isNotEmpty()) {
                            for (dependency in onlyCInteropDependencies) {
                                appendLine("void use_${dependency.name}_type_from_${module.name}(${dependency.name}_type* value) {}")
                            }
                            appendLine()
                        }
                        if (module.headerFileAddend.isNotBlank()) {
                            appendLine()
                            appendLine(module.headerFileAddend)
                        }
                        appendLine("#endif")
                    }
                )

                module.defFile = generatedSourcesDir.resolve(module.name + ".def")
                module.defFile.writeText(
                    buildString {
                        appendLine("headers=" + module.headerFile.name)
                        appendLine("depends=" + module.dependencies.map { it.name }.sorted().joinToString(" "))
                        if (module.defFileAddend.isNotBlank()) {
                            appendLine(module.defFileAddend)
                        }
                    }
                )
            }

            else -> error("Unknown module type: $module, ${module::class.java}")
        }
    }

    return KlibTestSourceModules(modules)
}

context(testRunner: AbstractNativeSimpleTest)
internal fun KlibTestSourceModules.compileToKlibsViaCli(
    produceUnpackedKlibs: Boolean = true,
    extraCliArgs: List<String> = emptyList(),
    transform: ((module: KlibTestSourceModule, successKlib: TestCompilationResult.Success<out KLIB>) -> Unit)? = null
) {
    val klibFilesDir = testRunner.buildDir.resolve(
        listOf(
            "klib-files",
            if (produceUnpackedKlibs) "unpacked" else "packed",
            if (transform != null) "transformed" else "non-transformed"
        ).joinToString(".")
    )
    klibFilesDir.mkdirs()

    fun KlibTestSourceModule.computeArtifactPath(): String {
        val basePath: String = klibFilesDir.resolve(name).path
        return if (produceUnpackedKlibs) basePath else "$basePath.klib"
    }

    modules.forEach { module ->
        val commonCompilerAndCInteropArgs: List<String> = buildList {
            if (produceUnpackedKlibs) add("-nopack")
            module.dependencies.forEach { dependency ->
                add("-l")
                add(dependency.computeArtifactPath())
            }
        }
        val compilerArgs = TestCompilerArgs(
            commonCompilerAndCInteropArgs + extraCliArgs,
            cinteropArgs = commonCompilerAndCInteropArgs,
        )

        val expectedArtifact = KLIB(klibFilesDir.resolve(module.computeArtifactPath()))

        val compilation = when (module) {
            is RegularKlibTestSourceModule -> {
                val testCase = testRunner.generateTestCaseWithSingleFile(
                    sourceFile = module.sourceFile.toFile(),
                    moduleName = module.name,
                    compilerArgs
                )

                LibraryCompilation(
                    settings = testRunner.testRunSettings,
                    freeCompilerArgs = testCase.freeCompilerArgs,
                    sourceModules = testCase.modules,
                    dependencies = emptySet(),
                    expectedArtifact = expectedArtifact
                )
            }

            is CInteropKlibTestSourceModule -> {
                check(extraCliArgs.isEmpty()) { "extraCmdLineParams are not allowed for cinterop modules" }

                val testCase = testRunner.generateTestCaseWithSingleFile(
                    sourceFile = module.defFile.toFile(),
                    moduleName = module.name,
                    compilerArgs
                )

                CInteropCompilation(
                    settings = testRunner.testRunSettings,
                    freeCompilerArgs = testCase.freeCompilerArgs,
                    defFile = module.defFile.toFile(),
                    sources = emptyList(),
                    dependencies = emptySet(),
                    expectedArtifact = expectedArtifact
                )
            }

            else -> error("Unknown module type: $module, ${module::class.java}")
        }

        val success = compilation.result.assertSuccess()
        transform?.invoke(module, success)
    }
}
