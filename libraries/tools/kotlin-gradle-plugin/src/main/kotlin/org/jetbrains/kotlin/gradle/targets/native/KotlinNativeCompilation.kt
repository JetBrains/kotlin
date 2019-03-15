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
import org.gradle.api.invocation.Gradle
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import java.util.*

private const val OLD_BINARY_API_DEPRECATION =
    "Use the `binaries` block instead. See: https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#building-final-native-binaries"

private val usedDeprecatedAPIs = WeakHashMap<Project, MutableSet<String>>()

private fun showDeprecationWarning(gradle: Gradle) {
    val rootProject = gradle.rootProject
    val deprecatedAPIs = usedDeprecatedAPIs[rootProject]?.sorted()?.joinToString(separator = "\n") { "|    $it" }.orEmpty()
    if (deprecatedAPIs.isNotEmpty()) {
        rootProject.logger.warn(
            """
            |
            |Some native binaries in this build are configured using deprecated DSL elements. Use the `binaries` DSL block instead.
            |The following deprecated DSL elements are used in this build:
            $deprecatedAPIs
            |
            |See details about the `binaries` block at https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#building-final-native-binaries
            """.trimMargin()
        )
    }
}

private fun KotlinNativeCompilation.registerDeprecatedApi(api: String) = with(target.project.rootProject) {
    val deprecatedAPIs = usedDeprecatedAPIs.computeIfAbsent(this) {
        gradle.projectsEvaluated(::showDeprecationWarning)
        mutableSetOf()
    }
    deprecatedAPIs.add(api)
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
        get() = buildTypesNoWarn.also { registerDeprecatedApi("KotlinNativeCompilation.buildTypes") }
        set(value) {
            registerDeprecatedApi("KotlinNativeCompilation.buildTypes")
            buildTypesNoWarn = value
        }

    // Used to access output kinds from internals of the plugin without printing
    // the deprecation warning during user's project configuration.
    // TODO@binaries: Drop when the old API are removed.
    internal var outputKindsNoWarn = mutableListOf<NativeOutputKind>()

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    var outputKinds: MutableList<NativeOutputKind>
        get() = outputKindsNoWarn.also { registerDeprecatedApi("KotlinNativeCompilation.outputKinds") }
        set(value) {
            registerDeprecatedApi("KotlinNativeCompilation.outputKinds")
            outputKindsNoWarn = value
        }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun outputKind(kind: NativeOutputKind) = outputKinds.add(kind).also {
        registerDeprecatedApi("KotlinNativeCompilation.outputKind(...)")
    }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun outputKinds(vararg kinds: NativeOutputKind) {
        registerDeprecatedApi("KotlinNativeCompilation.outputKinds(...)")
        outputKinds = kinds.toMutableList()
    }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun outputKinds(vararg kinds: String) {
        registerDeprecatedApi("KotlinNativeCompilation.outputKinds(...)")
        outputKinds = kinds.map { NativeOutputKind.valueOf(it.toUpperCase()) }.toMutableList()
    }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun outputKinds(kinds: List<Any>) {
        registerDeprecatedApi("KotlinNativeCompilation.outputKinds(...)")
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
        get() = entryPointNoWarn.also { registerDeprecatedApi("KotlinNativeCompilation.entryPoint") }
        set(value) {
            registerDeprecatedApi("KotlinNativeCompilation.entryPoint")
            entryPointNoWarn = value
        }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun entryPoint(value: String) {
        registerDeprecatedApi("KotlinNativeCompilation.entryPoint(...)")
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
        get() = linkerOptsNoWarn.also { registerDeprecatedApi("KotlinNativeCompilation.linkerOpts") }
        set(value) {
            registerDeprecatedApi("KotlinNativeCompilation.linkerOpts")
            linkerOptsNoWarn = value
        }

    fun cinterops(action: Closure<Unit>) = cinterops(ConfigureUtil.configureUsing(action))
    fun cinterops(action: Action<NamedDomainObjectContainer<DefaultCInteropSettings>>) = action.execute(cinterops)

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun linkerOpts(vararg values: String) = linkerOpts(values.toList()).also {
        registerDeprecatedApi("KotlinNativeCompilation.linkerOpts(...)")
    }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun linkerOpts(values: List<String>) {
        registerDeprecatedApi("KotlinNativeCompilation.linkerOpts(...)")
        linkerOpts.addAll(values)
    }

    // Task accessors.
    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun findLinkTask(kind: NativeOutputKind, buildType: NativeBuildType): KotlinNativeLink? =
        binaries[kind to buildType]?.linkTask.also { registerDeprecatedApi("KotlinNativeCompilation.findLinkTask(...)") }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun getLinkTask(kind: NativeOutputKind, buildType: NativeBuildType): KotlinNativeLink =
        findLinkTask(kind, buildType).also { registerDeprecatedApi("KotlinNativeCompilation.getLinkTask(...)") }
            ?: throw IllegalArgumentException("Cannot find a link task for the binary kind '$kind' and the build type '$buildType'")

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun findLinkTask(kind: String, buildType: String) =
        findLinkTask(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))
            .also { registerDeprecatedApi("KotlinNativeCompilation.findLinkTask(...)") }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun getLinkTask(kind: String, buildType: String) =
        getLinkTask(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))
            .also { registerDeprecatedApi("KotlinNativeCompilation.getLinkTask(...)") }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun findBinary(kind: NativeOutputKind, buildType: NativeBuildType): File? =
        findLinkTask(kind, buildType)?.outputFile?.get().also { registerDeprecatedApi("KotlinNativeCompilation.findBinary(...)") }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun getBinary(kind: NativeOutputKind, buildType: NativeBuildType): File =
        getLinkTask(kind, buildType).outputFile.get().also { registerDeprecatedApi("KotlinNativeCompilation.getBinary(...)") }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun findBinary(kind: String, buildType: String) =
        findBinary(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))
            .also { registerDeprecatedApi("KotlinNativeCompilation.findBinary(...)") }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun getBinary(kind: String, buildType: String) =
        getBinary(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))
            .also { registerDeprecatedApi("KotlinNativeCompilation.getBinary(...)") }

    // Naming
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun linkTaskName(kind: NativeOutputKind, buildType: NativeBuildType): String =
        lowerCamelCaseName(
            "link",
            KotlinNativeBinaryContainer.generateBinaryName(compilationName, buildType, kind.taskNameClassifier),
            target.targetName
        ).also { registerDeprecatedApi("KotlinNativeCompilation.linkTaskName(...)") }

    @Deprecated(OLD_BINARY_API_DEPRECATION)
    fun linkTaskName(kind: String, buildType: String) =
        linkTaskName(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))
            .also { registerDeprecatedApi("KotlinNativeCompilation.linkTaskName(...)") }

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