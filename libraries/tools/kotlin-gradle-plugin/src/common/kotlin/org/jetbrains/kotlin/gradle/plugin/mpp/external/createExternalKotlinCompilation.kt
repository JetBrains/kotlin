/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.hierarchy.SourceSetTreeClassifierWrapper
import org.jetbrains.kotlin.gradle.plugin.hierarchy.sourceSetTreeClassifier
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationFriendPathsResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSourceSetsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.*
import org.jetbrains.kotlin.gradle.plugin.mpp.decoratedInstance
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinCompilation.Delegate
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

/**
 *  Creates a compilation for External Kotlin Targets adhering to the configuration provided in the [descriptor]
 *  - The _kind_ of compilation will be chosen automatically by the specified [DecoratedExternalKotlinTarget.platformType]
 *  - The compilation will use the [ExternalKotlinCompilationDescriptor.defaultSourceSet] as its (default) source set
 *  - The compilation wil reference the compile task using the [ExternalKotlinCompilationDescriptor.compileTaskName] if specified
 *  - The compilation will reference the compile all task name using the[ExternalKotlinCompilationDescriptor.compileAllTaskName] if specified
 *  - Compilations .associateWith calls will be handled by the [ExternalKotlinCompilationDescriptor.compilationAssociator] if specified
 *  - An additional friendArtifactResolver will be respected if the [ExternalKotlinCompilationDescriptor.friendArtifactResolver] is specified
 *  - The [ExternalKotlinCompilationDescriptor.configure] method will be called before the compilation is available in [KotlinTarget.compilations]
 *  container
 */
@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinCompilation> DecoratedExternalKotlinTarget.createCompilation(
    descriptor: ExternalKotlinCompilationDescriptor<T>,
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
        compilationTaskNamesContainerFactory = { target, compilationName ->
            val default = DefaultKotlinCompilationTaskNamesContainerFactory.create(target, compilationName)
            default.copy(
                compileTaskName = descriptor.compileTaskName ?: default.compileTaskName,
                compileAllTaskName = descriptor.compileAllTaskName ?: default.compileAllTaskName
            )
        },
        compilationAssociator = @Suppress("unchecked_cast") KotlinCompilationAssociator { _, first, second ->
            descriptor.compilationAssociator.associate(
                first.decoratedInstance as T,
                second.decoratedInstance as DecoratedExternalKotlinCompilation
            )
        },
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
    val decoratedCompilation = descriptor.compilationFactory.create(Delegate(compilationImpl))

    decoratedCompilation.sourceSetTreeClassifier = descriptor.sourceSetTreeClassifierV2
        ?: @Suppress("DEPRECATION") SourceSetTreeClassifierWrapper(descriptor.sourceSetTreeClassifier)

    descriptor.configure?.invoke(decoratedCompilation)
    this.delegate.compilations.add(decoratedCompilation)

    setupCompileTask(decoratedCompilation)

    return decoratedCompilation
}

/**
 * @see createCompilation
 */
@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinCompilation> DecoratedExternalKotlinTarget.createCompilation(
    descriptor: ExternalKotlinCompilationDescriptorBuilder<T>.() -> Unit,
): T {
    return createCompilation(ExternalKotlinCompilationDescriptor(descriptor))
}

private fun DecoratedExternalKotlinTarget.setupCompileTask(
    compilation: DecoratedExternalKotlinCompilation,
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