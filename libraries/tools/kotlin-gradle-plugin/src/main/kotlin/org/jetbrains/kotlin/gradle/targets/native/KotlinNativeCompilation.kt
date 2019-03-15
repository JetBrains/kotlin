 /*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch", "DEPRECATION") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File

private const val OLD_BINARY_API_DEPRECATION =
    "Use the `binaries` block instead. See: https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#building-final-native-binaries"

private fun KotlinNativeCompilation.printDeprecationWarning() {
    SingleWarningPerBuild.show(
        target.project,
        """
            Some native binaries in this build are configured using deprecated APIs. Use the `binaries` block instead.
            The following APIs are deprecated:
                KotlinNativeCompilation.buildTypes
                KotlinNativeCompilation.outputKinds
                KotlinNativeCompilation.outputKinds(...)
                KotlinNativeCompilation.outputKind(...)
                KotlinNativeCompilation.entryPoint
                KotlinNativeCompilation.entryPoint(...)
                KotlinNativeCompilation.linkerOpts
                KotlinNativeCompilation.linkerOpts(...)
                KotlinNativeCompilation.findLinkTask(...)
                KotlinNativeCompilation.getLinkTask(...)
                KotlinNativeCompilation.findBinary(...)
                KotlinNativeCompilation.getBinary(...)
                KotlinNativeCompilation.linkTaskName(...)


            See details about the `binaries` block at https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#building-final-native-binaries
        """.trimIndent()
    )
}

class KotlinNativeCompilation(
    override val target: KotlinNativeTarget,
    name: String
) : AbstractKotlinCompilation<KotlinCommonOptions>(target, name), KotlinCompilationWithResources<KotlinCommonOptions> {

    override val kotlinOptions: KotlinCommonOptions
        get() = compileKotlinTask.kotlinOptions

    override val compileKotlinTask: KotlinNativeCompile
        get() = super.compileKotlinTask as KotlinNativeCompile

    private val project: Project
        get() = target.project

    // A collection containing all source sets used by this compilation
    // (taking into account dependencies between source sets). Used by both compilation
    // and linking tasks. Unlike kotlinSourceSets, includes dependency source sets.
    // TODO: Move into the compilation task when the linking task does klib linking instead of compilation.
    internal val allSources: MutableSet<SourceDirectorySet> = mutableSetOf()

    // TODO: Move into the compilation task when the linking task does klib linking instead of compilation.
    internal val commonSources: MutableSet<SourceDirectorySet> = mutableSetOf()

    var isTestCompilation = false

    var friendCompilationName: String? = null

    internal val friendCompilation: KotlinNativeCompilation?
        get() = friendCompilationName?.let {
            target.compilations.getByName(it)
        }

     // Used only to support the old APIs. TODO@binaries: Remove when the old APIs are removed.
    internal val binaries = mutableMapOf<Pair<NativeOutputKind, NativeBuildType>, NativeBinary>()

    // Native-specific DSL.
    var extraOpts = mutableListOf<String>()

    fun extraOpts(vararg values: Any) = extraOpts(values.toList())
    fun extraOpts(values: List<Any>) {
        extraOpts.addAll(values.map { it.toString() })
    }

    // Used to access build types from internals of the plugin without printing
    // the deprecation warning during user's project configuration.
    // TODO@binaries: Drop when the old API are removed.
    internal var buildTypesNoWarn = mutableListOf<NativeBuildType>()

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    var buildTypes: MutableList<NativeBuildType>
        get() = buildTypesNoWarn.also { printDeprecationWarning() }
        set(value) {
            printDeprecationWarning()
            buildTypesNoWarn = value
        }

    // Used to access output kinds from internals of the plugin without printing
    // the deprecation warning during user's project configuration.
    // TODO@binaries: Drop when the old API are removed.
    internal var outputKindsNoWarn = mutableListOf<NativeOutputKind>()

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    var outputKinds: MutableList<NativeOutputKind>
        get() = outputKindsNoWarn.also { printDeprecationWarning() }
        set(value) {
            printDeprecationWarning()
            outputKindsNoWarn = value
        }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun outputKind(kind: NativeOutputKind) = outputKinds.add(kind)

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun outputKinds(vararg kinds: NativeOutputKind) {
        outputKinds = kinds.toMutableList()
    }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun outputKinds(vararg kinds: String) {
        outputKinds = kinds.map { NativeOutputKind.valueOf(it.toUpperCase()) }.toMutableList()
    }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun outputKinds(kinds: List<Any>) {
        outputKinds = kinds.map {
            when (it) {
                is NativeOutputKind -> it
                is String -> NativeOutputKind.valueOf(it.toUpperCase())
                else -> error("Cannot use $it as an output kind")
            }
        }.toMutableList()
    }

    // Used to access entry point from internals of the plugin without printing
    // the deprecation warning during user's project configuration.
    // TODO@binaries: Drop when the old API are removed.
    internal var entryPointNoWarn: String? = null

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    var entryPoint: String?
        get() = entryPointNoWarn.also { printDeprecationWarning() }
        set(value) {
            printDeprecationWarning()
            entryPointNoWarn = value
        }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun entryPoint(value: String) {
        entryPoint = value
    }

    // Interop DSL.
    val cinterops = project.container(DefaultCInteropSettings::class.java) { cinteropName ->
        DefaultCInteropSettings(project, cinteropName, this)
    }

    // Used to access linker options from internals of the plugin without printing
    // the deprecation warning during user's project configuration.
    // TODO@binaries: Drop when the old API are removed.
    internal var linkerOptsNoWarn = mutableListOf<String>()

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    var linkerOpts: MutableList<String>
        get() = linkerOptsNoWarn.also { printDeprecationWarning() }
        set(value) {
            printDeprecationWarning()
            linkerOptsNoWarn = value
        }

    fun cinterops(action: Closure<Unit>) = cinterops(ConfigureUtil.configureUsing(action))
    fun cinterops(action: Action<NamedDomainObjectContainer<DefaultCInteropSettings>>) = action.execute(cinterops)

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun linkerOpts(vararg values: String) = linkerOpts(values.toList())
    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun linkerOpts(values: List<String>) {
        linkerOpts.addAll(values)
    }

    // Task accessors.
    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun findLinkTask(kind: NativeOutputKind, buildType: NativeBuildType): KotlinNativeLink? =
        binaries[kind to buildType]?.linkTask.also { printDeprecationWarning() }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun getLinkTask(kind: NativeOutputKind, buildType: NativeBuildType): KotlinNativeLink =
        findLinkTask(kind, buildType).also { printDeprecationWarning() }
            ?: throw IllegalArgumentException("Cannot find a link task for the binary kind '$kind' and the build type '$buildType'")

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun findLinkTask(kind: String, buildType: String) =
        findLinkTask(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))
            .also { printDeprecationWarning() }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun getLinkTask(kind: String, buildType: String) =
        getLinkTask(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))
            .also { printDeprecationWarning() }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun findBinary(kind: NativeOutputKind, buildType: NativeBuildType): File? =
        findLinkTask(kind, buildType)?.outputFile?.get().also { printDeprecationWarning() }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun getBinary(kind: NativeOutputKind, buildType: NativeBuildType): File =
        getLinkTask(kind, buildType).outputFile.get().also { printDeprecationWarning() }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun findBinary(kind: String, buildType: String) =
        findBinary(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))
            .also { printDeprecationWarning() }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun getBinary(kind: String, buildType: String) =
        getBinary(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))
            .also { printDeprecationWarning() }

    // Naming
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun linkTaskName(kind: NativeOutputKind, buildType: NativeBuildType): String =
        lowerCamelCaseName(
            "link",
            KotlinNativeBinaryContainer.generateBinaryName(compilationName, buildType, kind.taskNameClassifier),
            target.targetName
        ).also { printDeprecationWarning() }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun linkTaskName(kind: String, buildType: String) =
        linkTaskName(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))
            .also { printDeprecationWarning() }

    override val compileDependencyConfigurationName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "compileKlibraries"
        )

    override val compileAllTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName, "klibrary")

    val binariesTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName, "binaries")

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Boolean) {
        allSources.add(sourceSet.kotlin)
        if (addAsCommonSources) {
            commonSources.add(sourceSet.kotlin)
        }
    }
}