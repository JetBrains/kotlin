/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.metadata.createGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.targets.metadata.filesWithUnpackedArchives
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.project.model.KpmFragment
import java.util.concurrent.Callable

internal fun setupFragmentsMetadataForKpmModules(project: Project) {
    project.pm20Extension.modules.all { module ->
        configureMetadataResolutionAndBuild(module)
        configureMetadataExposure(module)
    }
}

private fun configureMetadataResolutionAndBuild(module: GradleKpmModule) {
    val project = module.project
    createResolvableMetadataConfigurationForModule(module)

    val metadataCompilationRegistry = MetadataCompilationRegistry()
    project.metadataCompilationRegistryByModuleId[module.moduleIdentifier] =
        metadataCompilationRegistry

    configureMetadataCompilationsAndCreateRegistry(module, metadataCompilationRegistry)

    configureMetadataJarTask(module, metadataCompilationRegistry)
    generateAndExportProjectStructureMetadata(module)
}

private fun configureMetadataExposure(module: GradleKpmModule) {
    val project = module.project
    project.configurations.create(metadataElementsConfigurationName(module)).apply {
        isCanBeConsumed = false
        module.ifMadePublic {
            isCanBeConsumed = true
        }
        isCanBeResolved = false
        project.artifacts.add(name, project.tasks.named(metadataJarName(module)))
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
        module.fragments.all { fragment ->
            // FIXME: native api-implementation
            project.addExtendsFromRelation(name, fragment.apiConfigurationName)
        }
        setModuleCapability(this, module)
    }

    val sourcesArtifactAppendix = dashSeparatedName(module.moduleClassifier, "all", "sources")
    val sourcesArtifact = sourcesJarTaskNamed(
        module.disambiguateName("allSourcesJar"),
        module.name,
        project,
        project.future {
            GradleKpmFragmentSourcesProvider().getAllFragmentSourcesAsMap(module).entries.associate { it.key.fragmentName to it.value.get() }
        },
        sourcesArtifactAppendix,
        "module",
    )
    DocumentationVariantConfigurator().createSourcesElementsConfiguration(
        project, sourceElementsConfigurationName(module),
        sourcesArtifact.get(), "sources", ComputedCapability.fromModuleOrNull(module)
    )
}

fun metadataElementsConfigurationName(module: GradleKpmModule) =
    module.disambiguateName("metadataElements")

fun sourceElementsConfigurationName(module: GradleKpmModule) =
    module.disambiguateName("sourceElements")

private fun generateAndExportProjectStructureMetadata(
    module: GradleKpmModule
) {
    val project = module.project
    val projectStructureMetadata = project.createGenerateProjectStructureMetadataTask(module)
    project.tasks.withType<Jar>().named(metadataJarName(module)).configure { jar ->
        jar.from(projectStructureMetadata.map { it.resultFile }) { spec ->
            spec.into("META-INF")
                .rename { MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME }
        }
    }
    GlobalProjectStructureMetadataStorage.registerProjectStructureMetadata(project) {
        buildProjectStructureMetadata(module)
    }
}

private fun createResolvableMetadataConfigurationForModule(module: GradleKpmModule) {
    val project = module.project
    project.configurations.create(module.resolvableMetadataConfigurationName).apply {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        module.fragments.all { fragment ->
            project.addExtendsFromRelation(name, fragment.apiConfigurationName)
            project.addExtendsFromRelation(name, fragment.implementationConfigurationName)
        }
    }
}

private fun configureMetadataCompilationsAndCreateRegistry(
    module: GradleKpmModule,
    metadataCompilationRegistry: MetadataCompilationRegistry
) {
    val project = module.project
    val metadataResolverFactory = GradleKpmFragmentGranularMetadataResolverFactory()
    module.fragments.all { fragment ->
        val metadataResolver = metadataResolverFactory.getOrCreate(fragment)
        createExtractMetadataTask(project, fragment, metadataResolver)
    }
    val compileAllTask = project.registerTask<DefaultTask>(lowerCamelCaseName(module.moduleClassifier, "metadataClasses"))
    module.fragments.all { fragment ->
        createCommonMetadataCompilation(fragment, compileAllTask, metadataCompilationRegistry)
        createNativeMetadataCompilation(fragment, compileAllTask, metadataCompilationRegistry)
    }
    metadataCompilationRegistry.withAll { compilation ->
        project.tasks.matching { it.name == compilation.compileKotlinTaskName }.configureEach { task ->
            task.onlyIf { compilation.isActive }
        }
    }
}

private fun configureMetadataJarTask(
    module: GradleKpmModule,
    registry: MetadataCompilationRegistry
) {
    val project = module.project
    val allMetadataJar = project.registerTask<Jar>(metadataJarName(module)) { task ->
        if (module.moduleClassifier != null) {
            task.archiveClassifier.set(module.moduleClassifier)
        }
        task.archiveAppendix.set("metadata")
        task.from()
    }
    module.fragments.all { fragment ->
        allMetadataJar.configure { jar ->
            val metadataOutput = project.filesProvider {
                val compilationData = registry.getForFragmentOrNull(fragment)
                    .takeIf { !fragment.isNativeHostSpecific() }
                    ?: return@filesProvider emptyList<Any>()
                project.filesWithUnpackedArchives(compilationData.output.allOutputs, setOf(KLIB_FILE_EXTENSION))
            }
            jar.from(metadataOutput) { spec ->
                spec.into(fragment.fragmentName)
            }
        }
    }
}

