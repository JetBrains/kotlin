/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.SourceDirectorySet

interface KotlinSourceSet : Named, HasKotlinDependencies {
    val kotlin: SourceDirectorySet

    fun kotlin(configure: SourceDirectorySet.() -> Unit): SourceDirectorySet

    fun kotlin(configure: Action<SourceDirectorySet>): SourceDirectorySet

    val resources: SourceDirectorySet

    val languageSettings: LanguageSettingsBuilder
    fun languageSettings(configure: LanguageSettingsBuilder.() -> Unit): LanguageSettingsBuilder
    fun languageSettings(configure: Action<LanguageSettingsBuilder>): LanguageSettingsBuilder

    fun dependsOn(other: KotlinSourceSet)
    val dependsOn: Set<KotlinSourceSet>

    val apiMetadataConfigurationName: String
    val implementationMetadataConfigurationName: String
    val compileOnlyMetadataConfigurationName: String
    val runtimeOnlyMetadataConfigurationName: String

    override val relatedConfigurationNames: List<String>
        get() = super.relatedConfigurationNames +
                listOf(
                    apiMetadataConfigurationName,
                    implementationMetadataConfigurationName,
                    compileOnlyMetadataConfigurationName,
                    runtimeOnlyMetadataConfigurationName
                )

    companion object {
        const val COMMON_MAIN_SOURCE_SET_NAME = "commonMain"
        const val COMMON_TEST_SOURCE_SET_NAME = "commonTest"
    }

    val customSourceFilesExtensions: Iterable<String> // lazy iterable expected

    val requiresVisibilityOf: Set<KotlinSourceSet>
    fun requiresVisibilityOf(other: KotlinSourceSet)

    fun addCustomSourceFilesExtensions(extensions: List<String>) {}
}
