package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.awaitMetadataCompilationsCreated
import org.jetbrains.kotlin.gradle.targets.metadata.findMetadataCompilation
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.setAttribute
import org.jetbrains.kotlin.tooling.core.UnsafeApi

/* Elements configuration */

internal val CInteropCommonizedCInteropApiElementsConfigurationsSetupAction = KotlinProjectSetupCoroutine setup@{
    val extension = multiplatformExtensionOrNull ?: return@setup
    val cinteropCommonizerTask = commonizeCInteropTask() ?: return@setup

    /*
    Expose api dependencies from Source Sets to the elements configuration
    */
    extension.awaitSourceSets().forEach { sourceSet ->
        val commonizerTarget = sourceSet.commonizerTarget.await() as? SharedCommonizerTarget ?: return@forEach
        val configuration = locateOrCreateCommonizedCInteropApiElementsConfiguration(commonizerTarget)
        val metadataCompilation = findMetadataCompilation(sourceSet) ?: return@forEach
        configuration.extendsFrom(metadataCompilation.internal.configurations.apiConfiguration)
    }

    /*
    Expose artifacts from cinterop commonizer
     */
    for (commonizerGroup in kotlinCInteropGroups.await()) {
        for (target in commonizerGroup.targets) {
            val configuration = locateOrCreateCommonizedCInteropApiElementsConfiguration(target)
            val commonizerTargetOutputDir = cinteropCommonizerTask.map { task ->
                CommonizerOutputFileLayout.resolveCommonizedDirectory(task.outputDirectory(commonizerGroup), target)
            }

            project.artifacts.add(configuration.name, commonizerTargetOutputDir) { artifact ->
                artifact.extension = CInteropCommonizerArtifactTypeAttribute.KLIB_COLLECTION_DIR
                artifact.type = CInteropCommonizerArtifactTypeAttribute.KLIB_COLLECTION_DIR
                artifact.builtBy(cinteropCommonizerTask)
            }
        }
    }
}

internal fun Project.locateOrCreateCommonizedCInteropApiElementsConfiguration(commonizerTarget: SharedCommonizerTarget): Configuration {
    val configurationName = commonizerTarget.identityString + "CInteropApiElements"
    configurations.findByName(configurationName)?.let { return it }

    return configurations.createConsumable(configurationName).also { configuration ->
        setupBasicCommonizedCInteropConfigurationAttributes(configuration, commonizerTarget)

        launch {
            val metadataTarget = multiplatformExtension.metadataTarget
            metadataTarget.awaitMetadataCompilationsCreated()
                .filter { compilation -> compilation.commonizerTarget.await() == commonizerTarget }
                .forEach { compilation -> configuration.extendsFrom(compilation.internal.configurations.apiConfiguration) }
        }
    }
}


/* Dependency configuration */

/**
 * Gives access the 'commonized cinterop dependency configuration' of the given [sourceSet].
 * The access is forced through this 'view' because the provided 'artifact view' is forced to be lenient as protective measure.
 *
 * If dependencies do not provide corresponding commonized cinterop element configurations then we should not fail the build!
 */
internal suspend fun Project.createCommonizedCInteropDependencyConfigurationView(sourceSet: KotlinSourceSet): FileCollection {
    @OptIn(UnsafeApi::class)
    val configuration = locateOrCreateCommonizedCInteropDependencyConfiguration(sourceSet) ?: return files()
    return configuration.incoming.artifactView { view -> view.isLenient = true }.files
}

@UnsafeApi("Use createCommonizedCInteropDependencyConfigurationView instead")
internal suspend fun Project.locateOrCreateCommonizedCInteropDependencyConfiguration(
    sourceSet: KotlinSourceSet,
): Configuration? {
    val commonizerTarget = sourceSet.commonizerTarget.await() ?: return null
    if (commonizerTarget !is SharedCommonizerTarget) return null

    val configurationName = sourceSet.name + "CInterop"
    configurations.findByName(configurationName)?.let { return it }

    val configuration = configurations.createResolvable(configurationName).also { configuration ->
        configuration.isVisible = false

        // Extends from Metadata Configuration associated with given source set to ensure matching
        configuration.extendsFrom(sourceSet.internal.resolvableMetadataConfiguration)
        setupBasicCommonizedCInteropConfigurationAttributes(configuration, commonizerTarget)

        /**
         * Dependencies require the [CInteropCommonizerArtifactTypeAttribute.KLIB].
         * If artifacts are added using [CInteropCommonizerArtifactTypeAttribute.KLIB_COLLECTION_DIR] then a transformation
         * has to happen that will resolve the exact list of klibs.
         */
        configuration.attributes.setAttribute(
            CInteropCommonizerArtifactTypeAttribute.attribute,
            CInteropCommonizerArtifactTypeAttribute.KLIB
        )
        configuration.description = "Commonized CInterop dependencies for $sourceSet with targets: '$commonizerTarget'."
    }

    return configuration
}

private fun Project.setupBasicCommonizedCInteropConfigurationAttributes(
    configuration: Configuration,
    commonizerTarget: SharedCommonizerTarget,
) {
    configuration.attributes.setAttribute(CommonizerTargetAttribute.attribute, commonizerTarget.identityString)
    configuration.attributes.setAttribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_COMMONIZED_CINTEROP))
    configuration.attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
}
