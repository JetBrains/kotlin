/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationDetailsImpl

import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationDetailsWithRuntime
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.GradleKpmDependencyFilesHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.newDependencyFilesHolder
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal open class DefaultCompilationDetailsWithRuntime<T : KotlinCommonOptions, CO : CompilerCommonOptions>(
    target: KotlinTarget,
    compilationPurpose: String,
    defaultSourceSet: KotlinSourceSet,
    createCompilerOptions: DefaultCompilationDetails<T, CO>.() -> HasCompilerOptions<CO>,
    createKotlinOptions: DefaultCompilationDetails<T, CO>.() -> T
) : DefaultCompilationDetails<T, CO>(
    target, compilationPurpose, defaultSourceSet, createCompilerOptions, createKotlinOptions
), CompilationDetailsWithRuntime<T> {
    override val runtimeDependencyFilesHolder: GradleKpmDependencyFilesHolder = project.newDependencyFilesHolder(
        lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "runtimeClasspath"
        )
    )
}