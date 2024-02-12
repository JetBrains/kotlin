/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.sources.dependsOnClosure
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.project.model.LanguageSettings

internal sealed class KotlinCompilationInfo {
    abstract val origin: Any
    abstract val project: Project
    abstract val platformType: KotlinPlatformType
    abstract val targetDisambiguationClassifier: String?
    abstract val compilationName: String
    abstract val moduleName: String
    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
    abstract val compilerOptions: DeprecatedHasCompilerOptions<*>
    abstract val compileKotlinTaskName: String
    abstract val compileAllTaskName: String
    abstract val languageSettings: LanguageSettings
    abstract val friendPaths: FileCollection
    abstract val refinesPaths: FileCollection
    abstract val isMain: Boolean
    abstract val classesDirs: ConfigurableFileCollection
    abstract val compileDependencyFiles: FileCollection
    abstract val sources: List<SourceDirectorySet>
    abstract val displayName: String

    class TCS(val compilation: KotlinCompilation<*>) : KotlinCompilationInfo() {

        override val origin: KotlinCompilation<*> = compilation

        override val project: Project
            get() = origin.project

        override val platformType: KotlinPlatformType
            get() = origin.platformType

        override val targetDisambiguationClassifier: String?
            get() = origin.target.disambiguationClassifier

        override val compilationName: String
            get() = origin.compilationName

        override val moduleName: String
            get() = origin.moduleNameForCompilation().get()

        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION", "DEPRECATION")
        override val compilerOptions: DeprecatedHasCompilerOptions<*>
            get() = origin.compilerOptions

        override val compileKotlinTaskName: String
            get() = origin.compileKotlinTaskName

        override val compileAllTaskName: String
            get() = origin.compileAllTaskName

        override val languageSettings: LanguageSettings
            get() = origin.defaultSourceSet.languageSettings

        override val friendPaths: FileCollection
            get() = project.filesProvider { origin.internal.friendPaths }

        override val refinesPaths: FileCollection
            get() = project.filesProvider files@{
                val metadataTarget = origin.target as? KotlinMetadataTarget ?: return@files emptyList<Any>()
                origin.kotlinSourceSets.dependsOnClosure
                    .mapNotNull { sourceSet -> metadataTarget.compilations.findByName(sourceSet.name)?.output?.classesDirs }
            }

        override val isMain: Boolean
            get() = origin.isMain()

        override val classesDirs: ConfigurableFileCollection
            get() = origin.output.classesDirs

        override val compileDependencyFiles: FileCollection
            get() = project.filesProvider { origin.compileDependencyFiles }

        override val sources: List<SourceDirectorySet>
            get() = origin.allKotlinSourceSets.map { it.kotlin }

        override val displayName: String
            get() = "compilation '${compilation.name}' in target '${compilation.target.name}'"

        override fun toString(): String {
            return displayName
        }
    }
}

internal fun KotlinCompilationInfo(compilation: KotlinCompilation<*>): KotlinCompilationInfo.TCS {
    return KotlinCompilationInfo.TCS(compilation)
}

internal val KotlinCompilationInfo.tcs: KotlinCompilationInfo.TCS
    get() = this as KotlinCompilationInfo.TCS
