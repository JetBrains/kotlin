/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationFriendPathsResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSourceSetInclusion
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSourceSetsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinNativeCompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.DefaultKotlinCompilationDependencyConfigurationsFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinNativeCompilerOptionsFactory
import org.jetbrains.kotlin.konan.target.KonanTarget

open class KotlinSharedNativeCompilationFactory internal constructor(
    final override val target: KotlinMetadataTarget,
    private val konanTargets: Set<KonanTarget>,
    private val defaultSourceSet: KotlinSourceSet
) : KotlinCompilationFactory<KotlinSharedNativeCompilation> {

    override val itemClass: Class<KotlinSharedNativeCompilation>
        get() = KotlinSharedNativeCompilation::class.java

    private val compilationImplFactory: KotlinCompilationImplFactory =
        KotlinCompilationImplFactory(
            compilationDependencyConfigurationsFactory = DefaultKotlinCompilationDependencyConfigurationsFactory.WithoutRuntime,
            compilerOptionsFactory = KotlinNativeCompilerOptionsFactory,
            compilationAssociator = KotlinNativeCompilationAssociator,

            /*
            Metadata compilation will only include directly added source sets to the compile task
             */
            compilationSourceSetInclusion = KotlinCompilationSourceSetInclusion(
                addSourcesToCompileTask = KotlinCompilationSourceSetInclusion.AddSourcesWithoutDependsOnClosure(
                    KotlinCompilationSourceSetInclusion.NativeAddSourcesToCompileTask
                )
            ),

            compilationFriendPathsResolver = DefaultKotlinCompilationFriendPathsResolver(
                friendArtifactResolver = DefaultKotlinCompilationFriendPathsResolver.FriendArtifactResolver.composite(
                    DefaultKotlinCompilationFriendPathsResolver.DefaultFriendArtifactResolver,
                    DefaultKotlinCompilationFriendPathsResolver.AdditionalMetadataFriendArtifactResolver
                )
            ),

            /*
            Metadata compilations are created *because* of a pre-existing SourceSet.
            We therefore can create the container inline
             */
            compilationSourceSetsContainerFactory = { _, _ -> KotlinCompilationSourceSetsContainer(defaultSourceSet) },
        )

    override fun create(name: String): KotlinSharedNativeCompilation {
        return target.project.objects.newInstance(
            itemClass, konanTargets.toList(), compilationImplFactory.create(target, name)
        )
    }
}
