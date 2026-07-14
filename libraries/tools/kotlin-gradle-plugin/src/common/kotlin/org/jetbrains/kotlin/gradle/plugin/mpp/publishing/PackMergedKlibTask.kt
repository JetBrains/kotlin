/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.transform.TransformSpec
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.artifacts.metadataFragmentIdentifier
import org.jetbrains.kotlin.gradle.artifacts.metadataPublishedArtifacts
import org.jetbrains.kotlin.gradle.artifacts.publishedMetadataCompilations
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.ProjectStructureMetadataTransformAction
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerCompositeMetadataJarBundling.cinteropMetadataDirectoryPath
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerDependent
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeCInteropTask
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizedOutputDirectory
import org.jetbrains.kotlin.gradle.targets.native.internal.from
import org.jetbrains.kotlin.gradle.utils.copyZipFilePartially
import org.jetbrains.kotlin.gradle.utils.registerTransformForArtifactType
import javax.inject.Inject

private const val PACK_MERGED_KLIB_TASK_NAME = "packMergedKlibTask"
internal const val MERGED_KLIB_ARTIFACT_TYPE = "mklib"
internal const val MERGED_KLIB_USAGE_SUFFIX = "-merged"

/**
 * These attributes are only used to force the [UnpackMergedKlibTransform] in resolvable configurations.
 * They are not used in consumable configurations and are never published (cf. 'uklibStateAttribute').
 */
internal val mergedKlibStateAttribute = Attribute.of("org.jetbrains.kotlin.mergedKlibState", String::class.java)
internal val mergedKlibStatePacked = "packed"
internal val mergedKlibStateUnpacked = "unpacked"

/**
 * Path of a given target's klib inside the merged klib archive.
 *
 * The path is derived from attributes that producer and consumer share (platform type, konan target, wasm target type)
 * instead of the producer's target name, so that consumers can locate the klib without knowing how the producer
 * named its targets.
 */
internal val KotlinTarget.mergedKlibPlatformPath: String?
    get() = when (this) {
        is KotlinNativeTarget -> "platform/${KotlinPlatformType.native.name}/${konanTarget.name}"
        is KotlinJsIrTarget -> when (wasmTargetType) {
            null -> "platform/${KotlinPlatformType.js.name}"
            KotlinWasmTargetType.JS -> "platform/${KotlinPlatformType.wasm.name}/${KotlinWasmTargetAttribute.js.name}"
            KotlinWasmTargetType.WASI -> "platform/${KotlinPlatformType.wasm.name}/${KotlinWasmTargetAttribute.wasi.name}"
        }
        else -> null
    }

