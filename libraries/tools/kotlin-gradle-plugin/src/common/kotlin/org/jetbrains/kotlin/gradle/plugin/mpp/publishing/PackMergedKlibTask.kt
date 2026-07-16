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
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.internal.artifacts.transform.UnzipTransform
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.GradleVersion
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.artifacts.metadataFragmentIdentifier
import org.jetbrains.kotlin.gradle.artifacts.metadataPublishedArtifacts
import org.jetbrains.kotlin.gradle.artifacts.publishedMetadataCompilations
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.metadataDescriptor
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.ProjectStructureMetadataTransformAction
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
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
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.copyZipFilePartially
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.registerTransformForArtifactType
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

private const val PACK_KAR_TASK_NAME = "packKar"
internal const val KAR_ARTIFACT_TYPE = "kar"

interface const val KAR_CONFIGURATION = "kotlinArchive"

/**
 * These attributes are only used to force the [KarToPlatformKlibTransformation] in resolvable configurations.
 * They are not used in consumable configurations and are never published (cf. 'uklibStateAttribute').
 */
internal val karStateAttribute = Attribute.of("org.jetbrains.kotlin.kar.state", String::class.java)
internal val karStateCompressed = "compressed"
internal val karStatePacked = "packed"
internal val karStateUnpacked = "unpacked"
internal val karStateProcessed = "processed"

internal val karCompressionMethodAttribute = Attribute.of("org.jetbrains.kotlin.kar.compression.method", String::class.java)
internal val karCompressionMethodNone = "none"
internal val karCompressionMethodXZ = "xz"

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

    val packTask = tasks.register(PACK_KAR_TASK_NAME, PackKarTask::class.java) { zip ->
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

        zip.destinationDirectory.set(layout.buildDirectory.dir("kar"))
        zip.archiveExtension.set(KAR_ARTIFACT_TYPE)
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

    /* Include all cinterops */
    extension.awaitTargets().filterIsInstance<KotlinNativeTarget>().forEach { target ->
        target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.forEach { cinterop ->
            val outputFile = tasks.named<CInteropProcess>(cinterop.interopProcessingTaskName).flatMap { cinteropTask ->
                cinteropTask.outputFileProvider
            }

            packTask.configure { zip ->
                zip.into("cinterops/${target.mergedKlibPlatformPath}/${outputFile.get().name}") { spec ->
                    spec.from(outputFile)
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

    project.configurations.create(KAR_CONFIGURATION).apply {
        extendsFrom(project.configurations.getByName(metadataTarget.internal.apiElementsConfigurationName))

        outgoing.artifact(packTask)
        attributes.attribute(karCompressionMethodAttribute, karCompressionMethodXZ)
        isCanBeConsumed = false
        isCanBeResolved = false

        if (GradleVersion.current() >= GradleVersion.version("8.2")) {
            isCanBeDeclared = true
        }
    }

    project.launch {
        multiplatformExtension.awaitSourceSets().forEach { sourceSet ->
            sourceSet.internal.resolvableMetadataConfiguration.apply {
                attributes.attribute(karStateAttribute, karStatePacked)
            }
        }
    }

    configureKarKlibTransformation()
    configureTransformActionFromKarToPsm()
}


internal val Project.packMergedKlibTask: TaskProvider<Task>
    get() = tasks.named(PACK_KAR_TASK_NAME)


/**
 * Allows platform compilations to consume merged klibs published by [org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetSoftwareComponent]:
 *
 * - At graph level, [MergedKlibUsageCompatibilityRule] lets configurations requesting e.g. 'kotlin-api'
 *   select variants published with the 'kotlin-api-merged' usage.
 * - At artifact level, '.mklib' artifacts carry [karStatePacked] while klib-consuming configurations
 *   request [karStateProcessed]; the mismatch forces [KarToPlatformKlibTransformation] which extracts the
 *   klib matching the consumer's platform type and konan target from the merged klib.
 */
internal fun Project.configureKarKlibTransformation() {
    dependencies.artifactTypes.maybeCreate(KAR_ARTIFACT_TYPE).apply {
        attributes.attribute(karStateAttribute, karStateCompressed)
    }

    multiplatformExtension.targets.configureEach { target ->
        val platformPath = target.mergedKlibPlatformPath ?: return@configureEach
        target.registerUnpackMergedKlibTransform(platformPath)
        target.requestProcessedKar()
    }
}

private fun KotlinTarget.registerUnpackMergedKlibTransform(platformPath: String) {
    project.dependencies.registerTransform(XZDecompressAction::class.java) { spec ->
        spec.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, KAR_ARTIFACT_TYPE)
        spec.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, KAR_ARTIFACT_TYPE)

        spec.from.attribute(karStateAttribute, karStateCompressed)
        spec.to.attribute(karStateAttribute, karStatePacked)

        spec.from.attribute(karCompressionMethodAttribute, karCompressionMethodXZ)
        spec.to.attribute(karCompressionMethodAttribute, karCompressionMethodNone)
    }

    project.dependencies.registerTransform(UnzipTransform::class.java) { spec ->
        spec.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, KAR_ARTIFACT_TYPE)
        spec.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, KAR_ARTIFACT_TYPE)

        spec.from.attribute(karCompressionMethodAttribute, karCompressionMethodNone)

        spec.from.attribute(karStateAttribute, karStatePacked)
        spec.to.attribute(karStateAttribute, karStateUnpacked)
    }


    project.dependencies.registerTransform(KarToPlatformKlibTransformation::class.java) { spec ->
        spec.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, KAR_ARTIFACT_TYPE)
        spec.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, "klib")

        spec.from.attribute(karStateAttribute, karStateUnpacked)
        spec.to.attribute(karStateAttribute, karStateProcessed)

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

