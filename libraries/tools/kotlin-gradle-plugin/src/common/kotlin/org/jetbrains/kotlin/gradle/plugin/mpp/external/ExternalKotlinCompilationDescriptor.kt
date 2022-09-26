/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.DecoratedKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.*
import kotlin.properties.Delegates

@ExternalKotlinTargetApi
interface ExternalKotlinCompilationDescriptor<T : ExternalKotlinCompilation> {
    fun interface DecoratedKotlinCompilationFactory<T : DecoratedKotlinCompilation<*>> {
        fun create(delegate: ExternalKotlinCompilation.Delegate): T
    }

    fun interface FriendArtifactResolver<T : ExternalKotlinCompilation> {
        fun resolveFriendPaths(compilation: T): FileCollection
    }

    fun interface CompilationAssociator<T : ExternalKotlinCompilation> {
        fun associate(compilation: T, main: ExternalKotlinCompilation)
    }

    val compilationName: String
    val compileTaskName: String?
    val compileAllTaskName: String?
    val defaultSourceSet: KotlinSourceSet
    val decoratedKotlinCompilationFactory: DecoratedKotlinCompilationFactory<T>
    val friendArtifactResolver: FriendArtifactResolver<T>?
    val compilationAssociator: CompilationAssociator<T>?
    val configure: ((T) -> Unit)?
}

@ExternalKotlinTargetApi
fun <T : ExternalKotlinCompilation> ExternalKotlinCompilationDescriptor(
    configure: ExternalKotlinCompilationDescriptorBuilder<T>.() -> Unit
): ExternalKotlinCompilationDescriptor<T> {
    return ExternalKotlinCompilationDescriptorBuilderImpl<T>().also(configure).run {
        ExternalKotlinCompilationDescriptorImpl(
            compilationName = compilationName,
            compileTaskName = compileTaskName,
            compileAllTaskName = compileAllTaskName,
            defaultSourceSet = defaultSourceSet,
            decoratedKotlinCompilationFactory = decoratedKotlinCompilationFactory,
            friendArtifactResolver = friendArtifactResolver,
            compilationAssociator = compilationAssociator,
            configure = this.configure
        )
    }
}

@ExternalKotlinTargetApi
interface ExternalKotlinCompilationDescriptorBuilder<T : ExternalKotlinCompilation> {
    var compilationName: String
    var compileTaskName: String?
    var compileAllTaskName: String?
    var defaultSourceSet: KotlinSourceSet
    var decoratedKotlinCompilationFactory: DecoratedKotlinCompilationFactory<T>
    var friendArtifactResolver: FriendArtifactResolver<T>?
    var compilationAssociator: CompilationAssociator<T>?
    var configure: ((T) -> Unit)?
    fun configure(action: (T) -> Unit) = apply {
        val configure = this.configure
        if (configure == null) this.configure = action
        else this.configure = { configure(it); action(it) }
    }
}

@ExternalKotlinTargetApi
private class ExternalKotlinCompilationDescriptorBuilderImpl<T : ExternalKotlinCompilation> :
    ExternalKotlinCompilationDescriptorBuilder<T> {
    override var compilationName: String by Delegates.notNull()
    override var compileTaskName: String? = null
    override var compileAllTaskName: String? = null
    override var defaultSourceSet: KotlinSourceSet by Delegates.notNull()
    override var decoratedKotlinCompilationFactory: DecoratedKotlinCompilationFactory<T> by Delegates.notNull()
    override var friendArtifactResolver: FriendArtifactResolver<T>? = null
    override var compilationAssociator: CompilationAssociator<T>? = null
    override var configure: ((T) -> Unit)? = null
}

@ExternalKotlinTargetApi
private data class ExternalKotlinCompilationDescriptorImpl<T : ExternalKotlinCompilation>(
    override val compilationName: String,
    override val compileTaskName: String?,
    override val compileAllTaskName: String?,
    override val defaultSourceSet: KotlinSourceSet,
    override val decoratedKotlinCompilationFactory: DecoratedKotlinCompilationFactory<T>,
    override val friendArtifactResolver: FriendArtifactResolver<T>?,
    override val compilationAssociator: CompilationAssociator<T>?,
    override val configure: ((T) -> Unit)?
) : ExternalKotlinCompilationDescriptor<T>