/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.*
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KarLayout.Attributes.CompressionMethod
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.ProjectStructureMetadataTransformAction
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublicationImpl.Companion.RESOURCES_ZIP_EXTENSION
import org.jetbrains.kotlin.gradle.plugin.setUsesPlatformOf
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.copyZipFilePartially
import org.jetbrains.kotlin.gradle.utils.ensureValidZipDirectoryPath
import org.jetbrains.kotlin.gradle.utils.listDescendants
import java.util.zip.ZipFile
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KarLayout.Attributes as KarAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KarLayout.Attributes.State as KarState

private fun <T : Any> TransformSpec<*>.changesAttribute(attribute: Attribute<T>, fromValue: T, toValue: T) {
    from.attribute(attribute, fromValue)
    to.attribute(attribute, toValue)
}

private fun TransformSpec<*>.changesState(fromValue: KarState, toValue: KarState) {
    changesAttribute(KarAttributes.state, fromValue, toValue)
}


private fun TransformSpec<*>.requiresTarget(target: KotlinTarget) {
    from.setUsesPlatformOf(target)
    to.setUsesPlatformOf(target)
}


private abstract class XZDecompressAction : TransformAction<TransformParameters.None> {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val archiveFile = inputArtifact.get().asFile
        val targetFile = outputs.file(archiveFile.nameWithoutExtension)

        archiveFile.inputStream().buffered().use { fileInput ->
            XZCompressorInputStream(fileInput).use { xzInput ->
                targetFile.outputStream().buffered().use { fileOutput ->
                    xzInput.copyTo(fileOutput)
                }
            }
        }
    }
}

internal fun Project.configureTransformActionFromKarXzToKar() {
    dependencies.artifactTypes.maybeCreate(KarLayout.XZ_ARTIFACT_TYPE).apply {
        attributes.attribute(KarAttributes.state, KarState.COMPRESSED)
    }
    dependencies.registerTransform(XZDecompressAction::class.java) { spec ->
        spec.changesState(KarState.COMPRESSED, KarState.DECOMPRESSED)
        spec.changesAttribute(KarAttributes.compressionMethod, CompressionMethod.XZ, CompressionMethod.NONE)
    }
}

@DisableCachingByDefault(because = "Extracting resources from a .kar is not worth caching")
internal abstract class KarToResourcesTransformation : TransformAction<KarToResourcesTransformation.Parameters> {

    interface Parameters : TransformParameters {
        @get:Input
        val resourcesPath: Property<String>
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        copyZipFilePartially(
            sourceZipFile = inputArtifact.get().asFile,
            destinationZipFile = outputs.file("${inputArtifact.get().asFile.nameWithoutExtension}.${RESOURCES_ZIP_EXTENSION}"),
            path = parameters.resourcesPath.get()
        )
    }
}

internal fun KotlinTarget.configureTransformActionFromKarToResources() {
    if (this !is KotlinTargetWithKotlinArchiveSupport) return
    project.dependencies.registerTransform(KarToResourcesTransformation::class.java) { spec ->
        spec.parameters.resourcesPath.set("${KarLayout.RESOURCES_DIRECTORY_NAME}/$platformNameInKotlinArchive")

        spec.requiresTarget(this)

        spec.changesState(KarState.DECOMPRESSED, KarState.RESOURCES_EXTRACTED)
    }
}


@DisableCachingByDefault(because = "Unpacking a .kar is not worth caching")
internal abstract class KarToPlatformArtifactsTransformation : TransformAction<KarToPlatformArtifactsTransformation.Parameters> {

    interface Parameters : TransformParameters {
        @get:Input
        val platformName: Property<String>
    }

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val karFile = inputArtifact.get().asFile
        val platformPath = ensureValidZipDirectoryPath("${KarLayout.PLATFORM_KLIBS_DIRECTORY_NAME}/${parameters.platformName.get()}")
        val cinteropsPath = ensureValidZipDirectoryPath("${KarLayout.CINTEROP_KLIBS_DIRECTORY_NAME}/${parameters.platformName.get()}")

        val cinteropKlibNames = ZipFile(karFile).use { kar ->
            kar.listDescendants(cinteropsPath)
                .map { entry -> entry.name.removePrefix(cinteropsPath).substringBefore('/') }
                .filter { it.isNotEmpty() }
                .distinct()
                .toList()
        }

        val platformKlibDir = outputs.dir("${karFile.nameWithoutExtension}-${parameters.platformName.get()}")

        /**
         * The code below copies:
         * * All content of karFile/platform/{platformName} -> karName-{platformName}/
         * * For each cinterop (located by listing of directories in karFile/cinterops/{platformName}:
         *      content of karFile/cinterop/{platformName}/{cinteropName} -> karName-{platformName}/{cinteropName}
         *
         *  [outputDirectoriesByArchivePath] is a mapping of archive directory path to a fs directory where to put it's content.
         *  Each directory in output dir should be created by outputs.dir to make gradle correctly track them
         */
        val outputDirectoriesByArchivePath = buildMap {
            put(platformPath, platformKlibDir)
            for (cinteropKlibName in cinteropKlibNames) {
                put("$cinteropsPath$cinteropKlibName/", outputs.dir(cinteropKlibName))
            }
        }
        check(outputDirectoriesByArchivePath.values.all { it.parentFile == platformKlibDir.parentFile }) {
            "Expected all transformed KLIB output directories to have the same parent"
        }

        fileSystemOperations.copy { copy ->
            copy.from(archiveOperations.zipTree(karFile)) { spec ->
                outputDirectoriesByArchivePath.keys.forEach { archivePath -> spec.include("$archivePath**") }
                spec.eachFile { file ->
                    val (archivePath, outputDirectory) = outputDirectoriesByArchivePath.entries.first { (archivePath) ->
                        file.path.startsWith(archivePath)
                    }
                    file.path = "${outputDirectory.name}/${file.path.removePrefix(archivePath)}"
                }
                spec.includeEmptyDirs = false
            }
            copy.into(platformKlibDir.parentFile)
        }
    }
}

internal fun KotlinTarget.configureTransformActionFromKarToPlatformArtifacts() {
    if (this !is KotlinTargetWithKotlinArchiveSupport) return
    project.dependencies.registerTransform(KarToPlatformArtifactsTransformation::class.java) { spec ->
        spec.parameters.platformName.set(platformNameInKotlinArchive)

        spec.requiresTarget(this)

        spec.changesState(KarState.DECOMPRESSED, KarState.PLATFORM_ARTIFACTS_EXTRACTED)
    }
}

internal fun Project.configureTransformActionFromKarToPsm() {
    dependencies.registerTransform(ProjectStructureMetadataTransformAction::class.java) { spec ->
        spec.parameters { params ->
            params.psmPath.set(KarLayout.PSM_FILE_PATH)
        }

        spec.changesAttribute(
            USAGE_ATTRIBUTE,
            project.usageByName(KotlinUsages.KOTLIN_METADATA),
            project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA)
        )
        spec.changesState(KarState.DECOMPRESSED, KarState.PSM_EXTRACTED)
    }
}