private fun KotlinTarget.requestProcessedKar() {
    compilations.configureEach { compilation ->
        val configurations = compilation.internal.configurations

        configurations.compileDependencyConfiguration.apply {
            attributes.attribute(karStateAttribute, karStateProcessed)
        }

        configurations.runtimeDependencyConfiguration?.apply {
            attributes.attribute(karStateAttribute, karStateProcessed)
        }
    }
}

/**
 * Extracts the klib stored under [Parameters.platformPath] from a merged klib archive
 * into an unpacked klib directory.
 */
@DisableCachingByDefault(because = "Unpacking a merged klib is not worth caching")
internal abstract class KarToPlatformKlibTransformation @Inject constructor(
    private val fileOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : TransformAction<KarToPlatformKlibTransformation.Parameters> {

    interface Parameters : TransformParameters {
        @get:Input
        val platformPath: Property<String>
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val karDirectory = inputArtifact.get().asFile
        val platformKlib = karDirectory.resolve(parameters.platformPath.get())
        val platformTargetKlibFile = outputs.dir(platformKlib.name)
        platformKlib.copyRecursively(platformTargetKlibFile)

        val cinteropsDirectory = karDirectory.resolve("cinterops").resolve(parameters.platformPath.get())
        cinteropsDirectory.listFiles().orEmpty().forEach { cinteropKlib ->
            val cinteropTargetKlibFile = outputs.dir(cinteropKlib.name)
            cinteropKlib.copyRecursively(cinteropTargetKlibFile)
        }
    }
}

private fun Project.configureTransformActionFromKarToPsm() {
    dependencies.registerTransformForArtifactType(
        ProjectStructureMetadataTransformAction::class.java,
        fromArtifactType = KAR_ARTIFACT_TYPE,
        toArtifactType = KotlinUsages.KOTLIN_PSM_METADATA
    ) { transform ->
        transform.parameters { params ->
            params.psmPath.set("metadata/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME")
        }
        transform.from.apply {
            attributes.attribute(USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
            attributes.attribute(karStateAttribute, karStatePacked)
            attributes.attribute(karCompressionMethodAttribute, karCompressionMethodNone)
        }
        transform.to.apply {
            attributes.attribute(USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
        }
    }
}
