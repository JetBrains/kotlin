/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApi
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

interface KotlinSourceSet : Named, HasProject, HasMutableExtras, HasKotlinDependencies {
    val kotlin: SourceDirectorySet

    fun kotlin(configure: SourceDirectorySet.() -> Unit): SourceDirectorySet

    fun kotlin(configure: Action<SourceDirectorySet>): SourceDirectorySet

    val resources: SourceDirectorySet

    val languageSettings: LanguageSettingsBuilder
    fun languageSettings(configure: LanguageSettingsBuilder.() -> Unit): LanguageSettingsBuilder
    fun languageSettings(configure: Action<LanguageSettingsBuilder>): LanguageSettingsBuilder

    /**
     * ### ⚠️ This API is marked as [DelicateKotlinGradlePluginApi]
     * It is preferred to express target hierarchies using the Kotlin Target Hierarchy DSL:
     * #### Example:
     * ```kotlin
     * kotlin {
     *     targetHierarchy.default()
     *     targetHierarchy.default { /* describe my extension */ }
     * }
     * ```
     *
     *
     * This API will mark this [KotlinSourceSet] as 'dependsOn' to the [other] Source Set:
     * #### Example: Lets consider creating an intermediate 'nativeMain' Source set, which can provide actuals for 'commonMain' and
     * expects for 'iosMain'
     *
     * ```kotlin
     * kotlin {
     *     // Step 1: Get reference to the Source Sets:
     *     val commonMain = sourceSets.commonMain.get()
     *     val nativeMain = sourceSets.nativeMain.get()
     *     val iosMain = sourceSets.iosMain.get()
     *
     *     // Step 2: Add 'dependsOn' edge from 'nativeMain' to 'commonMain'
     *     nativeMain.dependsOn(commonMain)
     *
     *     // Step 3: Add 'dependsOn' edge from 'iosMain' to 'nativeMain'
     *     iosMain.dependsOn(nativeMain)
     * }
     * ```
     */
    @DelicateKotlinGradlePluginApi
    fun dependsOn(other: KotlinSourceSet)

    val dependsOn: Set<KotlinSourceSet>

    @Deprecated(message = "KT-55312")
    val apiMetadataConfigurationName: String

    @Deprecated(message = "KT-55312")
    val implementationMetadataConfigurationName: String

    @Deprecated(message = "KT-55312")
    val compileOnlyMetadataConfigurationName: String

    @Deprecated(message = "KT-55230: RuntimeOnly scope is not supported for metadata dependency transformation")
    val runtimeOnlyMetadataConfigurationName: String

    companion object {
        const val COMMON_MAIN_SOURCE_SET_NAME = "commonMain"
        const val COMMON_TEST_SOURCE_SET_NAME = "commonTest"
    }

    @Deprecated("Scheduled for removal with Kotlin 2.0")
    val requiresVisibilityOf: Set<KotlinSourceSet>

    @Deprecated("Scheduled for removal with Kotlin 2.0")
    fun requiresVisibilityOf(other: KotlinSourceSet)

    val customSourceFilesExtensions: Iterable<String> // lazy iterable expected
    fun addCustomSourceFilesExtensions(extensions: List<String>) {}
}
