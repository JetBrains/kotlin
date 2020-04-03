/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.concurrent.Callable

abstract class AbstractKotlinNativeCompilation(
    target: KotlinTarget,
    val konanTarget: KonanTarget,
    compilationName: String
) : AbstractKotlinCompilation<KotlinCommonOptions>(target, compilationName) {

    override val kotlinOptions: KotlinCommonOptions
        get() = compileKotlinTask.kotlinOptions

    override val compileKotlinTask: KotlinNativeCompile
        get() = super.compileKotlinTask as KotlinNativeCompile

    // A collection containing all source sets used by this compilation
    // (taking into account dependencies between source sets). Used by both compilation
    // and linking tasks. Unlike kotlinSourceSets, includes dependency source sets.
    // TODO: Move into the compilation task when the linking task does klib linking instead of compilation.
    internal val allSources: MutableSet<SourceDirectorySet> = mutableSetOf()

    // TODO: Move into the compilation task when the linking task does klib linking instead of compilation.
    internal val commonSources: ConfigurableFileCollection = target.project.files()

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
        allSources.add(sourceSet.kotlin)
        commonSources.from(target.project.files(Callable { if (addAsCommonSources.value) sourceSet.kotlin else emptyList<Any>() }))
    }

    // Endorsed library controller.
    var enableEndorsedLibs: Boolean = false
}

class KotlinNativeCompilation(
    override val target: KotlinNativeTarget,
    konanTarget: KonanTarget,
    name: String
) : AbstractKotlinNativeCompilation(target, konanTarget, name),
    KotlinCompilationWithResources<KotlinCommonOptions> {

    private val project: Project
        get() = target.project

    // Interop DSL.
    val cinterops = project.container(DefaultCInteropSettings::class.java) { cinteropName ->
        DefaultCInteropSettings(project, cinteropName, this)
    }

    fun cinterops(action: Closure<Unit>) = cinterops(ConfigureUtil.configureUsing(action))
    fun cinterops(action: Action<NamedDomainObjectContainer<DefaultCInteropSettings>>) = action.execute(cinterops)

    // Naming
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

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

    override val kotlinOptions: KotlinCommonOptions
        get() = compileKotlinTask.kotlinOptions
}

class KotlinSharedNativeCompilation(override val target: KotlinMetadataTarget, name: String) :
    AbstractKotlinNativeCompilation(target, HostManager.host, name),
    KotlinMetadataCompilation<KotlinCommonOptions> {

    override val friendArtifacts: FileCollection
        get() = super.friendArtifacts.plus(run {
            val project = target.project
            val friendSourceSets = getVisibleSourceSetsFromAssociateCompilations(project, defaultSourceSet).toMutableSet().apply {
                // TODO: implement proper dependsOn/refines compiler args for Kotlin/Native and pass the dependsOn klibs separately;
                //       But for now, those dependencies don't have any special semantics, so passing all them as friends works, too
                addAll(defaultSourceSet.getSourceSetHierarchy())
                remove(defaultSourceSet)
            }
            project.files(friendSourceSets.mapNotNull { target.compilations.findByName(it.name)?.output?.classesDirs })
        })
}