internal val SetupMergedKlibTask = KotlinProjectSetupCoroutine {
    Stage.AfterFinaliseCompilations.await()

    val extension = project.multiplatformExtensionOrNull ?: return@KotlinProjectSetupCoroutine
    val compileKlibTasks = extension.awaitTargets().associateWith { target ->
        when (target) {
            is KotlinNativeTarget -> target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).compileTaskProvider
            is KotlinJsIrTarget -> target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).compileTaskProvider
            else -> null
        }
    }

    val packTask = tasks.register(PACK_MERGED_KLIB_TASK_NAME, Zip::class.java) { zip ->
        compileKlibTasks.forEach { (target, compileTaskProvider) ->
            val compileTask = compileTaskProvider?.get() ?: return@forEach
            val platformPath = target.mergedKlibPlatformPath ?: return@forEach
            zip.into(platformPath) { spec ->
                zip.dependsOn(compileTask)
                if (compileTask.produceUnpackagedKlib.get()) {
                    spec.from(compileTask.klibDirectory)
                } else {
                    spec.from(zipTree(compileTask.klibOutput))
                }
            }
        }

        zip.destinationDirectory.set(layout.buildDirectory.dir("merged-klib"))
        zip.archiveExtension.set(MERGED_KLIB_ARTIFACT_TYPE)
        zip.archiveFileName.set("merged-klib.$MERGED_KLIB_ARTIFACT_TYPE")
    }

    /* Include all metadata compile klibs */
    val metadataTarget = extension.metadataTarget
    metadataTarget.publishedMetadataCompilations().forEach { compilation ->
        packTask.configure { zip ->
            zip.into("metadata/${compilation.metadataFragmentIdentifier}") { spec ->
                spec.from(compilation.metadataPublishedArtifacts)
            }
        }

        if (compilation is KotlinSharedNativeCompilation) run {
            // duplicated from 'includeCommonizedCInteropMetadata' (deduplicate later)
            val commonizerTask = commonizeCInteropTask()?.get() ?: return@run
            val commonizerDependencyToken = CInteropCommonizerDependent.from(compilation) ?: return@run
            val outputDirectory = commonizerTask.commonizedOutputDirectory(commonizerDependencyToken) ?: return@run

            packTask.configure { zip ->
                zip.into("metadata/${cinteropMetadataDirectoryPath(compilation.defaultSourceSet.name)}") { spec ->
                    spec.from(outputDirectory)
                }
            }
        }
    }

    val psmTask = project.locateOrRegisterGenerateProjectStructureMetadataTask()
    packTask.configure { zip ->
        zip.into("metadata") { spec ->
            spec.from(psmTask)
        }
    }

    project.configurations.getByName(metadataTarget.internal.apiElementsConfigurationName).apply {
        outgoing.artifact(packTask)
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA + MERGED_KLIB_USAGE_SUFFIX))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
    }

    configureMergedKlibTransformation()
    configureTransformActionFromMklibToPsm()
}


internal val Project.packMergedKlibTask: TaskProvider<Task>
    get() = tasks.named(PACK_MERGED_KLIB_TASK_NAME)


/**
 * Allows platform compilations to consume merged klibs published by [org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetSoftwareComponent]:
 *
 * - At graph level, [MergedKlibUsageCompatibilityRule] lets configurations requesting e.g. 'kotlin-api'
 *   select variants published with the 'kotlin-api-merged' usage.
 * - At artifact level, '.mklib' artifacts carry [mergedKlibStatePacked] while klib-consuming configurations
 *   request [mergedKlibStateUnpacked]; the mismatch forces [UnpackMergedKlibTransform] which extracts the
 *   klib matching the consumer's platform type and konan target from the merged klib.
 */
internal fun Project.configureMergedKlibTransformation() {
    dependencies.attributesSchema.attribute(USAGE_ATTRIBUTE) { strategy ->
        strategy.compatibilityRules.add(MergedKlibUsageCompatibilityRule::class.java)
    }

    dependencies.artifactTypes.maybeCreate(MERGED_KLIB_ARTIFACT_TYPE)
        .attributes.attribute(mergedKlibStateAttribute, mergedKlibStatePacked)

    multiplatformExtension.targets.configureEach { target ->
        val platformPath = target.mergedKlibPlatformPath ?: return@configureEach
        target.registerUnpackMergedKlibTransform(platformPath)
        target.requestUnpackedMergedKlibs()
    }
}

private fun KotlinTarget.registerUnpackMergedKlibTransform(platformPath: String) {
    project.dependencies.registerTransform(UnpackMergedKlibTransform::class.java) { spec ->
        spec.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, MERGED_KLIB_ARTIFACT_TYPE)
        spec.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, "klib")

        spec.from.attribute(mergedKlibStateAttribute, mergedKlibStatePacked)
        spec.to.attribute(mergedKlibStateAttribute, mergedKlibStateUnpacked)

        /* Disambiguate the per-target transform registrations by the same attributes the consumer requests */
        spec.attributeForBothEnds(KotlinPlatformType.attribute, this.platformType)
        if (this is KotlinNativeTarget) {
            spec.attributeForBothEnds(KotlinNativeTarget.konanTargetAttribute, this.konanTarget.name)
        }
        if (this is KotlinJsIrTarget) {
            when (wasmTargetType) {
                KotlinWasmTargetType.JS -> KotlinWasmTargetAttribute.js
                KotlinWasmTargetType.WASI -> KotlinWasmTargetAttribute.wasi
                null -> null
            }?.let { wasmTargetAttributeValue ->
                spec.attributeForBothEnds(KotlinWasmTargetAttribute.wasmTargetAttribute, wasmTargetAttributeValue)
            }
        }

        spec.parameters.platformPath.set(platformPath)
    }
}

