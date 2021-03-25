/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.project.model.LanguageSettings

interface KotlinCompilationData<T : KotlinCommonOptions> {
    val project: Project
    val owner: Any?

    val compilationPurpose: String
    val compilationClassifier: String?

    val kotlinSourceDirectoriesByFragmentName: Map<String, SourceDirectorySet>
    val compileKotlinTaskName: String
    val compileAllTaskName: String

    val compileDependencyFiles: FileCollection
    val output: KotlinCompilationOutput

    val languageSettings: LanguageSettings
    val platformType: KotlinPlatformType

    val moduleName: String
    val ownModuleName: String

    val kotlinOptions: T

    val friendPaths: Iterable<FileCollection>
}

interface KotlinVariantCompilationData<T : KotlinCommonOptions> : KotlinCompilationData<T> {
    override val owner: KotlinGradleVariant

    override val project: Project get() = owner.containingModule.project

    override val compilationPurpose: String
        get() = owner.containingModule.name

    override val compilationClassifier: String
        get() = owner.name

    override val output: KotlinCompilationOutput
        get() = owner.compilationOutputs

    override val compileKotlinTaskName: String

    override val compileAllTaskName: String

    override val kotlinSourceDirectoriesByFragmentName: Map<String, SourceDirectorySet>

    override val compileDependencyFiles: FileCollection
        get() = owner.compileDependencyFiles

    override val languageSettings: LanguageSettings
        get() = owner.languageSettings

    override val platformType: KotlinPlatformType
        get() = owner.platformType

    override val ownModuleName: String
}