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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginPublicDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.utils.attributeOf
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.io.File

/**
 * A base class representing a final binary produced by the Kotlin/Native compiler
 * @param name - a name of the DSL entity.
 * @param baseName - a base name for the output binary file. E.g. for baseName foo we produce binaries foo.kexe, libfoo.so, foo.framework.
 * @param buildType - type of a binary: debug (not optimized, debuggable) or release (optimized, not debuggable)
 * @param compilation - a compilation used to produce this binary.
 *
 */
@KotlinGradlePluginPublicDsl
sealed class NativeBinary(
    private val name: String,
    open var baseName: String,
    val buildType: NativeBuildType,
    @Transient
    var compilation: KotlinNativeCompilation
) : Named {
    internal val baseNameProvider: Provider<String> = project.provider { baseName }

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

    var binaryOptions: MutableMap<String, String> = mutableMapOf()

    fun binaryOption(name: String, value: String) {
        // TODO: report if $name is unknown?
        binaryOptions[name] = value
    }

    /** Additional arguments passed to the Kotlin/Native compiler. */
    @Suppress("DEPRECATION")
    var freeCompilerArgs: List<String>
        get() = linkTask.kotlinOptions.freeCompilerArgs
        set(value) {
            linkTask.kotlinOptions.freeCompilerArgs = value
        }

    // Link task access.
    val linkTaskName: String
        get() = lowerCamelCaseName("link", name, target.targetName)

    @Deprecated("Use 'linkTaskProvider' instead", ReplaceWith("linkTaskProvider"))
    val linkTask: KotlinNativeLink
        get() = linkTaskProvider.get()

    val linkTaskProvider: TaskProvider<out KotlinNativeLink>
        get() = project.tasks.withType(KotlinNativeLink::class.java).named(linkTaskName)

    // Output access.
    // TODO: Provide output configurations and integrate them with Gradle Native.
    var outputDirectory: File
        get() = outputDirectoryProperty.get().asFile
        set(value) = outputDirectoryProperty.set(value)

    val outputDirectoryProperty: DirectoryProperty = with(project) {
        val targetSubDirectory = target.disambiguationClassifier?.let { "$it/" }.orEmpty()
        objects.directoryProperty().convention(layout.buildDirectory.dir("bin/$targetSubDirectory${this@NativeBinary.name}"))
    }

    private val outputFileProvider: Provider<File> by lazy {
        linkTaskProvider.flatMap { it.outputFile }
    }

    val outputFile: File
        get() = outputFileProvider.get()

    // Named implementation.
    override fun getName(): String = name
}

@KotlinGradlePluginPublicDsl
abstract class AbstractExecutable(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : NativeBinary(name, baseName, buildType, compilation)

@KotlinGradlePluginPublicDsl
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
            runTaskProvider?.configure {
                it.executable = outputFile.absolutePath
            }
        }

    /**
     * The fully qualified name of the main function. For an example:
     *
     * - "main"
     * - "com.example.main"
     *
     *  The main function can either take no arguments or an Array<String>.
     */
    var entryPoint: String? = null

    /**
     * Set the fully qualified name of the main function. For an example:
     *
     * - "main"
     * - "com.example.main"
     *
     *  The main function can either take no arguments or an Array<String>.
     */
    fun entryPoint(point: String?) {
        entryPoint = point
    }

    /**
     * A name of a task running this executable.
     * Returns null if the executables's target is not a host one (macosArm64, macosX64, linuxX64 or mingw64).
     */
    val runTaskName: String?
        get() = if (konanTarget in listOf(KonanTarget.MACOS_ARM64, KonanTarget.MACOS_X64, KonanTarget.LINUX_X64, KonanTarget.MINGW_X64)) {
            lowerCamelCaseName("run", name, compilation.target.targetName)
        } else {
            null
        }

    /**
     * A task running this executable.
     * Returns null if the executables's target is not a host one (macosArm64, macosX64, linuxX64 or mingw64).
     */
    val runTaskProvider: TaskProvider<AbstractExecTask<*>>?
        get() = runTaskName?.let { project.tasks.withType(AbstractExecTask::class.java).named(it) }

    val runTask: AbstractExecTask<*>?
        get() = runTaskProvider?.get()
}

@KotlinGradlePluginPublicDsl
class TestExecutable(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : AbstractExecutable(name, baseName, buildType, compilation) {

    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.TEST
}

@KotlinGradlePluginPublicDsl
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
    @ExperimentalKotlinGradlePluginApi
    var transitiveExport: Boolean
        get() = project.configurations.maybeCreateResolvable(exportConfigurationName).isTransitive
        set(value) {
            project.configurations.maybeCreateResolvable(exportConfigurationName).isTransitive = value
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

@KotlinGradlePluginPublicDsl
class StaticLibrary(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : AbstractNativeLibrary(name, baseName, buildType, compilation) {
    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.STATIC
}

@KotlinGradlePluginPublicDsl
class SharedLibrary(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : AbstractNativeLibrary(name, baseName, buildType, compilation) {
    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.DYNAMIC
}

@KotlinGradlePluginPublicDsl
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
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE)
    /**
     * Embed bitcode for the framework or not. See [BitcodeEmbeddingMode].
     */
    val embedBitcodeMode = project.objects.property(org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode::class.java)

    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE)
    var embedBitcode: org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode = org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.DISABLE

    /**
     * Enable or disable embedding bitcode for the framework. See [BitcodeEmbeddingMode].
     */
    @Suppress("DEPRECATION")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, replaceWith = ReplaceWith(""))
    fun embedBitcode(mode: org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode) {
        embedBitcodeMode.set(mode)
    }

    /**
     * [embedBitcode] is deprecated and has no effect
     */
    @Suppress("DEPRECATION")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, replaceWith = ReplaceWith(""))
    fun embedBitcode(mode: String) = embedBitcode(org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.valueOf(mode.toUpperCaseAsciiOnly()))

    /**
     * Specifies if the framework is linked as a static library (false by default).
     */
    var isStatic = false

    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE)
    object BitcodeEmbeddingMode {
        val DISABLE = org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.DISABLE
        val BITCODE = org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.BITCODE
        val MARKER = org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.MARKER
    }

    companion object {
        val frameworkTargets: Attribute<Set<String>> = attributeOf<Set<String>>(
            "org.jetbrains.kotlin.native.framework.targets"
        )
    }
}