private fun <T : Any> TransformSpec<*>.attributeForBothEnds(attribute: Attribute<T>, value: T) {
    from.attribute(attribute, value)
    to.attribute(attribute, value)
}

private fun KotlinTarget.requestUnpackedMergedKlibs() {
    compilations.configureEach { compilation ->
        val configurations = compilation.internal.configurations
        configurations.compileDependencyConfiguration
            .attributes.attribute(mergedKlibStateAttribute, mergedKlibStateUnpacked)
        configurations.runtimeDependencyConfiguration
            ?.attributes?.attribute(mergedKlibStateAttribute, mergedKlibStateUnpacked)
    }
}

/**
 * Merged klib variants are published with the original usage plus [MERGED_KLIB_USAGE_SUFFIX]
 * (see [org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetSoftwareComponent]). This rule allows consumers requesting the original usage
 * (e.g. 'kotlin-api') to select such variants; the actual klib is then extracted by [UnpackMergedKlibTransform].
 */
internal class MergedKlibUsageCompatibilityRule : AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
        val consumerUsage = consumerValue.name ?: return@with
        val producerUsage = producerValue.name ?: return@with
        if (producerUsage == consumerUsage + MERGED_KLIB_USAGE_SUFFIX) compatible()

        /*
         * 'kotlin-metadata' consumers (e.g. host-specific metadata configurations used by the metadata transformation)
         * may fall back to the platform 'kotlin-api' variant when no metadata variant is published for the target
         * (see KotlinUsages.KotlinMetadataCompatibility). Keep this fallback working when the platform variant
         * is published as a merged klib.
         */
        if (consumerUsage == KotlinUsages.KOTLIN_METADATA && producerUsage == KotlinUsages.KOTLIN_API + MERGED_KLIB_USAGE_SUFFIX) {
            compatible()
        }
    }
}

/**
 * Extracts the klib stored under [Parameters.platformPath] from a merged klib archive
 * into an unpacked klib directory.
 */
@DisableCachingByDefault(because = "Unpacking a merged klib is not worth caching")
internal abstract class UnpackMergedKlibTransform @Inject constructor(
    private val fileOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : TransformAction<UnpackMergedKlibTransform.Parameters> {

    interface Parameters : TransformParameters {
        @get:Input
        val platformPath: Property<String>
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val mergedKlib = inputArtifact.get().asFile
        val platformPath = parameters.platformPath.get()

        val outputFile = outputs.file(
            "${mergedKlib.nameWithoutExtension}-${platformPath.substringAfter('/').replace('/', '-')}.klib"
        )

        copyZipFilePartially(mergedKlib, outputFile, platformPath + "/")
    }
}

private fun Project.configureTransformActionFromMklibToPsm() {
    dependencies.registerTransformForArtifactType(
        ProjectStructureMetadataTransformAction::class.java,
        fromArtifactType = MERGED_KLIB_ARTIFACT_TYPE,
        toArtifactType = KotlinUsages.KOTLIN_PSM_METADATA
    ) { transform ->
        transform.parameters { params ->
            params.psmPath.set("metadata/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME")
        }
        transform.from.apply {
            attributes.attribute(USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA + MERGED_KLIB_USAGE_SUFFIX))
        }
        transform.to.apply {
            attributes.attribute(USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
        }
    }
}
