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
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.targets.metadata.getMetadataCompilationForSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.concurrent.Callable

abstract class AbstractKotlinNativeCompilation(
    target: KotlinTarget,
    val konanTarget: KonanTarget,
    compilationName: String
) : AbstractKotlinCompilation<KotlinCommonOptions>(target, compilationName) {

    override val kotlinOptions: KotlinCommonOptions = NativeCompileOptions { defaultSourceSet.languageSettings }

    private class NativeCompileOptions(languageSettingsProvider: () -> LanguageSettingsBuilder) : KotlinCommonOptions {
        private val languageSettings: LanguageSettingsBuilder by lazy(languageSettingsProvider)

        override var apiVersion: String?
            get() = languageSettings.apiVersion
            set(value) { languageSettings.apiVersion = value }

        override var languageVersion: String?
            get() = languageSettings.languageVersion
            set(value) { languageSettings.languageVersion = value }

        override var allWarningsAsErrors: Boolean = false
        override var suppressWarnings: Boolean = false
        override var verbose: Boolean = false

        override var freeCompilerArgs: List<String> = listOf()
    }

    override val compileKotlinTask: KotlinNativeCompile
        get() = super.compileKotlinTask as KotlinNativeCompile

    // A collection containing all source sets used by this compilation
    @Suppress("UNCHECKED_CAST")
    override val compileKotlinTaskProvider: TaskProvider<out KotlinNativeCompile>
        get() = super.compileKotlinTaskProvider as TaskProvider<out KotlinNativeCompile>

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) =
        compileKotlinTaskProvider.configure { task ->
            task.source(sourceSet.kotlin)
            task.commonSources.from(target.project.files(Callable { if (addAsCommonSources.value) sourceSet.kotlin else emptyList<Any>() }))
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
}

class KotlinSharedNativeCompilation(override val target: KotlinMetadataTarget, val konanTargets: List<KonanTarget>, name: String) :
    AbstractKotlinNativeCompilation(
        target,
        // TODO: this will end up as '-target' argument passed to K2Native, which is wrong.
        // Rewrite this when we'll compile native-shared source-sets against commonized platform libs
        // We find any konan target that is enabled on the current host in order to pass the checks that avoid compiling the code otherwise.
        konanTargets.find { it.enabledOnCurrentHost } ?: konanTargets.first(),
        name
    ),
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
            project.files(friendSourceSets.mapNotNull { project.getMetadataCompilationForSourceSet(it)?.output?.classesDirs })
        })
}