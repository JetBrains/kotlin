/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.android.internal

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.sdklib.IAndroidTarget
import com.android.sdklib.repository.AndroidSdkHandler
import com.android.sdklib.repository.LoggerProgressIndicatorWrapper
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.artifacts.result.ComponentResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.AttributesSchema
import org.gradle.api.capabilities.Capability
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal
import org.gradle.api.internal.artifacts.dependencies.ProjectDependencyInternal
import org.gradle.api.internal.attributes.AttributeContainerInternal
import org.gradle.api.internal.attributes.AttributesSchemaInternal
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.internal.component.model.AttributeConfigurationSelector
import org.jetbrains.kotlin.gradle.plugin.forEachVariant
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import java.nio.file.Path

data class AndroidDependency(
    val name: String? = null,
    val jar: File? = null,
    val source: File? = null,
    val group: String? = null,
    val version: String? = null,
    val collection: Set<File>? = null
)

@Suppress("unused")
object AndroidDependencyResolver {
    private fun getAndroidSdkJar(project: Project, versions: List<String>): AndroidDependency? {
        val androidExtension = project.extensions.findByName("android") as BaseExtension? ?: return null
        val useNioPath = versions[0].toInt() >= 7

        val sdkHandler = if (useNioPath) {
            val androidLocationBuildService = getAndroidLocationBuildService(project) ?: return null
            val androidLocationsProvider = getClassOrNull("com.android.prefs.AndroidLocationsProvider") ?: return null
            val getInstance =
                AndroidSdkHandler::class.java.getMethodOrNull("getInstance", androidLocationsProvider, Path::class.java) ?: return null
            getInstance(null, androidLocationBuildService, androidExtension.sdkDirectory.toPath()) as AndroidSdkHandler
        } else {
            AndroidSdkHandler.getInstance(androidExtension.sdkDirectory)
        }
        val logger = LoggerProgressIndicatorWrapper(LoggerWrapper(project.logger))
        val androidTarget =
            sdkHandler.getAndroidTargetManager(logger).getTargetFromHashString(androidExtension.compileSdkVersion, logger) ?: return null

        val jar: File
        val sources: File
        if (useNioPath) {
            val getPath = IAndroidTarget::class.java.getMethodOrNull("getPath", Int::class.java) ?: return null
            jar = (getPath(androidTarget, IAndroidTarget.ANDROID_JAR) as Path).toFile()
            sources = (getPath(androidTarget, IAndroidTarget.SOURCES) as Path).toFile()
        } else {
            jar = File(androidTarget.getPath(IAndroidTarget.ANDROID_JAR))
            sources = File(androidTarget.getPath(IAndroidTarget.SOURCES))
        }
        return AndroidDependency(
            androidTarget.fullName,
            jar,
            sources
        )
    }

    private fun getAndroidLocationBuildService(project: Project): BuildService<BuildServiceParameters.None>? =
        getClassOrNull("com.android.build.gradle.internal.services.BuildServicesKt")?.let { buildServices ->
            getClassOrNull("com.android.build.gradle.internal.services.AndroidLocationsBuildService")?.let { androidLocationsService ->
                val getBuildService =
                    buildServices.getMethodOrNull("getBuildService", BuildServiceRegistry::class.java, androidLocationsService.javaClass)
                @Suppress("UNCHECKED_CAST")
                (getBuildService?.invoke(
                    null,
                    project.gradle.sharedServices,
                    androidLocationsService
                ) as Provider<BuildService<BuildServiceParameters.None>>?)?.get()
            }
        }

    private fun getClassOrNull(s: String) =
        try {
            Class.forName(s)
        } catch (e: Exception) {
            null
        }

    private fun Class<*>.getMethodOrNull(name: String, vararg parameterTypes: Class<*>) =
        try {
            getMethod(name, *parameterTypes)
        } catch (e: Exception) {
            null
        }

    private fun getAndroidPluginVersions(): List<String>? {
        val version = getClassOrNull("com.android.Version")?.let {
            try {
                it.getField("ANDROID_GRADLE_PLUGIN_VERSION").get(null) as String
            } catch (e: Exception) {
                return null
            }
        } ?: return null
        return version.split('.')
    }

