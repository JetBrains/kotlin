/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.AbstractExecTask
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/**
 * A base class representing a final binary produced by the Kotlin/Native compiler
 * @param name - a name of the DSL entity.
 * @param baseName - a base name for the output binary file. E.g. for baseName foo we produce binaries foo.kexe, libfoo.so, foo.framework.
 * @param buildType - type of a binary: debug (not optimized, debuggable) or release (optimized, not debuggable)
 * @param compilation - a compilation used to produce this binary.
 *
 */
sealed class NativeBinary(
    private val name: String,
    open var baseName: String,
    val buildType: NativeBuildType,
    var compilation: KotlinNativeCompilation
) : Named {

    val target: KotlinNativeTarget
        get() = compilation.target

    val project: Project
        get() = target.project

    abstract val outputKind: NativeOutputKind

    // Configuration DSL.
    var debuggable: Boolean = buildType.debuggable
    var optimized: Boolean = buildType.optimized

    /** Additional options passed to the linker by the Kotlin/Native compiler. */
    var linkerOpts: MutableList<String> = mutableListOf()

    /** Additional options passed to the linker by the Kotlin/Native compiler. */
    fun linkerOpts(vararg options: String) {
        linkerOpts.addAll(options.toList())
    }

    /** Additional options passed to the linker by the Kotlin/Native compiler. */
    fun linkerOpts(options: Iterable<String>) {
        linkerOpts.addAll(options)
    }

    /** Additional arguments passed to the Kotlin/Native compiler. */
    var freeCompilerArgs: MutableList<String> = mutableListOf()

    // Link task access.
    val linkTaskName: String
        get() = lowerCamelCaseName("link", name, target.targetName)

    val linkTask: KotlinNativeLink
        get() = project.tasks.getByName(linkTaskName) as KotlinNativeLink

    // Output access.
    // TODO: Provide output configurations and integrate them with Gradle Native.
    val outputDirectory: File = with(project) {
        val targetSubDirectory = target.disambiguationClassifier?.let { "$it/" }.orEmpty()
        buildDir.resolve("bin/$targetSubDirectory${this@NativeBinary.name}")
    }

    val outputFile: File
        get() = linkTask.outputFile.get()

    // Named implementation.
    override fun getName(): String = name
}

class Executable constructor(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation,
    internal val isDefaultTestExecutable: Boolean
) : NativeBinary(name, baseName, buildType, compilation) {

    constructor(
        name: String,
        baseName: String,
        buildType: NativeBuildType,
        compilation: KotlinNativeCompilation) : this(name, baseName, buildType, compilation, false)

    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.EXECUTABLE

    override var baseName: String
        get() = super.baseName
        set(value) {
            super.baseName = value
            runTask?.executable = outputFile.absolutePath
        }

    var entryPoint: String? = null

    fun entryPoint(point: String?) {
        entryPoint = point
    }

    /**
     * A name of task running this executable.
     * Returns null if the executables's target is not a host one (macosX64, linuxX64 or mingw64).
     */
    val runTaskName: String?
        get() {
            if (target.konanTarget !in listOf(KonanTarget.MACOS_X64, KonanTarget.LINUX_X64, KonanTarget.MINGW_X64)) {
                return null
            }

            return if (isDefaultTestExecutable) {
                lowerCamelCaseName(compilation.target.targetName, AbstractKotlinTargetConfigurator.testTaskNameSuffix)
            } else {
                lowerCamelCaseName("run", name, compilation.target.targetName)
            }
        }

    /**
     * A task running this executable.
     * Returns null if the executables's target is not a host one (macosX64, linuxX64 or mingw64).
     */
    val runTask: AbstractExecTask<*>?
        get() = runTaskName?.let { project.tasks.getByName(it) as AbstractExecTask<*> }
}

class StaticLibrary(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : NativeBinary(name, baseName, buildType, compilation) {
    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.STATIC
}

class SharedLibrary(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : NativeBinary(name, baseName, buildType, compilation) {
    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.DYNAMIC
}

class Framework(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : NativeBinary(name, baseName, buildType, compilation) {

    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.FRAMEWORK

    // Export symbols from klibraries.
    val exportConfigurationName: String
        get() = target.disambiguateName(lowerCamelCaseName(name, "export"))

    /**
     * If dependencies added by the [export] method are resolved transitively or not.
     */
    var transitiveExport: Boolean
        get() = project.configurations.maybeCreate(exportConfigurationName).isTransitive
        set(value) {
            project.configurations.maybeCreate(exportConfigurationName).isTransitive = value
        }

    /**
     * Add a dependency to be exported in the framework.
     */
    fun export(dependency: Any) {
        project.dependencies.add(exportConfigurationName, dependency)
    }

    /**
     * Add a dependency to be exported in the framework.
     */
    fun export(dependency: Any, configure: Closure<*>) {
        project.dependencies.add(exportConfigurationName, dependency, configure)
    }

    /**
     * Add a dependency to be exported in the framework.
     */
    fun export(dependency: Any, configure: Action<in Dependency>) {
        project.dependencies.add(exportConfigurationName, dependency)?.let {
            configure.execute(it)
        }
    }

    // Embedding bitcode.
    /**
     * Embed bitcode for the framework or not. See [BitcodeEmbeddingMode].
     */
    var embedBitcode: BitcodeEmbeddingMode =
        if (target.konanTarget == KonanTarget.IOS_ARM64 || target.konanTarget == KonanTarget.IOS_ARM32) {
            buildType.iosEmbedBitcode
        } else {
            BitcodeEmbeddingMode.DISABLE
        }

    /**
     * Enable or disable embedding bitcode for the framework. See [BitcodeEmbeddingMode].
     */
    fun embedBitcode(mode: BitcodeEmbeddingMode) {
        embedBitcode = mode
    }

    /**
     * Enable or disable embedding bitcode for the framework.
     * The parameter [mode] is one of the following string constants:
     *
     *     disable - Don't embed LLVM IR bitcode.
     *     bitcode - Embed LLVM IR bitcode as data.
     *               Has the same effect as the -Xembed-bitcode command line option.
     *     marker - Embed placeholder LLVM IR data as a marker.
     *              Has the same effect as the -Xembed-bitcode-marker command line option.
     */
    fun embedBitcode(mode: String) = embedBitcode(BitcodeEmbeddingMode.valueOf(mode.toUpperCase()))

    enum class BitcodeEmbeddingMode {
        /** Don't embed LLVM IR bitcode. */
        DISABLE,
        /** Embed LLVM IR bitcode as data. */
        BITCODE,
        /** Embed placeholder LLVM IR data as a marker. */
        MARKER,
    }
}


