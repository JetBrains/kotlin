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
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmAbstractFragmentMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.plugin.sources.dependsOnClosure
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.toSetOrEmpty
import org.jetbrains.kotlin.project.model.LanguageSettings

internal sealed class KotlinCompilationInfo {
    abstract val origin: Any
    abstract val project: Project
    abstract val platformType: KotlinPlatformType
    abstract val targetDisambiguationClassifier: String?
    abstract val compilationName: String
    abstract val moduleName: String
    abstract val compilerOptions: HasCompilerOptions<*>
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

        override val compilerOptions: HasCompilerOptions<*>
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

    class KPM(val compilationData: GradleKpmCompilationData<*>) : KotlinCompilationInfo() {

        override val origin: GradleKpmCompilationData<*> = compilationData

        override val project: Project
            get() = origin.project

        override val platformType: KotlinPlatformType
            get() = origin.platformType

        override val targetDisambiguationClassifier: String?
            get() = origin.compilationClassifier

        override val compilationName: String
            get() = origin.compilationPurpose

        override val moduleName: String
            get() = origin.moduleName

        override val compilerOptions: HasCompilerOptions<*>
            get() = origin.compilerOptions

        override val compileKotlinTaskName: String
            get() = origin.compileKotlinTaskName

        override val compileAllTaskName: String
            get() = origin.compileAllTaskName

        override val languageSettings: LanguageSettings
            get() = origin.languageSettings

        override val friendPaths: FileCollection
            get() = project.filesProvider { origin.friendPaths }

        override val refinesPaths: FileCollection
            get() = project.filesProvider files@{
                val compilationData = origin as? GradleKpmAbstractFragmentMetadataCompilationData<*> ?: return@files emptyList<Any>()
                val fragment = compilationData.fragment

                fragment.refinesClosure.minus(fragment).map {
                    val compilation = compilationData.metadataCompilationRegistry.getForFragmentOrNull(it) ?: return@map project.files()
                    compilation.output.classesDirs
                }
            }

        override val isMain: Boolean
            get() = origin.compilationPurpose == GradleKpmModule.MAIN_MODULE_NAME

        override val classesDirs: ConfigurableFileCollection
            get() = origin.output.classesDirs

        override val compileDependencyFiles: FileCollection
            get() = project.filesProvider { origin.compileDependencyFiles }

        override val sources: List<SourceDirectorySet>
            get() = origin.kotlinSourceDirectoriesByFragmentName.values.toList()

        override val displayName: String
            get() = origin.toString()

        override fun toString(): String {
            return displayName
        }
    }
}

internal fun KotlinCompilationInfo(compilation: KotlinCompilation<*>): KotlinCompilationInfo.TCS {
    return KotlinCompilationInfo.TCS(compilation)
}

internal val KotlinCompilationInfo.tcsOrNull: KotlinCompilationInfo.TCS?
    get() = when (this) {
        is KotlinCompilationInfo.KPM -> null
        is KotlinCompilationInfo.TCS -> this
    }

internal val KotlinCompilationInfo.tcs: KotlinCompilationInfo.TCS
    get() = this as KotlinCompilationInfo.TCS

internal val KotlinCompilationInfo.kpmOrNull: KotlinCompilationInfo.KPM?
    get() = when (this) {
        is KotlinCompilationInfo.KPM -> this
        is KotlinCompilationInfo.TCS -> null
    }
