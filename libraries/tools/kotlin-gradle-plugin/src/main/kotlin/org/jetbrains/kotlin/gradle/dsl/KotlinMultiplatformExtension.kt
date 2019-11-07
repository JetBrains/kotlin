/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.SdkLocationSourceSet
import com.android.build.gradle.internal.SdkLocator
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ARTIFACT_TYPE
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.android.sdklib.IAndroidTarget
import com.android.sdklib.repository.AndroidSdkHandler
import com.android.sdklib.repository.LoggerProgressIndicatorWrapper
import groovy.lang.Closure
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File

open class KotlinMultiplatformExtension :
    KotlinProjectExtension(),
    KotlinTargetContainerWithPresetFunctions,
    KotlinTargetContainerWithNativeShortcuts
{
    override lateinit var presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>
        internal set

    override lateinit var targets: NamedDomainObjectCollection<KotlinTarget>
        internal set

    @Suppress("unused") // DSL
    val testableTargets: NamedDomainObjectCollection<KotlinTargetWithTests<*, *>>
        get() = targets.withType(KotlinTargetWithTests::class.java)

    internal var isGradleMetadataAvailable: Boolean = false
    internal var isGradleMetadataExperimental: Boolean = false

    fun metadata(configure: KotlinOnlyTarget<KotlinCommonCompilation>.() -> Unit = { }): KotlinOnlyTarget<KotlinCommonCompilation> =
        @Suppress("UNCHECKED_CAST")
        (targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME) as KotlinOnlyTarget<KotlinCommonCompilation>).also(configure)

    fun metadata(configure: Closure<*>) = metadata { ConfigureUtil.configure(configure, this) }

    fun <T : KotlinTarget> targetFromPreset(
        preset: KotlinTargetPreset<T>,
        name: String = preset.name,
        configure: T.() -> Unit = { }
    ): T = configureOrCreate(name, preset, configure)

    fun targetFromPreset(preset: KotlinTargetPreset<*>, name: String, configure: Closure<*>) =
        targetFromPreset(preset, name) { ConfigureUtil.configure(configure, this) }

    fun targetFromPreset(preset: KotlinTargetPreset<*>) = targetFromPreset(preset, preset.name) { }
    fun targetFromPreset(preset: KotlinTargetPreset<*>, name: String) = targetFromPreset(preset, name) { }
    fun targetFromPreset(preset: KotlinTargetPreset<*>, configure: Closure<*>) = targetFromPreset(preset, preset.name, configure)

    internal val rootSoftwareComponent: KotlinSoftwareComponent by lazy {
        KotlinSoftwareComponentWithCoordinatesAndPublication("kotlin", targets)
    }

    private fun getAndroidSdkJar(project: Project): File? {
        val androidExtension = project.extensions.findByName("android") as BaseExtension? ?: return null
        val sdkLocation = SdkLocator.getSdkLocation(SdkLocationSourceSet(project.rootDir)).directory ?: return null
        val sdkHandler = AndroidSdkHandler.getInstance(sdkLocation)
        val logger = LoggerProgressIndicatorWrapper(LoggerWrapper(project.logger))
        val androidTarget = sdkHandler.getAndroidTargetManager(logger).getTargetFromHashString(androidExtension.compileSdkVersion, logger)
        return File(androidTarget.getPath(IAndroidTarget.ANDROID_JAR))
    }

    fun getAndroidSourceSetDependencies(project: Project): Map<String, List<File>?> {
        data class SourceSetConfigs(val implConfig: Configuration, val compileConfig: Configuration)

        val androidSdkJar = getAndroidSdkJar(project) ?: return emptyMap()
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

            val selfResolved = dependencies.filterIsInstance<SelfResolvingDependency>().flatMap { it.resolve() }
            val resolvedExternal = dependencies.filterIsInstance<DefaultExternalModuleDependency>()
                .flatMap { collectDependencies(it.module, entry.value.compileConfig) }

            val result = (selfResolved + resolvedExternal + androidSdkJar).toMutableList()

            if (entry.key == "androidMain") {
                // this is a terrible hack, but looks like the only way, other than proper support via light-classes
                val task = project.tasks.findByName("processDebugResources") as? LinkApplicationAndroidResourcesTask
                @Suppress("UnstableApiUsage")
                task?.rClassOutputJar?.orNull?.asFile?.let { result += it }
            }

            result
        }.toMap()
    }

    @Suppress("UnstableApiUsage")
    private fun collectDependencies(
        module: ModuleIdentifier,
        compileClasspathConf: Configuration
    ): List<File> {
        val viewConfig: (ArtifactView.ViewConfiguration) -> Unit = { config ->
            config.attributes { it.attribute(ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.JAR.type) }
            config.isLenient = true
        }

        val resolvedArtifacts = compileClasspathConf.incoming.artifactView(viewConfig).artifacts.mapNotNull {
            val id = (it.id.componentIdentifier as? DefaultModuleComponentIdentifier)?.moduleIdentifier ?: return@mapNotNull null
            id to it
        }.toMap()

        val resolutionResults = compileClasspathConf.incoming.resolutionResult.allComponents.mapNotNull {
            val id = it.moduleVersion?.module ?: return@mapNotNull null
            id to it
        }.toMap()

        val deps = HashSet<ModuleIdentifier>().also { doCollectDependencies(listOf(module), resolutionResults, it) }
        return deps.mapNotNull { resolvedArtifacts[it] }.map { it.file }
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

internal fun KotlinTarget.isProducedFromPreset(kotlinTargetPreset: KotlinTargetPreset<*>): Boolean =
    preset == kotlinTargetPreset

internal fun <T : KotlinTarget> KotlinTargetsContainerWithPresets.configureOrCreate(
    targetName: String,
    targetPreset: KotlinTargetPreset<T>,
    configure: T.() -> Unit
): T {
    val existingTarget = targets.findByName(targetName)
    when {
        existingTarget?.isProducedFromPreset(targetPreset) ?: false -> {
            @Suppress("UNCHECKED_CAST")
            configure(existingTarget as T)
            return existingTarget
        }
        existingTarget == null -> {
            val newTarget = targetPreset.createTarget(targetName)
            targets.add(newTarget)
            configure(newTarget)
            return newTarget
        }
        else -> {
            throw InvalidUserCodeException(
                "The target '$targetName' already exists, but it was not created with the '${targetPreset.name}' preset. " +
                        "To configure it, access it by name in `kotlin.targets`" +
                        " or use the preset function '${existingTarget.preset?.name}'."
                            .takeIf { existingTarget.preset != null } ?: "."
            )
        }
    }
}
