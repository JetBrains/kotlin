/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExternalVariantApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentGranularMetadataResolverFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.utils.UnsafeApi

@OptIn(UnsafeApi::class)
internal fun IdeaKotlinProjectModelBuilder.Companion.default(
    extension: KotlinPm20ProjectExtension
) = IdeaKotlinProjectModelBuilderImpl(extension).apply {
    val fragmentMetadataResolverFactory = FragmentGranularMetadataResolverFactory()

    registerDependencyResolver(
        resolver = IdeaKotlinSourceDependencyResolver(fragmentMetadataResolverFactory),
        constraint = IdeaKotlinProjectModelBuilder.FragmentConstraint.unconstrained,
        phase = IdeaKotlinProjectModelBuilder.DependencyResolutionPhase.SourceDependencyResolution,
        level = IdeaKotlinProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKotlinMetadataBinaryDependencyResolver(fragmentMetadataResolverFactory),
        constraint = !IdeaKotlinProjectModelBuilder.FragmentConstraint.isVariant,
        phase = IdeaKotlinProjectModelBuilder.DependencyResolutionPhase.BinaryDependencyResolution,
        level = IdeaKotlinProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKotlinOriginalMetadataDependencyResolver(fragmentMetadataResolverFactory),
        constraint = !IdeaKotlinProjectModelBuilder.FragmentConstraint.isVariant,
        phase = IdeaKotlinProjectModelBuilder.DependencyResolutionPhase.BinaryDependencyResolution,
        level = IdeaKotlinProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKotlinPlatformDependencyResolver(),
        constraint = IdeaKotlinProjectModelBuilder.FragmentConstraint.isVariant,
        phase = IdeaKotlinProjectModelBuilder.DependencyResolutionPhase.BinaryDependencyResolution,
        level = IdeaKotlinProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKotlinNativeStdlibDependencyResolver,
        constraint = IdeaKotlinProjectModelBuilder.FragmentConstraint.isNative,
        phase = IdeaKotlinProjectModelBuilder.DependencyResolutionPhase.BinaryDependencyResolution,
        level = IdeaKotlinProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKotlinNativePlatformDependencyResolver(),
        constraint = IdeaKotlinProjectModelBuilder.FragmentConstraint.isNative,
        phase = IdeaKotlinProjectModelBuilder.DependencyResolutionPhase.BinaryDependencyResolution,
        level = IdeaKotlinProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKotlinSourcesAndDocumentationResolver(),
        constraint = IdeaKotlinProjectModelBuilder.FragmentConstraint.unconstrained,
        phase = IdeaKotlinProjectModelBuilder.DependencyResolutionPhase.PostDependencyResolution,
        level = IdeaKotlinProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyTransformer(
        transformer = IdeaKotlinSinglePlatformStdlibCommonFilter,
        phase = IdeaKotlinProjectModelBuilder.DependencyTransformationPhase.DependencyFilteringPhase,
        constraint = IdeaKotlinProjectModelBuilder.FragmentConstraint.unconstrained
    )

    registerDependencyTransformer(
        transformer = IdeaKotlinUnusedSourcesAndDocumentationFilter,
        phase = IdeaKotlinProjectModelBuilder.DependencyTransformationPhase.DependencyFilteringPhase,
        constraint = IdeaKotlinProjectModelBuilder.FragmentConstraint.unconstrained
    )

    registerDependencyEffect(
        effect = IdeaKotlinDependencyLogger,
        constraint = IdeaKotlinProjectModelBuilder.FragmentConstraint.unconstrained
    )

    registerDependencyEffect(
        effect = IdeaKotlinMissingFileDependencyLogger,
        constraint = IdeaKotlinProjectModelBuilder.FragmentConstraint.unconstrained
    )
}
