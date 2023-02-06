/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.SourceSetConstraint
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.*
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyTransformers.IdePlatformStdlibCommonDependencyFilter
import org.jetbrains.kotlin.gradle.targets.native.internal.getCommonizerTarget

internal fun IdeMultiplatformImport(extension: KotlinProjectExtension): IdeMultiplatformImport {
    return IdeMultiplatformImportImpl(extension).apply {

        registerDependencyResolver(
            resolver = IdeDependsOnDependencyResolver,
            constraint = SourceSetConstraint.unconstrained,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeFriendSourceDependencyResolver,
            constraint = SourceSetConstraint.unconstrained,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeVisibleMultiplatformSourceDependencyResolver,
            constraint = !SourceSetConstraint.isLeaf,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeJvmAndAndroidSourceDependencyResolver,
            constraint = SourceSetConstraint.isJvmAndAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeNativeStdlibDependencyResolver,
            constraint = SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        registerDependencyResolver(
            resolver = IdeNativePlatformDependencyResolver,
            constraint = SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeCommonizedNativePlatformDependencyResolver,
            constraint = SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeTransformedMetadataDependencyResolver,
            constraint = !SourceSetConstraint.isLeaf,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeOriginalMetadataDependencyResolver,
            constraint = !SourceSetConstraint.isLeaf,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        registerDependencyResolver(
            resolver = IdeBinaryDependencyResolver(),
            constraint = SourceSetConstraint.isSinglePlatformType,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdePlatformCinteropDependencyResolver,
            constraint = { sourceSet -> SourceSetConstraint.isSingleKotlinTarget(sourceSet) && SourceSetConstraint.isNative(sourceSet) },
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        registerDependencyResolver(
            resolver = IdeCommonizedCinteropDependencyResolver,
            constraint = { sourceSet -> getCommonizerTarget(sourceSet) is SharedCommonizerTarget },
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        registerDependencyResolver(
            resolver = IdeCInteropMetadataDependencyClasspathResolver,
            constraint = { SourceSetConstraint.isNative(it) && !SourceSetConstraint.isSingleKotlinTarget(it) },
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        registerDependencyResolver(
            resolver = IdeProjectToProjectCInteropDependencyResolver,
            constraint = { SourceSetConstraint.isNative(it) && SourceSetConstraint.isSingleKotlinTarget(it) },
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        /*
        Register resolution of dependencies for jvm+android dependencies:
        This resolver will resolve dependencies visible to the source set from a 'jvm' perspective.
         */
        registerDependencyResolver(
            resolver = IdeJvmAndAndroidPlatformBinaryDependencyResolver(extension.project),
            constraint = SourceSetConstraint.isJvmAndAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdePlatformSourcesResolver(),
            constraint = SourceSetConstraint.isSinglePlatformType,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourcesAndDocumentationResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerAdditionalArtifactResolver(
            resolver = IdeMetadataSourcesResolver(),
            constraint = !SourceSetConstraint.isSinglePlatformType,
            phase = IdeMultiplatformImport.AdditionalArtifactResolutionPhase.SourcesAndDocumentationResolution,
            level = IdeMultiplatformImport.AdditionalArtifactResolutionLevel.Default
        )

        if (extension.project.kotlinPropertiesProvider.enableSlowIdeSourcesJarResolver) {
            registerAdditionalArtifactResolver(
                resolver = IdeArtifactResolutionQuerySourcesAndDocumentationResolver,
                constraint = SourceSetConstraint.unconstrained,
                phase = IdeMultiplatformImport.AdditionalArtifactResolutionPhase.SourcesAndDocumentationResolution,
                level = IdeMultiplatformImport.AdditionalArtifactResolutionLevel.Default
            )
        }

        registerDependencyTransformer(
            transformer = IdePlatformStdlibCommonDependencyFilter,
            constraint = SourceSetConstraint.isSinglePlatformType and !SourceSetConstraint.isSharedNative,
            phase = IdeMultiplatformImport.DependencyTransformationPhase.DependencyFilteringPhase,
        )

        registerDependencyEffect(
            effect = IdeDependencyLogger,
            constraint = SourceSetConstraint.unconstrained
        )

        registerExtrasSerializationExtension(kotlinExtrasSerialization)

        /* Overwrite android dependencies by empty resolver */
        registerDependencyResolver(
            resolver = IdeDependencyResolver.Empty,
            constraint = SourceSetConstraint.isAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Overwrite
        )

        registerDependencyResolver(
            resolver = IdeDependencyResolver.Empty,
            constraint = SourceSetConstraint.isAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourcesAndDocumentationResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Overwrite
        )

        registerAdditionalArtifactResolver(
            resolver = IdeAdditionalArtifactResolver.Empty,
            constraint = SourceSetConstraint.isAndroid,
            phase = IdeMultiplatformImport.AdditionalArtifactResolutionPhase.SourcesAndDocumentationResolution,
            level = IdeMultiplatformImport.AdditionalArtifactResolutionLevel.Overwrite
        )
    }
}
