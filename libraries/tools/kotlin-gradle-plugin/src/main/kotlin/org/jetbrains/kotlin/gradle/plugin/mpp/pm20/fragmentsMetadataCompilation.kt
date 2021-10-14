/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCommonSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.tasks.CompileAllTask
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.project.model.KotlinModuleFragment
import java.util.concurrent.Callable

internal fun configureMetadataResolutionAndBuild(module: KotlinGradleModule) {
    val project = module.project

    val metadataCompilationRegistry = MetadataCompilationRegistry()
    project.pm20Extension.metadataCompilationRegistryByModuleId[module.moduleIdentifier] =
        metadataCompilationRegistry

    configureMetadataCompilationsAndCreateRegistry(module, metadataCompilationRegistry)

    GlobalProjectStructureMetadataStorage.registerProjectStructureMetadata(project) {
        buildProjectStructureMetadata(module)
    }
}

private fun configureMetadataCompilationsAndCreateRegistry(
    module: KotlinGradleModule,
    metadataCompilationRegistry: MetadataCompilationRegistry
) {
    val project = module.project
    val metadataResolutionByFragment = mutableMapOf<KotlinGradleFragment, FragmentGranularMetadataResolver>()
    module.fragments.all { fragment ->
        val transformation = FragmentGranularMetadataResolver(fragment, lazy {
            fragment.refinesClosure.minus(fragment).map {
                metadataResolutionByFragment.getValue(it)
            }
        })
        metadataResolutionByFragment[fragment] = transformation
        createExtractMetadataTask(project, fragment, transformation)
    }
    module.fragments.all { fragment ->
        createCommonMetadataCompilation(fragment, metadataCompilationRegistry)
        createNativeMetadataCompilation(fragment, metadataCompilationRegistry)
    }
    metadataCompilationRegistry.withAll { compilation ->
        project.tasks.matching { it.name == compilation.compileKotlinTaskName }.configureEach { task ->
            task.onlyIf { compilation.isActive }
        }
    }
}

private fun createCommonMetadataCompilation(
    fragment: KotlinGradleFragment,
    metadataCompilationRegistry: MetadataCompilationRegistry
) {
    val module = fragment.containingModule
    val project = module.project

    val metadataCompilationData =
        KotlinCommonFragmentMetadataCompilationDataImpl(
            project,
            fragment,
            module,
            module.taskProvider(CompileAllTask),
            metadataCompilationRegistry,
            lazy { resolvedMetadataProviders(fragment) }
        )
    MetadataCompilationTasksConfigurator(project).createKotlinCommonCompilationTask(fragment, metadataCompilationData)
    metadataCompilationRegistry.registerCommon(fragment, metadataCompilationData)
}

private fun createNativeMetadataCompilation(
    fragment: KotlinGradleFragment,
    metadataCompilationRegistry: MetadataCompilationRegistry
) {
    val module = fragment.containingModule
    val project = module.project

    val metadataCompilationData =
        KotlinNativeFragmentMetadataCompilationDataImpl(
            project,
            fragment,
            module,
            module.taskProvider(CompileAllTask),
            metadataCompilationRegistry,
            lazy { resolvedMetadataProviders(fragment) }
        )
    MetadataCompilationTasksConfigurator(project).createKotlinNativeMetadataCompilationTask(fragment, metadataCompilationData)
    metadataCompilationRegistry.registerNative(fragment, metadataCompilationData)
}

private class MetadataCompilationTasksConfigurator(project: Project) : KotlinCompilationTaskConfigurator(project) {
    fun createKotlinCommonCompilationTask(
        fragment: KotlinGradleFragment,
        compilationData: KotlinCommonFragmentMetadataCompilationData
    ) {
        KotlinCommonSourceSetProcessor(
            compilationData,
            KotlinTasksProvider()
        ).run()
        val allSources = getSourcesForFragmentCompilation(fragment)
        val commonSources = getCommonSourcesForFragmentCompilation(fragment)

        addSourcesToKotlinCompileTask(project, compilationData.compileKotlinTaskName, emptyList()) { allSources }
        addCommonSourcesToKotlinCompileTask(project, compilationData.compileKotlinTaskName, emptyList()) { commonSources }

        project.tasks.named(compilationData.compileKotlinTaskName, AbstractKotlinCompile::class.java).configure {
            it.kotlinPluginData = project.compilerPluginProviderForMetadata(fragment, compilationData)
        }
    }

    fun createKotlinNativeMetadataCompilationTask(
        fragment: KotlinGradleFragment,
        compilationData: KotlinNativeFragmentMetadataCompilationData
    ): TaskProvider<KotlinNativeCompile> = createKotlinNativeCompilationTask(fragment, compilationData) {
        kotlinPluginData = project.compilerPluginProviderForNativeMetadata(fragment, compilationData)
    }

    override fun getSourcesForFragmentCompilation(fragment: KotlinGradleFragment): MultipleSourceRootsProvider {
        return project.provider { listOf(fragmentSourcesProvider.getFragmentOwnSources(fragment)) }
    }

    override fun getCommonSourcesForFragmentCompilation(fragment: KotlinGradleFragment): MultipleSourceRootsProvider {
        return project.provider { listOf(fragmentSourcesProvider.getFragmentOwnSources(fragment)) }
    }
}

private fun resolvedMetadataProviders(fragment: KotlinGradleFragment) =
    fragment.refinesClosure.map {
        FragmentResolvedMetadataProvider(
            fragment.project.tasks.withType<TransformKotlinGranularMetadataForFragment>().named(transformFragmentMetadataTaskName(it))
        )
    }

private fun createExtractMetadataTask(
    project: Project,
    fragment: KotlinGradleFragment,
    transformation: FragmentGranularMetadataResolver
) {
    project.tasks.register(
        transformFragmentMetadataTaskName(fragment),
        TransformKotlinGranularMetadataForFragment::class.java,
        fragment,
        transformation
    ).configure { task ->
        task.dependsOn(Callable {
            fragment.refinesClosure.mapNotNull { refined ->
                if (refined !== fragment)
                    project.tasks.named(transformFragmentMetadataTaskName(refined))
                else null
            }
        })
    }
}

// FIXME: use this function once more than one platform is supported
private fun disableMetadataCompilationIfNotYetSupported(
    metadataCompilationData: AbstractKotlinFragmentMetadataCompilationData<*>
) {
    val fragment = metadataCompilationData.fragment
    val platforms = fragment.containingModule.variantsContainingFragment(fragment).map { it.platformType }.toSet()
    if (platforms != setOf(KotlinPlatformType.native) && platforms.size == 1
        || platforms == setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)
    ) {
        fragment.containingModule.project.tasks.named(metadataCompilationData.compileKotlinTaskName).configure {
            it.enabled = false
        }
    }
}

private fun transformFragmentMetadataTaskName(fragment: KotlinModuleFragment) =
    lowerCamelCaseName("resolve", fragment.disambiguateName("Metadata"))
