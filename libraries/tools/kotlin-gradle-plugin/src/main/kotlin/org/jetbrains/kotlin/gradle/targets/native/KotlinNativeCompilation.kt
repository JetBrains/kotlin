 /*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
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
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File

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

     // Used only to support the old APIs. TODO: Remove when the old APIs are removed.
    internal val binaries = mutableMapOf<Pair<NativeOutputKind, NativeBuildType>, NativeBinary>()

    // Native-specific DSL.
    var extraOpts = mutableListOf<String>()

    fun extraOpts(vararg values: Any) = extraOpts(values.toList())
    fun extraOpts(values: List<Any>) {
        extraOpts.addAll(values.map { it.toString() })
    }

    var buildTypes = mutableListOf<NativeBuildType>()
    var outputKinds = mutableListOf<NativeOutputKind>()

    fun outputKind(kind: NativeOutputKind) = outputKinds.add(kind)

    fun outputKinds(vararg kinds: NativeOutputKind) {
        outputKinds = kinds.toMutableList()
    }

    fun outputKinds(vararg kinds: String) {
        outputKinds = kinds.map { NativeOutputKind.valueOf(it.toUpperCase()) }.toMutableList()
    }

    fun outputKinds(kinds: List<Any>) {
        outputKinds = kinds.map {
            when (it) {
                is NativeOutputKind -> it
                is String -> NativeOutputKind.valueOf(it.toUpperCase())
                else -> error("Cannot use $it as an output kind")
            }
        }.toMutableList()
    }

    var entryPoint: String? = null
    fun entryPoint(value: String) {
        entryPoint = value
    }

    // Interop DSL.
    val cinterops = project.container(DefaultCInteropSettings::class.java) { cinteropName ->
        DefaultCInteropSettings(project, cinteropName, this)
    }

    var linkerOpts = mutableListOf<String>()

    fun cinterops(action: NamedDomainObjectContainer<DefaultCInteropSettings>.() -> Unit) = cinterops.action()
    fun cinterops(action: Closure<Unit>) = cinterops(ConfigureUtil.configureUsing(action))
    fun cinterops(action: Action<NamedDomainObjectContainer<DefaultCInteropSettings>>) = action.execute(cinterops)

    fun linkerOpts(vararg values: String) = linkerOpts(values.toList())
    fun linkerOpts(values: List<String>) {
        linkerOpts.addAll(values)
    }

    // Task accessors.
    fun findLinkTask(kind: NativeOutputKind, buildType: NativeBuildType): KotlinNativeLink? = binaries[kind to buildType]?.linkTask

    fun getLinkTask(kind: NativeOutputKind, buildType: NativeBuildType): KotlinNativeLink =
        findLinkTask(kind, buildType)
            ?: throw IllegalArgumentException("Cannot find a link task for the binary kind '$kind' and the build type '$buildType'")

    fun findLinkTask(kind: String, buildType: String) =
        findLinkTask(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))

    fun getLinkTask(kind: String, buildType: String) =
        getLinkTask(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))

    fun findBinary(kind: NativeOutputKind, buildType: NativeBuildType): File? = findLinkTask(kind, buildType)?.outputFile?.get()

    fun getBinary(kind: NativeOutputKind, buildType: NativeBuildType): File = getLinkTask(kind, buildType).outputFile.get()

    fun findBinary(kind: String, buildType: String) =
        findBinary(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))

    fun getBinary(kind: String, buildType: String) =
        getBinary(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))

    // Naming
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    fun linkTaskName(kind: NativeOutputKind, buildType: NativeBuildType): String =
        lowerCamelCaseName(
            "link",
            KotlinNativeBinaryContainer.generateBinaryName(compilationName, buildType, kind.taskNameClassifier),
            target.targetName
        )

    fun linkTaskName(kind: String, buildType: String) =
        linkTaskName(NativeOutputKind.valueOf(kind.toUpperCase()), NativeBuildType.valueOf(buildType.toUpperCase()))

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