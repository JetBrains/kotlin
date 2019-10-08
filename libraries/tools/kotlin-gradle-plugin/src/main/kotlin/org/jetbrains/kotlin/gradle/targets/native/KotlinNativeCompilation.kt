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
import org.gradle.api.file.SourceDirectorySet
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.concurrent.Callable

class KotlinNativeCompilation(
    override val target: KotlinNativeTarget,
    val konanTarget: KonanTarget,
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
    internal val commonSources: ConfigurableFileCollection = project.files()

    @Deprecated("Use associateWith(...) to add a friend compilation and associateWith to get all of them.")
    var friendCompilationName: String? = null
        set(value) {
            SingleWarningPerBuild.show(
                project,
                "Property `friendCompilationName` of `KotlinNativeCompilation` has been deprecated and will be removed. " +
                        "Use `associateWith(...)` instead."
            )
            field = value
        }

    internal val friendCompilations: List<KotlinNativeCompilation>
        get() = mutableListOf<KotlinNativeCompilation>().apply {
            @Suppress("DEPRECATION")
            friendCompilationName?.let {
                add(target.compilations.getByName(it))
            }
            addAll(associateWithTransitiveClosure.filterIsInstance<KotlinNativeCompilation>())
        }

    // Native-specific DSL.
    private fun showDeprecationWarning() = SingleWarningPerBuild.show(
        project,
        "The compilation.extraOpts method used in this build is deprecated. Use compilation.kotlinOptions.freeCompilerArgs instead."
    )

    internal var extraOptsNoWarn: MutableList<String> = mutableListOf()

    @Deprecated("Use kotlinOptions.freeCompilerArgs instead", ReplaceWith("kotlinOptions.freeCompilerArgs"))
    var extraOpts: MutableList<String>
        get() {
            showDeprecationWarning()
            return extraOptsNoWarn
        }
        set(value) {
            showDeprecationWarning()
            extraOptsNoWarn = value
        }

    @Deprecated("Use kotlinOptions.freeCompilerArgs instead", ReplaceWith("kotlinOptions.freeCompilerArgs += values as Array<String>"))
    @Suppress("Deprecation")
    fun extraOpts(vararg values: Any) = extraOpts(values.toList())

    @Deprecated("Use kotlinOptions.freeCompilerArgs instead", ReplaceWith("kotlinOptions.freeCompilerArgs += values as List<String>"))
    @Suppress("Deprecation")
    fun extraOpts(values: List<Any>) {
        extraOpts.addAll(values.map { it.toString() })
    }

    // Interop DSL.
    val cinterops = project.container(DefaultCInteropSettings::class.java) { cinteropName ->
        DefaultCInteropSettings(project, cinteropName, this)
    }

    // Endorsed library controller.
    var enableEndorsedLibs = false

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

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
        allSources.add(sourceSet.kotlin)
        commonSources.from(project.files(Callable { if (addAsCommonSources.value) sourceSet.kotlin else emptyList<Any>() }))
    }
}