/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.commonizer.ResultsConsumer
import org.jetbrains.kotlin.descriptors.commonizer.ResultsConsumer.ModuleResult
import org.jetbrains.kotlin.descriptors.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.LeafTarget
import org.jetbrains.kotlin.descriptors.commonizer.SharedTarget
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BaseWriterImpl
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KotlinLibraryLayoutForWriter
import org.jetbrains.kotlin.library.impl.KotlinLibraryWriterImpl
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile

internal class NativeDistributionResultsConsumer(
    private val repository: File,
    private val originalLibraries: AllNativeLibraries,
    private val destination: File,
    private val copyStdlib: Boolean,
    private val copyEndorsedLibs: Boolean,
    private val logProgress: (String) -> Unit
) : ResultsConsumer {
    private val consumedTargets = LinkedHashSet<CommonizerTarget>()
    private val allLeafTargets = originalLibraries.librariesByTargets.keys
    private val sharedTarget = SharedTarget(allLeafTargets)

    override fun consumeResults(target: CommonizerTarget, moduleResults: Collection<ModuleResult>) {
        val added = consumedTargets.add(target)
        check(added)

        serializeTarget(target, moduleResults)
    }

    override fun successfullyFinished(status: Status) {
        // optimization: stdlib and endorsed libraries effectively remain the same across all Kotlin/Native targets,
        // so they can be just copied to the new destination without running serializer
        copyCommonStandardLibraries()

        when (status) {
            Status.NOTHING_TO_DO -> {
                // It may happen that all targets to be commonized (or at least all but one target) miss platform libraries.
                // In such case commonizer will do nothing and raise a special status value 'NOTHING_TO_DO'.
                // So, let's just copy platform libraries from the target where they are to the new destination.
                allLeafTargets.forEach(::copyTargetAsIs)
            }
            Status.DONE -> {
                // 'targetsToCopy' are some leaf targets with empty set of platform libraries
                val targetsToCopy = allLeafTargets - consumedTargets.filterIsInstance<LeafTarget>()
                targetsToCopy.forEach(::copyTargetAsIs)
            }
        }
    }

    private fun copyCommonStandardLibraries() {
        if (copyStdlib || copyEndorsedLibs) {
            repository.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
                .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
                .listFiles()
                ?.filter { it.isDirectory }
                ?.let {
                    if (copyStdlib) {
                        if (copyEndorsedLibs) it else it.filter { dir -> dir.endsWith(KONAN_STDLIB_NAME) }
                    } else
                        it.filter { dir -> !dir.endsWith(KONAN_STDLIB_NAME) }
                }?.forEach { libraryOrigin ->
                    val libraryDestination = destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).resolve(libraryOrigin.name)
                    libraryOrigin.copyRecursively(libraryDestination)
                }

            val what = listOfNotNull(
                "standard library".takeIf { copyStdlib },
                "endorsed libraries".takeIf { copyEndorsedLibs }
            ).joinToString(separator = " and ")

            logProgress("Copied $what")
        }
    }

    private fun copyTargetAsIs(leafTarget: LeafTarget) {
        val librariesCount = originalLibraries.librariesByTargets.getValue(leafTarget).libraries.size

        val librariesDestination = leafTarget.librariesDestination
        librariesDestination.mkdirs() // always create an empty directory even if there is nothing to copy

        val librariesSource = leafTarget.platformLibrariesSource
        if (librariesSource.isDirectory) librariesSource.copyRecursively(librariesDestination)

        logProgress("Copied $librariesCount libraries for ${leafTarget.prettyName}")
    }

    private fun serializeTarget(target: CommonizerTarget, moduleResults: Collection<ModuleResult>) {
        val librariesDestination = target.librariesDestination
        librariesDestination.mkdirs() // always create an empty directory even if there is nothing to copy

        val manifestProvider = when (target) {
            is LeafTarget -> originalLibraries.librariesByTargets.getValue(target)
            is SharedTarget -> CommonNativeManifestDataProvider(originalLibraries.librariesByTargets.values)
        }

        for (moduleResult in moduleResults) {
            when (moduleResult) {
                is ModuleResult.Commonized -> {
                    val libraryName = moduleResult.libraryName

                    val manifestData = manifestProvider.getManifest(libraryName)
                    val libraryDestination = librariesDestination.resolve(libraryName)

                    writeLibrary(moduleResult.metadata, manifestData, libraryDestination)
                }
                is ModuleResult.Missing -> {
                    val libraryName = moduleResult.libraryName
                    val missingModuleLocation = moduleResult.originalLocation

                    missingModuleLocation.copyRecursively(librariesDestination.resolve(libraryName))
                }
            }
        }

        logProgress("Written libraries for ${target.prettyCommonizedName(sharedTarget)}")
    }

    private fun writeLibrary(
        metadata: SerializedMetadata,
        manifestData: NativeSensitiveManifestData,
        libraryDestination: File
    ) {
        val layout = KFile(libraryDestination.path).let { KotlinLibraryLayoutForWriter(it, it) }
        val library = KotlinLibraryWriterImpl(
            moduleName = manifestData.uniqueName,
            versions = manifestData.versions,
            builtInsPlatform = BuiltInsPlatform.NATIVE,
            nativeTargets = emptyList(), // will be overwritten with NativeSensitiveManifestData.applyTo() below
            nopack = true,
            shortName = manifestData.shortName,
            layout = layout
        )
        library.addMetadata(metadata)
        manifestData.applyTo(library.base as BaseWriterImpl)
        library.commit()
    }

    private val LeafTarget.platformLibrariesSource: File
        get() = repository.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
            .resolve(name)

    private val CommonizerTarget.librariesDestination: File
        get() = when (this) {
            is LeafTarget -> destination.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR).resolve(name)
            is SharedTarget -> destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
        }
}