internal fun metadataJarName(module: GradleKpmModule) =
    lowerCamelCaseName(module.moduleClassifier, KotlinMetadataTargetConfigurator.ALL_METADATA_JAR_NAME)

private fun createCommonMetadataCompilation(
    fragment: GradleKpmFragment,
    compileAllTask: TaskProvider<DefaultTask>,
    metadataCompilationRegistry: MetadataCompilationRegistry
) {
    val module = fragment.containingModule
    val project = module.project

    val metadataCompilationData =
        GradleKpmCommonFragmentMetadataCompilationDataImpl(
            project,
            fragment,
            module,
            compileAllTask,
            metadataCompilationRegistry,
            lazy { resolvedMetadataProviders(fragment) }
        )
    GradleKpmMetadataCompilationTasksConfigurator(project).createKotlinCommonCompilationTask(fragment, metadataCompilationData)
    metadataCompilationRegistry.registerCommon(fragment, metadataCompilationData)
}

private fun createNativeMetadataCompilation(
    fragment: GradleKpmFragment,
    compileAllTask: TaskProvider<DefaultTask>,
    metadataCompilationRegistry: MetadataCompilationRegistry
) {
    val module = fragment.containingModule
    val project = module.project

    val metadataCompilationData =
        GradleKpmNativeFragmentMetadataCompilationDataImpl(
            project,
            fragment,
            module,
            compileAllTask,
            metadataCompilationRegistry,
            lazy { resolvedMetadataProviders(fragment) }
        )
    GradleKpmMetadataCompilationTasksConfigurator(project).createKotlinNativeMetadataCompilationTask(fragment, metadataCompilationData)
    metadataCompilationRegistry.registerNative(fragment, metadataCompilationData)
}

private class GradleKpmMetadataCompilationTasksConfigurator(project: Project) : GradleKpmCompilationTaskConfigurator(project) {
    fun createKotlinCommonCompilationTask(
        fragment: GradleKpmFragment,
        compilationData: GradleKpmCommonFragmentMetadataCompilationData
    ) {
        KotlinCommonSourceSetProcessor(KotlinCompilationInfo.KPM(compilationData), KotlinTasksProvider()).run()
        val allSources = getSourcesForFragmentCompilation(fragment)
        val commonSources = getCommonSourcesForFragmentCompilation(fragment)

        addSourcesToKotlinCompileTask(project, compilationData.compileKotlinTaskName, emptyList()) { allSources }
        addCommonSourcesToKotlinCompileTask(project, compilationData.compileKotlinTaskName, emptyList()) { commonSources }

        project.tasks.named(compilationData.compileKotlinTaskName, AbstractKotlinCompile::class.java).configure {
            it.kotlinPluginData = project.compilerPluginProviderForMetadata(fragment, compilationData)
        }
    }

    fun createKotlinNativeMetadataCompilationTask(
        fragment: GradleKpmFragment,
        compilationData: GradleKpmNativeFragmentMetadataCompilationData
    ): TaskProvider<KotlinNativeCompile> = createKotlinNativeCompilationTask(fragment, compilationData) {
        kotlinPluginData = project.compilerPluginProviderForNativeMetadata(fragment, compilationData)
    }

    override fun getSourcesForFragmentCompilation(fragment: GradleKpmFragment): MultipleSourceRootsProvider {
        return project.provider { listOf(fragmentSourcesProvider.getFragmentOwnSources(fragment)) }
    }

    override fun getCommonSourcesForFragmentCompilation(fragment: GradleKpmFragment): MultipleSourceRootsProvider {
        return project.provider { listOf(fragmentSourcesProvider.getFragmentOwnSources(fragment)) }
    }
}

private fun resolvedMetadataProviders(fragment: GradleKpmFragment) =
    fragment.withRefinesClosure.map {
        FragmentResolvedMetadataProvider(
            fragment.project.tasks.withType<GradleKpmMetadataDependencyTransformationTask>().named(transformFragmentMetadataTaskName(it))
        )
    }

private fun createExtractMetadataTask(
    project: Project,
    fragment: GradleKpmFragment,
    transformation: GradleKpmFragmentGranularMetadataResolver
) {
    project.tasks.register(
        transformFragmentMetadataTaskName(fragment),
        GradleKpmMetadataDependencyTransformationTask::class.java,
        fragment,
        transformation
    ).configure { task ->
        task.dependsOn(Callable {
            fragment.withRefinesClosure.mapNotNull { refined ->
                if (refined !== fragment)
                    project.tasks.named(transformFragmentMetadataTaskName(refined))
                else null
            }
        })
    }
}

// FIXME: use this function once more than one platform is supported
private fun disableMetadataCompilationIfNotYetSupported(
    metadataCompilationData: GradleKpmAbstractFragmentMetadataCompilationData<*>
) {
    val fragment = metadataCompilationData.fragment
    val platforms = fragment.containingVariants.map { it.platformType }.toSet()
    if (platforms != setOf(KotlinPlatformType.native) && platforms.size == 1
        || platforms == setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)
    ) {
        fragment.containingModule.project.tasks.named(metadataCompilationData.compileKotlinTaskName).configure {
            it.enabled = false
        }
    }
}

private fun transformFragmentMetadataTaskName(fragment: KpmFragment) =
    lowerCamelCaseName("resolve", fragment.disambiguateName("Metadata"))
