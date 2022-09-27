/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationDetailsImpl

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.CompilerMultiplatformCommonOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerMultiplatformCommonOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations

internal class MetadataCompilationDetails(
    target: KotlinTarget,
    name: String,
    defaultSourceSet: KotlinSourceSet,
) : DefaultCompilationDetails<KotlinMultiplatformCommonOptions, CompilerMultiplatformCommonOptions>(
    target,
    name,
    defaultSourceSet,
    {
        object : HasCompilerOptions<CompilerMultiplatformCommonOptions> {
            override val options: CompilerMultiplatformCommonOptions =
                target.project.objects.newInstance(CompilerMultiplatformCommonOptionsDefault::class.java)
        }
    },
    {
        object : KotlinMultiplatformCommonOptions {
            override val options: CompilerMultiplatformCommonOptions
                get() = compilerOptions.options
        }
    }
) {

    override val friendArtifacts: FileCollection
        get() = super.friendArtifacts.plus(run {
            val project = target.project
            val friendSourceSets = getVisibleSourceSetsFromAssociateCompilations(defaultSourceSet)
            project.files(friendSourceSets.mapNotNull { target.compilations.findByName(it.name)?.output?.classesDirs })
        })
}