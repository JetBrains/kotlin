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
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizerTarget

internal fun IdeMultiplatformImport(extension: KotlinProjectExtension): IdeMultiplatformImport {
    return IdeMultiplatformImportImpl(extension).apply {

        registerDependencyResolver(
            resolver = IdeDependsOnDependencyResolver,
            constraint = SourceSetConstraint.unconstrained,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeFriendSourceDependencyResolver,
            constraint = SourceSetConstraint.unconstrained,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeVisibleMultiplatformSourceDependencyResolver,
            constraint = !SourceSetConstraint.isLeaf,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeJvmAndAndroidSourceDependencyResolver,
            constraint = SourceSetConstraint.isJvmAndAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeNativeStdlibDependencyResolver,
            constraint = SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeNativePlatformDependencyResolver,
            constraint = SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeCommonizedNativePlatformDependencyResolver,
            constraint = SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeTransformedMetadataDependencyResolver,
            constraint = !SourceSetConstraint.isLeaf and !SourceSetConstraint.isJvmAndAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeOriginalMetadataDependencyResolver,
            constraint = !SourceSetConstraint.isLeaf,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeBinaryDependencyResolver(),
            constraint = SourceSetConstraint.isSinglePlatformType,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdePlatformCinteropDependencyResolver,
            constraint = SourceSetConstraint.isSingleKotlinTarget and SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeCommonizedCinteropDependencyResolver,
            constraint = { sourceSet -> sourceSet.commonizerTarget.getOrThrow() is SharedCommonizerTarget },
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeCInteropMetadataDependencyClasspathResolver,
            constraint = SourceSetConstraint.isNative and !SourceSetConstraint.isSingleKotlinTarget,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdeProjectToProjectCInteropDependencyResolver,
            constraint = SourceSetConstraint.isNative and SourceSetConstraint.isSingleKotlinTarget,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        /*
        Register resolution of dependencies for jvm+android dependencies:
        This resolver will resolve dependencies visible to the source set from a 'jvm' perspective.
         */
        registerDependencyResolver(
            resolver = IdeJvmAndAndroidPlatformBinaryDependencyResolver(extension.project),
            constraint = SourceSetConstraint.isJvmAndAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerDependencyResolver(
            resolver = IdePlatformSourcesResolver(),
            constraint = SourceSetConstraint.isSinglePlatformType,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourcesAndDocumentationResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        registerAdditionalArtifactResolver(
            resolver = IdeMetadataSourcesResolver(),
            constraint = !SourceSetConstraint.isSinglePlatformType,
            phase = IdeMultiplatformImport.AdditionalArtifactResolutionPhase.SourcesAndDocumentationResolution,
            priority = IdeMultiplatformImport.Priority.normal
        )

        if (extension.project.kotlinPropertiesProvider.enableSlowIdeSourcesJarResolver) {
            registerAdditionalArtifactResolver(
                resolver = IdeArtifactResolutionQuerySourcesAndDocumentationResolver,
                constraint = SourceSetConstraint.unconstrained,
                phase = IdeMultiplatformImport.AdditionalArtifactResolutionPhase.SourcesAndDocumentationResolution,
                priority = IdeMultiplatformImport.Priority.normal
            )
        }

        registerDependencyTransformer(
            transformer = IdePlatformStdlibCommonDependencyFilter,
            constraint = SourceSetConstraint.isSinglePlatformType and !SourceSetConstraint.isSharedNative
                    or SourceSetConstraint.isJvmAndAndroid,
            phase = IdeMultiplatformImport.DependencyTransformationPhase.DependencyFilteringPhase,
        )

        registerDependencyEffect(
            effect = IdeDependencyLogger,
            constraint = SourceSetConstraint.unconstrained
        )

        registerExtrasSerializationExtension(kotlinExtrasSerialization)

        /* Overwrite android dependencies by empty resolver */
        registerDependencyResolver(
            resolver = IdeDependencyResolver.empty,
            constraint = SourceSetConstraint.isAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            priority = IdeMultiplatformImport.Priority.high
        )

        registerDependencyResolver(
            resolver = IdeDependencyResolver.empty,
            constraint = SourceSetConstraint.isAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourcesAndDocumentationResolution,
            priority = IdeMultiplatformImport.Priority.high
        )

        registerAdditionalArtifactResolver(
            resolver = IdeAdditionalArtifactResolver.empty,
            constraint = SourceSetConstraint.isAndroid,
            phase = IdeMultiplatformImport.AdditionalArtifactResolutionPhase.SourcesAndDocumentationResolution,
            priority = IdeMultiplatformImport.Priority.high
        )
    }
}
