/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinSourceSetTreeClassifier
import org.jetbrains.kotlin.gradle.plugin.mpp.DecoratedKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.*
import kotlin.properties.Delegates

/**
 * Describes how a compilation created for an external [KotlinTarget] should be created.
 * @see createCompilation
 */
@ExternalKotlinTargetApi
interface ExternalKotlinCompilationDescriptor<T : DecoratedExternalKotlinCompilation> {

    /**
     * Factory creating the decorated instance ([DecoratedExternalKotlinCompilation] using the backing implementation inside
     * the [DecoratedExternalKotlinCompilation.Delegate]
     */
    @ExternalKotlinTargetApi
    fun interface CompilationFactory<T : DecoratedKotlinCompilation<*>> {
        fun create(delegate: DecoratedExternalKotlinCompilation.Delegate): T
    }

    /**
     * Providing an additional [FriendArtifactResolver] will allow to add 'friend artifacts' to the compilation
     */
    @ExternalKotlinTargetApi
    fun interface FriendArtifactResolver<T : DecoratedExternalKotlinCompilation> {
        fun resolveFriendPaths(compilation: T): FileCollection
    }

    /**
     * Implementing a [CompilationAssociator] allows for intercepting the [KotlinCompilation.associateWith] calls, effectively
     * handling them on behalf of the authors of the external [KotlinTarget]
     */
    @ExternalKotlinTargetApi
    fun interface CompilationAssociator<in T : DecoratedExternalKotlinCompilation> {
        fun associate(auxiliary: T, main: DecoratedExternalKotlinCompilation)

        @ExternalKotlinTargetApi
        companion object {
            val default = CompilationAssociator<DecoratedExternalKotlinCompilation> { aux, main ->
                DefaultKotlinCompilationAssociator.associate(aux.target, aux.compilation.internal, main.compilation.internal)
            }
        }
    }

    val compilationName: String
    val compileTaskName: String?
    val compileAllTaskName: String?
    val defaultSourceSet: KotlinSourceSet
    val compilationFactory: CompilationFactory<T>
    val friendArtifactResolver: FriendArtifactResolver<T>?
    val compilationAssociator: CompilationAssociator<T>

    @Suppress("DEPRECATION")
    @Deprecated("Use .sourceSetTreeClassifierV2 instead")
    val sourceSetTreeClassifier: SourceSetTreeClassifier
    val sourceSetTreeClassifierV2: KotlinSourceSetTreeClassifier?

    val configure: ((T) -> Unit)?
}

@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinCompilation> ExternalKotlinCompilationDescriptor(
    configure: ExternalKotlinCompilationDescriptorBuilder<T>.() -> Unit,
): ExternalKotlinCompilationDescriptor<T> {
    return ExternalKotlinCompilationDescriptorBuilder<T>().also(configure).run {
        ExternalKotlinCompilationDescriptorImpl(
            compilationName = compilationName,
            compileTaskName = compileTaskName,
            compileAllTaskName = compileAllTaskName,
            defaultSourceSet = defaultSourceSet,
            compilationFactory = compilationFactory,
            friendArtifactResolver = friendArtifactResolver,
            compilationAssociator = compilationAssociator,
            sourceSetTreeClassifier = @Suppress("DEPRECATION") sourceSetTreeClassifier,
            sourceSetTreeClassifierV2 = sourceSetTreeClassifierV2,
            configure = this.configure
        )
    }
}

@ExternalKotlinTargetApi
class ExternalKotlinCompilationDescriptorBuilder<T : DecoratedExternalKotlinCompilation> internal constructor() {
    var compilationName: String by Delegates.notNull()
    var compileTaskName: String? = null
    var compileAllTaskName: String? = null
    var defaultSourceSet: KotlinSourceSet by Delegates.notNull()
    var compilationFactory: CompilationFactory<T> by Delegates.notNull()
    var friendArtifactResolver: FriendArtifactResolver<T>? = null
    var compilationAssociator: CompilationAssociator<T> = CompilationAssociator.default

    @Suppress("DEPRECATION")
    @Deprecated("Use sourceSetTreeClassifierV2 instead")
    var sourceSetTreeClassifier: SourceSetTreeClassifier = SourceSetTreeClassifier.Default
    var sourceSetTreeClassifierV2: KotlinSourceSetTreeClassifier? = null


    internal var configure: ((T) -> Unit)? = null

    fun configure(action: (T) -> Unit) = apply {
        val configure = this.configure
        if (configure == null) this.configure = action
        else this.configure = { configure(it); action(it) }
    }
}

private data class ExternalKotlinCompilationDescriptorImpl<T : DecoratedExternalKotlinCompilation>(
    override val compilationName: String,
    override val compileTaskName: String?,
    override val compileAllTaskName: String?,
    override val defaultSourceSet: KotlinSourceSet,
    override val compilationFactory: CompilationFactory<T>,
    override val friendArtifactResolver: FriendArtifactResolver<T>?,
    override val compilationAssociator: CompilationAssociator<T>,
    @Deprecated("Use .sourceSetTreeClassifierV2 instead")
    @Suppress("DEPRECATION")
    override val sourceSetTreeClassifier: SourceSetTreeClassifier,
    override val sourceSetTreeClassifierV2: KotlinSourceSetTreeClassifier?,
    override val configure: ((T) -> Unit)?,
) : ExternalKotlinCompilationDescriptor<T>