/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializer
import org.jetbrains.kotlin.gradle.kpm.idea.kotlinDebugKey
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.SourceSetConstraint
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.*

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
            constraint = SourceSetConstraint.unconstrained,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
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
            resolver = IdePlatformDependencyResolver(),
            constraint = SourceSetConstraint.isLeaf,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
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
            resolver = IdeNativePlatformDependencyResolver,
            constraint = SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeNativeStdlibDependencyResolver,
            constraint = SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        /* Overwrite android dependencies by empty resolver */
        registerDependencyResolver(
            resolver = IdeDependencyResolver.Empty,
            constraint = SourceSetConstraint.isAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Overwrite
        )

        registerExtrasSerializationExtension {
            register(kotlinDebugKey, IdeaKotlinExtrasSerializer.javaIoSerializable())
        }
    }
}
