/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import javax.inject.Inject

interface KotlinMetadataCompilation<T : KotlinCommonOptions> : KotlinCompilation<T>

open class KotlinCommonCompilation @Inject internal constructor(compilation: KotlinCompilationImpl) :
    @Suppress("DEPRECATION") AbstractKotlinCompilation<KotlinCommonOptions>(compilation),
    KotlinMetadataCompilation<KotlinCommonOptions> {
    @Suppress("DEPRECATION")
    @Deprecated("Accessing task instance directly is deprecated", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTask: KotlinCompileCommon
        get() = compilation.compileKotlinTask as KotlinCompileCommon

    @Suppress("UNCHECKED_CAST")
    override val compileTaskProvider: TaskProvider<KotlinCompilationTask<KotlinMultiplatformCommonCompilerOptions>>
        get() = compilation.compileTaskProvider as TaskProvider<KotlinCompilationTask<KotlinMultiplatformCommonCompilerOptions>>

    internal val isKlibCompilation: Boolean
        get() = target.project.isKotlinGranularMetadataEnabled && !forceCompilationToKotlinMetadata

    internal var forceCompilationToKotlinMetadata: Boolean = false
}

