/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExternalVariantApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmExtrasSerializer
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragmentGranularMetadataResolverFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.tooling.core.UnsafeApi

@OptIn(UnsafeApi::class)
internal fun IdeaKpmProjectModelBuilder.Companion.default(
    extension: KotlinPm20ProjectExtension
) = IdeaKpmProjectModelBuilderImpl(extension).apply {
    val fragmentMetadataResolverFactory = GradleKpmFragmentGranularMetadataResolverFactory()

    registerDependencyResolver(
        resolver = IdeaKpmRefinesDependencyResolver,
        constraint = IdeaKpmProjectModelBuilder.FragmentConstraint.unconstrained,
        phase = IdeaKpmProjectModelBuilder.DependencyResolutionPhase.SourceDependencyResolution,
        level = IdeaKpmProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKpmGranularFragmentDependencyResolver(fragmentMetadataResolverFactory),
        constraint = IdeaKpmProjectModelBuilder.FragmentConstraint.unconstrained,
        phase = IdeaKpmProjectModelBuilder.DependencyResolutionPhase.SourceDependencyResolution,
        level = IdeaKpmProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKpmMetadataBinaryDependencyResolver(fragmentMetadataResolverFactory),
        constraint = !IdeaKpmProjectModelBuilder.FragmentConstraint.isVariant,
        phase = IdeaKpmProjectModelBuilder.DependencyResolutionPhase.BinaryDependencyResolution,
        level = IdeaKpmProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKpmOriginalMetadataDependencyResolver(fragmentMetadataResolverFactory),
        constraint = !IdeaKpmProjectModelBuilder.FragmentConstraint.isVariant,
        phase = IdeaKpmProjectModelBuilder.DependencyResolutionPhase.BinaryDependencyResolution,
        level = IdeaKpmProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKpmPlatformDependencyResolver(),
        constraint = IdeaKpmProjectModelBuilder.FragmentConstraint.isVariant,
        phase = IdeaKpmProjectModelBuilder.DependencyResolutionPhase.BinaryDependencyResolution,
        level = IdeaKpmProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKpmNativeStdlibDependencyResolver,
        constraint = IdeaKpmProjectModelBuilder.FragmentConstraint.isNative,
        phase = IdeaKpmProjectModelBuilder.DependencyResolutionPhase.BinaryDependencyResolution,
        level = IdeaKpmProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKpmNativePlatformDependencyResolver(),
        constraint = IdeaKpmProjectModelBuilder.FragmentConstraint.isNative,
        phase = IdeaKpmProjectModelBuilder.DependencyResolutionPhase.BinaryDependencyResolution,
        level = IdeaKpmProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyResolver(
        resolver = IdeaKpmSourcesAndDocumentationResolver(),
        constraint = IdeaKpmProjectModelBuilder.FragmentConstraint.unconstrained,
        phase = IdeaKpmProjectModelBuilder.DependencyResolutionPhase.PostDependencyResolution,
        level = IdeaKpmProjectModelBuilder.DependencyResolutionLevel.Default
    )

    registerDependencyTransformer(
        transformer = IdeaKpmSinglePlatformStdlibCommonFilter,
        phase = IdeaKpmProjectModelBuilder.DependencyTransformationPhase.DependencyFilteringPhase,
        constraint = IdeaKpmProjectModelBuilder.FragmentConstraint.unconstrained
    )

    registerDependencyTransformer(
        transformer = IdeaKpmUnusedSourcesAndDocumentationFilter,
        phase = IdeaKpmProjectModelBuilder.DependencyTransformationPhase.DependencyFilteringPhase,
        constraint = IdeaKpmProjectModelBuilder.FragmentConstraint.unconstrained
    )

    registerDependencyEffect(
        effect = IdeaKpmDependencyLogger,
        constraint = IdeaKpmProjectModelBuilder.FragmentConstraint.unconstrained
    )

    registerDependencyEffect(
        effect = IdeaKpmMissingFileDependencyLogger,
        constraint = IdeaKpmProjectModelBuilder.FragmentConstraint.unconstrained
    )

    registerExtrasSerializationExtension {
        /* For transporting debugging breadcrumbs into the ide */
        register(kotlinDebugKey, IdeaKpmExtrasSerializer.javaIoSerializable())
    }
}
