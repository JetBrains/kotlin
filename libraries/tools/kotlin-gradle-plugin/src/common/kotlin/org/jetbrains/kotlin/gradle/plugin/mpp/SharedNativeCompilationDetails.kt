/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.getMetadataCompilationForSourceSet

internal open class SharedNativeCompilationDetails(
    target: KotlinTarget,
    compilationPurpose: String,
    defaultSourceSet: KotlinSourceSet,
    @Suppress("DEPRECATION")
    createCompilerOptions: DefaultCompilationDetails<KotlinCommonOptions, CompilerCommonOptions>.() -> HasCompilerOptions<CompilerCommonOptions>,
    @Suppress("DEPRECATION")
    createKotlinOptions: DefaultCompilationDetails<KotlinCommonOptions, CompilerCommonOptions>.() -> KotlinCommonOptions
) : DefaultCompilationDetails<KotlinCommonOptions, CompilerCommonOptions>(
    target,
    compilationPurpose,
    defaultSourceSet,
    createCompilerOptions,
    createKotlinOptions
) {

    override val friendArtifacts: FileCollection
        get() = super.friendArtifacts.plus(run {
            val project = target.project
            val friendSourceSets = getVisibleSourceSetsFromAssociateCompilations(defaultSourceSet).toMutableSet().apply {
                // TODO: implement proper dependsOn/refines compiler args for Kotlin/Native and pass the dependsOn klibs separately;
                //       But for now, those dependencies don't have any special semantics, so passing all them as friends works, too
                addAll(defaultSourceSet.internal.dependsOnClosure)
            }
            project.files(friendSourceSets.mapNotNull { project.getMetadataCompilationForSourceSet(it)?.output?.classesDirs })
        })

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
        addSourcesToKotlinNativeCompileTask(project, compileKotlinTaskName, { sourceSet.kotlin }, addAsCommonSources)
    }
}