    private fun isAndroidPluginCompatible(versions: List<String>): Boolean {
        return versions[0].toInt() >= 4 || versions[0].toInt() >= 3 && versions[1].toInt() >= 6
    }

    private data class SourceSetConfigs(val implConfig: Configuration, val compileConfigs: MutableList<Configuration> = ArrayList())

    fun getAndroidSourceSetDependencies(project: Project): Map<String, List<AndroidDependency>?> {
        val androidPluginVersions = getAndroidPluginVersions() ?: return emptyMap()
        if (!isAndroidPluginCompatible(androidPluginVersions)) return emptyMap()

        val androidSdkJar = getAndroidSdkJar(project, androidPluginVersions) ?: return emptyMap()
        val sourceSet2Impl = HashMap<String, SourceSetConfigs>()
        val allImplConfigs = HashSet<Configuration>()

        project.forEachVariant { variant ->
            val compileConfig = variant.compileConfiguration
            variant.sourceSets.filterIsInstance(AndroidSourceSet::class.java).map {
                val implConfig = project.configurations.getByName(it.implementationConfigurationName)
                allImplConfigs.add(implConfig)
                val sourceSetConfigs =
                    sourceSet2Impl.computeIfAbsent(lowerCamelCaseName("android", it.name)) { SourceSetConfigs(implConfig) }
                // The same sourceset can be included into multiple variants (e.g. androidMain)
                sourceSetConfigs.compileConfigs.add(compileConfig)
            }
        }

        val attributesSchema = project.dependencies.attributesSchema

        return sourceSet2Impl.mapValues { (sourceSetName, sourceSetConfigs) ->
            val dependencies = findDependencies(allImplConfigs, sourceSetConfigs, attributesSchema)

            val selfResolved = dependencies.filterIsInstance<SelfResolvingDependency>().map { dependency ->
                val collection = dependency.resolve().takeIf(Collection<File?>::isNotEmpty)
                AndroidDependency(dependency.name, group = dependency.group, collection = collection)
            }
            val resolvedExternal =
                collectDependencies(dependencies.filterIsInstance<ExternalModuleDependency>(), sourceSetConfigs.compileConfigs)

            val result = (selfResolved + resolvedExternal + androidSdkJar).toMutableList()

            if (sourceSetName == "androidMain") {
                // this is a terrible hack, but looks like the only way, other than proper support via light-classes
                val task = project.tasks.findByName("processDebugResources")
                if (task != null) {
                    getClassOrNull("com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask")?.let { linkAppClass ->
                        @Suppress("UNCHECKED_CAST")
                        val rClassOutputJar =
                            linkAppClass.getMethodOrNull("getRClassOutputJar")?.invoke(task) as Provider<FileSystemLocation>?
                        rClassOutputJar?.orNull?.asFile?.let { result += AndroidDependency("R.jar", it) }
                    }
                }
            }

            result
        }.toMap()
    }

    @Suppress("UnstableApiUsage")
    private fun collectDependencies(
        dependencies: List<ExternalModuleDependency>,
        compileClasspathConfigs: List<Configuration>
    ): Set<AndroidDependency> {
        val processedJarArtifactType = try {
            AndroidArtifacts.ArtifactType.valueOf("PROCESSED_JAR")
        } catch (e: IllegalArgumentException) {
            AndroidArtifacts.ArtifactType.JAR
        }
        val viewConfig: (ArtifactView.ViewConfiguration) -> Unit = { config ->
            config.attributes { it.attribute(AndroidArtifacts.ARTIFACT_TYPE, processedJarArtifactType.type) }
            config.isLenient = true
        }

        val allResults = LinkedHashSet<ResolvedArtifactResult>()
        val allComponents = LinkedHashSet<ResolvedComponentResult>()

        compileClasspathConfigs.forEach { config ->
            // If the current sourceset's implementation configuration is included into several compile classpath configuration's,
            // we'll take only the results which are the same across all of them. The dependencies which resolve differently will appear
            // in the more specific sourcesets.
            with(config.incoming) {
                allResults.addOrRetainAll(artifactView(viewConfig).artifacts.artifacts, ArtifactResult::getId)
                allComponents.addOrRetainAll(resolutionResult.allComponents, ComponentResult::getId)
            }
        }

        val resolvedArtifacts = allResults.mapNotNull {
            val componentIdentifier = it.id.componentIdentifier as? ModuleComponentIdentifier ?: return@mapNotNull null
            val id = componentIdentifier.moduleIdentifier ?: return@mapNotNull null
            id to AndroidDependency(id.name, it.file, null, id.group, componentIdentifier.version)
        }.toMap()

        val resolutionResults = allComponents.mapNotNull {
            val id = it.moduleVersion?.module ?: return@mapNotNull null
            id to it
        }.toMap()

        return dependencies.flatMapTo(HashSet()) { dependency ->
            val deps = HashSet<ModuleIdentifier>().also { doCollectDependencies(listOf(dependency.module), resolutionResults, it) }
            deps.mapNotNull { resolvedArtifacts[it] }
        }
    }

