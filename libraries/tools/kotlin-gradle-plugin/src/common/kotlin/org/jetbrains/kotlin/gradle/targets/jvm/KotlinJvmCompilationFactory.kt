/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationFriendPathsResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinJvmCompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.DefaultKotlinCompilationSourceSetsContainerFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.DefaultKotlinCompilationSourceSetsContainerFactory.DefaultDefaultSourceSetNaming
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinJvmCompilerOptionsFactory
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.javaSourceSets
import org.jetbrains.kotlin.tooling.core.extrasKeyOf

open class KotlinJvmCompilationFactory internal constructor(
    final override val target: KotlinJvmTarget
) : KotlinCompilationFactory<KotlinJvmCompilation> {

    private val sourceSetsNaming = DefaultDefaultSourceSetNaming
    private val compilationImplFactory: KotlinCompilationImplFactory =
        KotlinCompilationImplFactory(
            compilationSourceSetsContainerFactory = DefaultKotlinCompilationSourceSetsContainerFactory(
                defaultSourceSetNaming = sourceSetsNaming,
            ),
            compilationFriendPathsResolver = DefaultKotlinCompilationFriendPathsResolver(
                friendArtifactResolver = DefaultKotlinCompilationFriendPathsResolver.FriendArtifactResolver.composite(
                    DefaultKotlinCompilationFriendPathsResolver.DefaultFriendArtifactResolver,
                    DefaultKotlinCompilationFriendPathsResolver.AdditionalJvmFriendArtifactResolver
                )
            ),
            compilerOptionsFactory = KotlinJvmCompilerOptionsFactory,
            compilationAssociator = KotlinJvmCompilationAssociator,
        )

    override val itemClass: Class<KotlinJvmCompilation>
        get() = KotlinJvmCompilation::class.java

    override fun create(name: String): KotlinJvmCompilation {
        // Dirty hack: Marking that we are in the compilation creation mode so any new invocation of
        // AbstractKotlinPlugin.setUpJavaSourceSets.javaSourceSets.all {} will be ignored as it is the default java source set
        // for this new compilation.
        // Otherwise, '.all {}' is triggered immediately on source set creation and tries to create a
        // new compilation for it as compilation for it is not yet created.
        target.extras[extrasKeyOf<Boolean>(EXTRA_CREATING_DEFAULT_JAVA_SOURCE_NAME)] = true
        // source set should be created before compilation as otherwise Gradle complains about source set configurations
        // that were not created by Gradle itself
        val defaultJavaSourceSet = project.javaSourceSets.maybeCreate(sourceSetsNaming.defaultSourceSetName(target, name))

        return target.project.objects.newInstance(
            KotlinJvmCompilation::class.java,
            compilationImplFactory.create(target, name),
            defaultJavaSourceSet
        ).also {
            target.extras[extrasKeyOf(EXTRA_CREATING_DEFAULT_JAVA_SOURCE_NAME)] = false
        }
    }

    internal companion object {
        internal const val EXTRA_CREATING_DEFAULT_JAVA_SOURCE_NAME = "kotlin.internal.creatingDefaultJavaSourceSet"
    }
}
