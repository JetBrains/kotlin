/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializer
import org.jetbrains.kotlin.gradle.kpm.idea.kotlinDebugKey
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.SourceSetConstraint
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.*
import org.jetbrains.kotlin.gradle.plugin.sources.internal

fun IdeMultiplatformImport(extension: KotlinMultiplatformExtension): IdeMultiplatformImport {
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
            constraint = SourceSetConstraint.unconstrained,
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
            constraint = SourceSetConstraint.isMetadata,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeOriginalMetadataDependencyResolver,
            constraint = SourceSetConstraint.isMetadata,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        registerDependencyResolver(
            resolver = IdePlatformDependencyResolver(),
            constraint = SourceSetConstraint.isPlatform,
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
            constraint = IdeMultiplatformImport.SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        /* Overwrite android dependencies by empty resolver */
        registerDependencyResolver(
            resolver = IdeDependencyResolver.Empty,
            constraint = SourceSetConstraint { sourceSet ->
                sourceSet.internal.compilations.map { it.platformType }.toSet() == setOf(androidJvm)
            },
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Overwrite
        )

        registerExtrasSerializationExtension {
            register(kotlinDebugKey, IdeaKotlinExtrasSerializer.javaIoSerializable())
        }
    }
}