    @Suppress("UnstableApiUsage")
    private tailrec fun doCollectDependencies(
        modules: List<ModuleIdentifier>,
        resolutionResults: Map<ModuleIdentifier, ResolvedComponentResult>,
        result: MutableSet<ModuleIdentifier>
    ) {
        if (modules.isEmpty()) return
        val newModules = modules.filter { result.add(it) }.mapNotNull { resolutionResults[it] }.flatMap { it.dependencies }
            .mapNotNull { (it.requested as? ModuleComponentSelector)?.moduleIdentifier }
        doCollectDependencies(newModules, resolutionResults, result)
    }

    private fun findDependencies(
        implConfigs: Set<Configuration>,
        sourceSetConfigs: SourceSetConfigs,
        attributesSchema: AttributesSchema
    ): Set<Dependency> {
        // Using the attributes of the first configuration. If there are multiple configurations, then differences in resolve would be
        // discarded later anyway, so they won't make any difference
        val attributes = sourceSetConfigs.compileConfigs.first().attributes

        return HashSet<Dependency>().also {
            doFindDependencies(implConfigs, listOf(sourceSetConfigs.implConfig), attributes, attributesSchema, it)
        }
    }

    private tailrec fun doFindDependencies(
        implConfigs: Set<Configuration>,
        configs: List<Configuration>,
        attributes: AttributeContainer,
        attributesSchema: AttributesSchema,
        result: MutableSet<Dependency>,
        visited: MutableSet<Configuration> = HashSet()
    ) {
        if (configs.isEmpty()) return

        val allDependencies = configs.flatMap { it.dependencies }
        val allExtendsFrom = configs.flatMap { it.extendsFrom }.toMutableList()

        result.addAll(allDependencies.filter { it !is ProjectDependency })

        val projectDependencies = allDependencies.filterIsInstance<ProjectDependencyInternal>()
        allExtendsFrom.addAll(selectConfigurations(projectDependencies, attributes, attributesSchema))

        doFindDependencies(
            implConfigs,
            allExtendsFrom.filter { it !in implConfigs && visited.add(it) },
            attributes,
            attributesSchema,
            result
        )
    }

    private fun selectConfigurations(
        projectDependencies: List<ProjectDependencyInternal>,
        attributes: AttributeContainer,
        attributesSchema: AttributesSchema
    ): List<Configuration> {
        return projectDependencies.mapNotNull { projectDependency ->
            val rootComponentMetaData = (projectDependency.findProjectConfiguration() as? ConfigurationInternal)?.toRootComponentMetaData()
                ?: return@mapNotNull null

            val matching = AttributeConfigurationSelector.selectConfigurationUsingAttributeMatching(
                (attributes as? AttributeContainerInternal)?.asImmutable() ?: return@mapNotNull null,
                emptyList<Capability>(),
                rootComponentMetaData,
                (attributesSchema as? AttributesSchemaInternal) ?: return@mapNotNull null,
                emptyList()
            )
            projectDependency.dependencyProject.configurations.findByName(matching.name)
        }
    }

    private inline fun <E, K> MutableSet<E>.addOrRetainAll(c: Collection<E>, crossinline selector: (E) -> K) {
        if (isEmpty()) {
            addAll(c)
        } else {
            val selectedSet = c.map(selector).toSet()
            retainAll { selectedSet.contains(selector(it)) }
        }
    }
}