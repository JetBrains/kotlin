/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.Named
import org.gradle.api.file.SourceDirectorySet

interface KotlinSourceSet : Named, HasKotlinDependencies {
    val kotlin: SourceDirectorySet
    fun kotlin(configureClosure: Closure<Any?>): SourceDirectorySet

    val resources: SourceDirectorySet

    val languageSettings: LanguageSettingsBuilder
    fun languageSettings(configure: LanguageSettingsBuilder.() -> Unit): LanguageSettingsBuilder
    fun languageSettings(configureClosure: Closure<Any?>): LanguageSettingsBuilder

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
