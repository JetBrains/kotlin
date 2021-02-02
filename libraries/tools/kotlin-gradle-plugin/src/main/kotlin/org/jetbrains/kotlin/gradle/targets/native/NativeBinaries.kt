/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
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
    baseNameProvided: String,
    val buildType: NativeBuildType,
    @Transient
    var compilation: KotlinNativeCompilation
) : Named {
    open var baseName: String
        get() = baseNameProvider.get()
        set(value) {
            baseNameProvider = project.provider { value }
        }
    internal var baseNameProvider: Provider<String> = project.provider { baseNameProvided }

    internal val konanTarget: KonanTarget
        get() = compilation.konanTarget

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
    var freeCompilerArgs: List<String>
        get() = linkTask.kotlinOptions.freeCompilerArgs
        set(value) {
            linkTask.kotlinOptions.freeCompilerArgs = value
        }

    // Link task access.
    val linkTaskName: String
        get() = lowerCamelCaseName("link", name, target.targetName)

    val linkTask: KotlinNativeLink
        get() = linkTaskProvider.get()

    val linkTaskProvider: TaskProvider<out KotlinNativeLink>
        get() = project.tasks.withType(KotlinNativeLink::class.java).named(linkTaskName)

    // Output access.
    // TODO: Provide output configurations and integrate them with Gradle Native.
    var outputDirectory: File = with(project) {
        val targetSubDirectory = target.disambiguationClassifier?.let { "$it/" }.orEmpty()
        buildDir.resolve("bin/$targetSubDirectory${this@NativeBinary.name}")
    }

    val outputFile: File by lazy {
        linkTask.outputFile.get()
    }

    // Named implementation.
    override fun getName(): String = name
}

abstract class AbstractExecutable(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : NativeBinary(name, baseName, buildType, compilation)

class Executable constructor(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : AbstractExecutable(name, baseName, buildType, compilation) {

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
     * A name of a task running this executable.
     * Returns null if the executables's target is not a host one (macosX64, linuxX64 or mingw64).
     */
    val runTaskName: String?
        get() = if (konanTarget in listOf(KonanTarget.MACOS_X64, KonanTarget.LINUX_X64, KonanTarget.MINGW_X64)) {
            lowerCamelCaseName("run", name, compilation.target.targetName)
        } else {
            null
        }

    /**
     * A task running this executable.
     * Returns null if the executables's target is not a host one (macosX64, linuxX64 or mingw64).
     */
    val runTask: AbstractExecTask<*>?
        get() = runTaskName?.let { project.tasks.getByName(it) as AbstractExecTask<*> }
}

class TestExecutable(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : AbstractExecutable(name, baseName, buildType, compilation) {

    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.TEST
}

abstract class AbstractNativeLibrary(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : NativeBinary(name, baseName, buildType, compilation) {

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
}

class StaticLibrary(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : AbstractNativeLibrary(name, baseName, buildType, compilation) {
    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.STATIC
}

class SharedLibrary(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : AbstractNativeLibrary(name, baseName, buildType, compilation) {
    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.DYNAMIC
}

class Framework(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : AbstractNativeLibrary(name, baseName, buildType, compilation), HasAttributes {

    private val attributeContainer = HierarchyAttributeContainer(parent = compilation.attributes)

    override fun getAttributes() = attributeContainer

    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.FRAMEWORK

    // Embedding bitcode.
    /**
     * Embed bitcode for the framework or not. See [BitcodeEmbeddingMode].
     */
    var embedBitcode: BitcodeEmbeddingMode = buildType.embedBitcode(konanTarget)

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

    /**
     * Specifies if the framework is linked as a static library (false by default).
     */
    var isStatic = false

    enum class BitcodeEmbeddingMode {
        /** Don't embed LLVM IR bitcode. */
        DISABLE,

        /** Embed LLVM IR bitcode as data. */
        BITCODE,

        /** Embed placeholder LLVM IR data as a marker. */
        MARKER,
    }

    companion object {
        val frameworkTargets = Attribute.of(
            "org.jetbrains.kotlin.native.framework.targets",
            Set::class.java
        )
    }
}


