package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropKlibLibraryElements.cinteropKlibLibraryElements
import org.jetbrains.kotlin.gradle.utils.markConsumable
import org.jetbrains.kotlin.gradle.utils.markResolvable
import org.jetbrains.kotlin.tooling.core.UnsafeApi

/* Elements configuration */

internal fun Project.locateOrCreateCommonizedCInteropApiElementsConfiguration(commonizerTarget: SharedCommonizerTarget): Configuration {
    val configurationName = commonizerTarget.identityString + "CInteropApiElements"
    configurations.findByName(configurationName)?.let { return it }

    return configurations.create(configurationName) { configuration ->
        configuration.markConsumable()
        setupBasicCommonizedCInteropConfigurationAttributes(configuration, commonizerTarget)
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

    val configurationName = commonizerTarget.identityString + "CInterop"
    configurations.findByName(configurationName)?.let { return it }

    val configuration = configurations.create(configurationName) { configuration ->
        configuration.isVisible = false
        configuration.markResolvable()

        // Extends from Metadata Configuration associated with given source set to ensure matching
        configuration.extendsFrom(sourceSet.internal.resolvableMetadataConfiguration)
        setupBasicCommonizedCInteropConfigurationAttributes(configuration, commonizerTarget)

        /**
         * Dependencies require the [CInteropCommonizerArtifactTypeAttribute.KLIB].
         * If artifacts are added using [CInteropCommonizerArtifactTypeAttribute.KLIB_COLLECTION_DIR] then a transformation
         * has to happen that will resolve the exact list of klibs.
         */
        configuration.attributes.attribute(CInteropCommonizerArtifactTypeAttribute.attribute, CInteropCommonizerArtifactTypeAttribute.KLIB)
        description = "Commonized CInterop dependencies for targets: '$commonizerTarget'."
    }

    return configuration
}

private fun Project.setupBasicCommonizedCInteropConfigurationAttributes(
    configuration: Configuration,
    commonizerTarget: SharedCommonizerTarget,
) {
    configuration.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    configuration.attributes.attribute(CommonizerTargetAttribute.attribute, commonizerTarget.identityString)
    configuration.attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, cinteropKlibLibraryElements())
    configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_CINTEROP))
    configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
}
