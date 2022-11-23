/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializer
import org.jetbrains.kotlin.gradle.kpm.idea.kotlinDebugKey
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.*

fun IdeMultiplatformImport(extension: KotlinMultiplatformExtension): IdeMultiplatformImport {
    return IdeMultiplatformImportImpl(extension).apply {

        registerDependencyResolver(
            resolver = IdeDependsOnDependencyResolver,
            constraint = IdeMultiplatformImport.SourceSetConstraint.unconstrained,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeFriendSourceDependencyResolver,
            constraint = IdeMultiplatformImport.SourceSetConstraint.unconstrained,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeVisibleMultiplatformSourceDependencyResolver,
            constraint = IdeMultiplatformImport.SourceSetConstraint.unconstrained,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeTransformedMetadataDependencyResolver,
            constraint = IdeMultiplatformImport.SourceSetConstraint.isMetadata,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeOriginalMetadataDependencyResolver,
            constraint = IdeMultiplatformImport.SourceSetConstraint.isMetadata,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        registerDependencyResolver(
            resolver = IdePlatformBinaryDependencyResolver(),
            constraint = IdeMultiplatformImport.SourceSetConstraint.isPlatform,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        /*
        Register resolution of dependencies for jvm+android dependencies:
        This resolver will resolve dependencies visible to the source set from a 'jvm' perspective.
         */
        registerDependencyResolver(
            resolver = IdeJvmAndAndroidPlatformBinaryDependencyResolver(extension.project),
            constraint = IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeNativePlatformDependencyResolver,
            constraint = IdeMultiplatformImport.SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default
        )

        registerDependencyResolver(
            resolver = IdeNativeStdlibDependencyResolver,
            constraint = IdeMultiplatformImport.SourceSetConstraint.isNative,
            phase = IdeMultiplatformImport.DependencyResolutionPhase.BinaryDependencyResolution,
            level = IdeMultiplatformImport.DependencyResolutionLevel.Default,
        )

        registerExtrasSerializationExtension {
            register(kotlinDebugKey, IdeaKotlinExtrasSerializer.javaIoSerializable())
        }
    }
}
