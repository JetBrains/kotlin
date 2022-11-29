/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationFriendPathsResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSourceSetsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory.KotlinCompilationTaskNamesContainerFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.decoratedInstance
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalDecoratedKotlinCompilation.Delegate
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

@ExternalKotlinTargetApi
fun <T : ExternalDecoratedKotlinCompilation> DecoratedExternalKotlinTarget.createCompilation(
    descriptor: ExternalKotlinCompilationDescriptor<T>
): T {
    val compilationImplFactory = KotlinCompilationImplFactory(
        compilerOptionsFactory = when (platformType) {
            KotlinPlatformType.common -> KotlinMultiplatformCommonCompilerOptionsFactory
            KotlinPlatformType.jvm -> KotlinJvmCompilerOptionsFactory
            KotlinPlatformType.androidJvm -> KotlinJvmCompilerOptionsFactory
            KotlinPlatformType.js -> KotlinJsCompilerOptionsFactory
            KotlinPlatformType.native -> KotlinNativeCompilerOptionsFactory
            KotlinPlatformType.wasm -> KotlinMultiplatformCommonCompilerOptionsFactory
        },
        compilationSourceSetsContainerFactory = { _, _ -> KotlinCompilationSourceSetsContainer(descriptor.defaultSourceSet) },
        compilationTaskNamesContainerFactory = KotlinCompilationTaskNamesContainerFactory { target, compilationName ->
            val default = DefaultKotlinCompilationTaskNamesContainerFactory.create(target, compilationName)
            default.copy(
                compileTaskName = descriptor.compileTaskName ?: default.compileTaskName,
                compileAllTaskName = descriptor.compileAllTaskName ?: default.compileAllTaskName
            )
        },
        compilationAssociator = descriptor.compilationAssociator?.let { declaredAssociator ->
            @Suppress("unchecked_cast")
            KotlinCompilationAssociator { _, first, second ->
                declaredAssociator.associate(first.decoratedInstance as T, second.decoratedInstance as ExternalDecoratedKotlinCompilation)
            }
        } ?: DefaultKotlinCompilationAssociator,
        compilationFriendPathsResolver = DefaultKotlinCompilationFriendPathsResolver(
            DefaultKotlinCompilationFriendPathsResolver.FriendArtifactResolver.composite(
                DefaultKotlinCompilationFriendPathsResolver.DefaultFriendArtifactResolver,
                descriptor.friendArtifactResolver?.let { declaredResolver ->
                    DefaultKotlinCompilationFriendPathsResolver.FriendArtifactResolver { compilation ->
                        @Suppress("unchecked_cast")
                        declaredResolver.resolveFriendPaths(compilation as T)
                    }
                }
            )
        )
    )

    val compilationImpl = compilationImplFactory.create(this, descriptor.compilationName)
    val decoratedCompilation = descriptor.decoratedKotlinCompilationFactory.create(Delegate(compilationImpl))
    descriptor.configure?.invoke(decoratedCompilation)
    this.delegate.compilations.add(decoratedCompilation)

    setupCompileTask(decoratedCompilation)

    return decoratedCompilation
}

@ExternalKotlinTargetApi
fun <T : ExternalDecoratedKotlinCompilation> DecoratedExternalKotlinTarget.createCompilation(
    descriptor: ExternalKotlinCompilationDescriptorBuilder<T>.() -> Unit
): T {
    return createCompilation(ExternalKotlinCompilationDescriptor(descriptor))
}


@OptIn(ExternalKotlinTargetApi::class)
private fun DecoratedExternalKotlinTarget.setupCompileTask(
    compilation: ExternalDecoratedKotlinCompilation
) {
    val tasksProvider = KotlinTasksProvider()
    val compilationInfo = KotlinCompilationInfo(compilation)

    val sourceSetProcessor = when (platformType) {
        KotlinPlatformType.common ->
            KotlinCommonSourceSetProcessor(compilationInfo, tasksProvider)

        KotlinPlatformType.jvm, KotlinPlatformType.androidJvm ->
            Kotlin2JvmSourceSetProcessor(tasksProvider, compilationInfo)

        KotlinPlatformType.js ->
            KotlinJsIrSourceSetProcessor(tasksProvider, compilationInfo)

        else -> throw UnsupportedOperationException("KotlinPlatformType.$platformType is not supported")
    }

    sourceSetProcessor.run()

    logger.info("Created compile task: ${sourceSetProcessor.kotlinTask.name}")
}