/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinNativeCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinNativeFragmentMetadataCompilationData
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject

abstract class AbstractKotlinNativeCompilation(
    override val konanTarget: KonanTarget,
    compilationDetails: CompilationDetails<KotlinCommonOptions>
) : AbstractKotlinCompilation<KotlinCommonOptions>(
    compilationDetails
),
    KotlinNativeCompilationData<KotlinCommonOptions> {

    @Suppress("DEPRECATION")
    @Deprecated("Accessing task instance directly is deprecated", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTask: KotlinNativeCompile
        get() = super.compileKotlinTask as KotlinNativeCompile

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    @Deprecated("Replaced with compileTaskProvider", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTaskProvider: TaskProvider<out KotlinNativeCompile>
        get() = super.compileKotlinTaskProvider as TaskProvider<out KotlinNativeCompile>

    @Suppress("UNCHECKED_CAST")
    override val compileTaskProvider: TaskProvider<KotlinNativeCompile>
        get() = super.compileTaskProvider as TaskProvider<KotlinNativeCompile>

    internal val useGenericPluginArtifact: Boolean
        get() = project.nativeUseEmbeddableCompilerJar

    // Endorsed library controller.
    @Deprecated("Please declare explicit dependency on kotlinx-cli. This option is scheduled to be removed in 1.9.0")
    override var enableEndorsedLibs: Boolean = false
}

internal val Project.nativeUseEmbeddableCompilerJar: Boolean
    get() = PropertiesProvider(this).nativeUseEmbeddableCompilerJar

internal fun addSourcesToKotlinNativeCompileTask(
    project: Project,
    taskName: String,
    sourceFiles: () -> Iterable<File>,
    addAsCommonSources: Lazy<Boolean>
) {
    project.tasks.withType(KotlinNativeCompile::class.java).matching { it.name == taskName }.configureEach { task ->
        task.setSource(sourceFiles)
        task.commonSources.from(project.files(Callable { if (addAsCommonSources.value) sourceFiles() else emptyList() }))
    }

}

abstract class KotlinNativeCompilation @Inject constructor(
    konanTarget: KonanTarget,
    details: CompilationDetails<KotlinCommonOptions>
) : AbstractKotlinNativeCompilation(konanTarget, details),
    KotlinCompilationWithResources<KotlinCommonOptions> {

    override val target: KotlinNativeTarget
        get() = super.target as KotlinNativeTarget

    override val compilerOptions: NativeCompilerOptions
        get() = super.compilerOptions as NativeCompilerOptions

    // Interop DSL.
    val cinterops = project.container(DefaultCInteropSettings::class.java) { cinteropName ->
        project.objects.newInstance(DefaultCInteropSettings::class.java, cinteropName, this)
    }

    fun cinterops(action: Action<NamedDomainObjectContainer<DefaultCInteropSettings>>) = action.execute(cinterops)

    // Naming
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    val binariesTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationPurpose, "binaries")
}

abstract class KotlinSharedNativeCompilation @Inject constructor(
    val konanTargets: List<KonanTarget>,
    compilationDetails: CompilationDetails<KotlinCommonOptions>
) : KotlinNativeFragmentMetadataCompilationData,
    AbstractKotlinNativeCompilation(
        // TODO: this will end up as '-target' argument passed to K2Native, which is wrong.
        // Rewrite this when we'll compile native-shared source-sets against commonized platform libs
        // We find any konan target that is enabled on the current host in order to pass the checks that avoid compiling the code otherwise.
        konanTargets.find { it.enabledOnCurrentHost } ?: konanTargets.first(),
        compilationDetails
    ),
    KotlinMetadataCompilation<KotlinCommonOptions> {

    override fun getName() =
        if (compilationDetails is MetadataMappedCompilationDetails) defaultSourceSetName else super.compilationPurpose

    override val target: KotlinMetadataTarget get() = super.target as KotlinMetadataTarget

    override val isActive: Boolean
        get() = true // old plugin only creates necessary compilations
}
