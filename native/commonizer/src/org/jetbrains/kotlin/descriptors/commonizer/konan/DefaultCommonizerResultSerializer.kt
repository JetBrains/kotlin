/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.commonizer.api.CommonizerOutputLayout
import org.jetbrains.kotlin.commonizer.api.CommonizerTarget
import org.jetbrains.kotlin.commonizer.api.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.api.SharedCommonizerTarget
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.ModuleResult
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerResult
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerResult.Done
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerResult.NothingToDo
import org.jetbrains.kotlin.descriptors.konan.NATIVE_STDLIB_MODULE_NAME
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BaseWriterImpl
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KotlinLibraryWriterImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.konan.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.util.Logger
import java.io.File

internal class DefaultCommonizerResultSerializer(
    private val destination: File,
    private val destinationLayout: CommonizerOutputLayout,
    private val logger: Logger,
) : CommonizerResultSerializer {

    override fun invoke(
        originalLibraries: AllNativeLibraries,
        commonizerResult: CommonizerResult
    ) {
        when (commonizerResult) {
            is NothingToDo ->
                // It may happen that all targets to be commonized (or at least all but one target) miss platform libraries.
                // In such case commonizer will do nothing and return a special commonizerResult value 'NothingToCommonize'.
                // So, let's just copy platform libraries from the target where they are to the new destination.
                originalLibraries.librariesByTargets.forEach { (target, librariesToCommonize) ->
                    copyTargetAsIs(target, librariesToCommonize.libraries)
                }

            is Done -> {
                val serializer = KlibMetadataMonolithicSerializer(
                    languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                    metadataVersion = KlibMetadataVersion.INSTANCE,
                    skipExpects = false,
                    project = null
                )

                // 'targetsToCopy' are some targets with empty set of platform libraries
                val targetsToCopy = originalLibraries.librariesByTargets.keys - commonizerResult.leafTargets
                if (targetsToCopy.isNotEmpty()) {
                    targetsToCopy.forEach { target ->
                        val librariesToCommonize = originalLibraries.librariesByTargets.getValue(target)
                        copyTargetAsIs(target, librariesToCommonize.libraries)
                    }
                }

                val leafTargetNames = commonizerResult.leafTargets.map { it.name }
                val targetsToSerialize = commonizerResult.leafTargets + commonizerResult.sharedTarget
                targetsToSerialize.forEach { target ->
                    val moduleResults: Collection<ModuleResult> = commonizerResult.modulesByTargets.getValue(target)
                    val newModules: Collection<ModuleDescriptor> = moduleResults.mapNotNull { (it as? ModuleResult.Commonized)?.module }
                    val absentModuleLocations: List<File> =
                        moduleResults.mapNotNull { (it as? ModuleResult.Missing)?.originalLocation }

                    val manifestProvider: NativeManifestDataProvider
                    val starredTarget: String?
                    when (target) {
                        is LeafCommonizerTarget -> {
                            manifestProvider = originalLibraries.librariesByTargets.getValue(target)
                            starredTarget = target.name
                        }
                        is SharedCommonizerTarget -> {
                            manifestProvider = CommonNativeManifestDataProvider(originalLibraries.librariesByTargets.values)
                            starredTarget = null
                        }
                    }

                    val targetName = leafTargetNames.joinToString { if (it == starredTarget) "$it(*)" else it }
                    serializeTarget(target, targetName, newModules, absentModuleLocations, manifestProvider, serializer)
                }
            }
        }
    }

    private fun copyTargetAsIs(leafTarget: LeafCommonizerTarget, libraries: List<NativeLibrary>) {
        // TODO SELLMAIR NOW: How to test this?
        val librariesDestination = destinationLayout.getTargetDirectory(destination, leafTarget)
        librariesDestination.mkdirs() // always create an empty directory even if there is nothing to copy
        libraries.map { it.library.libraryFile.absolutePath }.map(::File).forEach { libraryFile ->
            libraryFile.copyRecursively(destination.resolve(libraryFile.name))
        }
    }

    private fun serializeTarget(
        target: CommonizerTarget,
        targetName: String,
        newModules: Collection<ModuleDescriptor>,
        absentModuleLocations: List<File>,
        manifestProvider: NativeManifestDataProvider,
        serializer: KlibMetadataMonolithicSerializer
    ) {
        val librariesDestination = destinationLayout.getTargetDirectory(destination, target)
        librariesDestination.mkdirs() // always create an empty directory even if there is nothing to copy

        for (newModule in newModules) {
            val libraryName = newModule.name

            if (!shouldBeSerialized(libraryName))
                continue

            val metadata = serializer.serializeModule(newModule)
            val plainName = libraryName.asString().removePrefix("<").removeSuffix(">")

            val manifestData = manifestProvider.getManifest(plainName)
            val libraryDestination = librariesDestination.resolve(plainName)

            writeLibrary(metadata, manifestData, libraryDestination)
        }

        for (absentModuleLocation in absentModuleLocations) {
            val libraryName = absentModuleLocation.name
            absentModuleLocation.copyRecursively(librariesDestination.resolve(libraryName))
        }

        logger.log("Written libraries for [$targetName]")
    }

    private fun writeLibrary(
        metadata: SerializedMetadata,
        manifestData: NativeSensitiveManifestData,
        destination: File
    ) {
        val library = KotlinLibraryWriterImpl(
            libDir = org.jetbrains.kotlin.konan.file.File(destination.path),
            moduleName = manifestData.uniqueName,
            versions = manifestData.versions,
            builtInsPlatform = BuiltInsPlatform.NATIVE,
            nativeTargets = emptyList(), // will be overwritten with NativeSensitiveManifestData.applyTo() below
            nopack = true,
            shortName = manifestData.shortName
        )
        library.addMetadata(metadata)
        manifestData.applyTo(library.base as BaseWriterImpl)
        library.commit()
    }

    private companion object {
        fun shouldBeSerialized(libraryName: Name) =
            libraryName != NATIVE_STDLIB_MODULE_NAME &&
                    libraryName != KlibResolvedModuleDescriptorsFactoryImpl.FORWARD_DECLARATIONS_MODULE_NAME
    }

}
