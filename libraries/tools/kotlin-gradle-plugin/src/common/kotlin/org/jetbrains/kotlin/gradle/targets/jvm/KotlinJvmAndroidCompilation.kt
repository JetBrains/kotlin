/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.getJavaTaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import javax.inject.Inject

abstract class KotlinJvmAndroidCompilation @Inject constructor(
    final override val compilationDetails: AndroidCompilationDetails
) : AbstractKotlinCompilationToRunnableFiles<KotlinJvmOptions>(compilationDetails) {

    val androidVariant: BaseVariant = compilationDetails.androidVariant

    internal val testedVariantArtifacts: Property<FileCollection> =
        compilationDetails.target.project.objects.property(FileCollection::class.java)

    @Suppress("DEPRECATION")
    @Deprecated("Accessing task instance directly is deprecated", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTask: org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        get() = super.compileKotlinTask as org.jetbrains.kotlin.gradle.tasks.KotlinCompile

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    @Deprecated("Replaced with compileTaskProvider", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTaskProvider: TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>
        get() = super.compileKotlinTaskProvider as TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>

    @Suppress("UNCHECKED_CAST")
    override val compileTaskProvider: TaskProvider<out KotlinCompilationTask<KotlinJvmCompilerOptions>>
        get() = super.compileTaskProvider as TaskProvider<KotlinCompilationTask<KotlinJvmCompilerOptions>>

    val compileJavaTaskProvider: TaskProvider<out JavaCompile>
        get() = androidVariant.getJavaTaskProvider()
}