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
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
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
            val getInstance = AndroidSdkHandler::class.java.getMethodOrNull("getInstance", Path::class.java) ?: return null
            getInstance(null, androidExtension.sdkDirectory.toPath()) as AndroidSdkHandler
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

    fun getAndroidSourceSetDependencies(project: Project): Map<String, List<AndroidDependency>?> {
        val androidPluginVersions = getAndroidPluginVersions() ?: return emptyMap()
        if (!isAndroidPluginCompatible(androidPluginVersions)) return emptyMap()

        data class SourceSetConfigs(val implConfig: Configuration, val compileConfig: Configuration)

        val androidSdkJar = getAndroidSdkJar(project, androidPluginVersions) ?: return emptyMap()
        val sourceSet2Impl = HashMap<String, SourceSetConfigs>()
        val allImplConfigs = HashSet<Configuration>()

        project.forEachVariant { variant ->
            val compileConfig = variant.compileConfiguration
            variant.sourceSets.filterIsInstance(AndroidSourceSet::class.java).map {
                val implConfig = project.configurations.getByName(it.implementationConfigurationName)
                allImplConfigs.add(implConfig)
                sourceSet2Impl[lowerCamelCaseName("android", it.name)] = SourceSetConfigs(implConfig, compileConfig)
            }
        }

        return sourceSet2Impl.mapValues { entry ->
            val dependencies = findDependencies(allImplConfigs, entry.value.implConfig)

            val selfResolved = dependencies.filterIsInstance<SelfResolvingDependency>().map { dependency ->
                val collection = dependency.resolve().takeIf(Collection<File?>::isNotEmpty)
                AndroidDependency(dependency.name, group = dependency.group, collection = collection)
            }
            val resolvedExternal = dependencies.filterIsInstance<ExternalModuleDependency>()
                .flatMap { collectDependencies(it.module, entry.value.compileConfig) }

            val result = (selfResolved + resolvedExternal + androidSdkJar).toMutableList()

            if (entry.key == "androidMain") {
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
        module: ModuleIdentifier,
        compileClasspathConf: Configuration
    ): List<AndroidDependency> {
        val processedJarArtifactType = try {
            AndroidArtifacts.ArtifactType.valueOf("PROCESSED_JAR")
        } catch (e: IllegalArgumentException) {
            AndroidArtifacts.ArtifactType.JAR
        }
        val viewConfig: (ArtifactView.ViewConfiguration) -> Unit = { config ->
            config.attributes { it.attribute(AndroidArtifacts.ARTIFACT_TYPE, processedJarArtifactType.type) }
            config.isLenient = true
        }

        val resolvedArtifacts = compileClasspathConf.incoming.artifactView(viewConfig).artifacts.mapNotNull {
            val componentIdentifier = it.id.componentIdentifier as? ModuleComponentIdentifier ?: return@mapNotNull null
            val id = componentIdentifier.moduleIdentifier ?: return@mapNotNull null
            id to AndroidDependency(id.name, it.file, null, id.group, componentIdentifier.version)
        }.toMap()

        val resolutionResults = compileClasspathConf.incoming.resolutionResult.allComponents.mapNotNull {
            val id = it.moduleVersion?.module ?: return@mapNotNull null
            id to it
        }.toMap()

        val deps = HashSet<ModuleIdentifier>().also { doCollectDependencies(listOf(module), resolutionResults, it) }
        return deps.mapNotNull { resolvedArtifacts[it] }
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

    private fun findDependencies(implConfigs: Set<Configuration>, conf: Configuration): Set<Dependency> {
        return HashSet<Dependency>().also { doFindDependencies(implConfigs, listOf(conf), it) }
    }

    private tailrec fun doFindDependencies(
        implConfigs: Set<Configuration>, configs: List<Configuration>, result: MutableSet<Dependency>,
        visited: MutableSet<Configuration> = HashSet()
    ) {
        if (configs.isEmpty()) return
        result.addAll(configs.flatMap { it.dependencies })
        doFindDependencies(implConfigs, configs.flatMap { it.extendsFrom }.filter { it !in implConfigs && visited.add(it) }, result)
    }
}