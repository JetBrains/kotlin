/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain

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

    val languageSettings: LanguageSettingsBuilder
    val platformType: KotlinPlatformType

    val moduleName: String
    val ownModuleName: String

    val kotlinOptions: T

    val friendPaths: Iterable<FileCollection>
}

fun KotlinCompilationData<*>.isMain(): Boolean = when (this) {
    is KotlinCompilation<*> -> isMain()
    else -> compilationPurpose == KotlinGradleModule.MAIN_MODULE_NAME
}