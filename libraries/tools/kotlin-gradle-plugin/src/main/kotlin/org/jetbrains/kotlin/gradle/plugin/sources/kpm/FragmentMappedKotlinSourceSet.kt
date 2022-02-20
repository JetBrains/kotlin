/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.kpm

import groovy.lang.Closure
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.unambiguousNameInProject
import org.jetbrains.kotlin.gradle.plugin.sources.createDefaultSourceDirectorySet

class FragmentMappedKotlinSourceSet(
    private val sourceSetName: String,
    private val project: Project,
    internal val underlyingFragment: KotlinGradleFragment
) : KotlinSourceSet {
    val displayName: String
        get() = sourceSetName

    override val apiConfigurationName: String get() = underlyingFragment.apiConfigurationName
    override val implementationConfigurationName: String get() = underlyingFragment.implementationConfigurationName
    override val compileOnlyConfigurationName: String get() = underlyingFragment.compileOnlyConfigurationName
    override val runtimeOnlyConfigurationName: String get() = underlyingFragment.runtimeOnlyConfigurationName

    // FIXME deprecate the property in the supertype?
    override val apiMetadataConfigurationName: String
        get() = error("should not be used with KPM mapped model")
    override val implementationMetadataConfigurationName: String
        get() = error("should not be used with KPM mapped model")
    override val compileOnlyMetadataConfigurationName: String
        get() = error("should not be used with KPM mapped model")
    override val runtimeOnlyMetadataConfigurationName: String
        get() = error("should not be used with KPM mapped model")

    override val kotlin: SourceDirectorySet = underlyingFragment.kotlinSourceRoots

    override val languageSettings: LanguageSettingsBuilder = underlyingFragment.languageSettings

    override val resources: SourceDirectorySet = createDefaultSourceDirectorySet(project, "$name resources")

    override fun kotlin(configureClosure: Closure<Any?>): SourceDirectorySet =
        kotlin.apply { project.configure(this, configureClosure) }

    override fun languageSettings(configureClosure: Closure<Any?>): LanguageSettingsBuilder = languageSettings.apply {
        project.configure(this, configureClosure)
    }

    override fun languageSettings(configure: LanguageSettingsBuilder.() -> Unit): LanguageSettingsBuilder =
        languageSettings.apply { configure(this) }

    override fun getName(): String = displayName

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        underlyingFragment.dependencies(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        underlyingFragment.dependencies(configureClosure)

    override fun dependsOn(other: KotlinSourceSet) {
        if (other !is FragmentMappedKotlinSourceSet) {
            throw InvalidUserDataException("Could set up dependsOn relationship with an unknown source set $other")
        }
        val otherFragment = other.underlyingFragment
        underlyingFragment.refines(otherFragment)
    }

    override val dependsOn: Set<KotlinSourceSet>
        get() = project.kotlinExtension.sourceSets.filter {
            it is FragmentMappedKotlinSourceSet && it.underlyingFragment in underlyingFragment.directRefinesDependencies
        }.toSet()

    override fun toString(): String = "source set $name"

    override val requiresVisibilityOf: Set<KotlinSourceSet>
        get() = emptySet()

    override fun requiresVisibilityOf(other: KotlinSourceSet) {
        throw UnsupportedOperationException("requiresVisibilityOf is not supported for the mapped model")
    }

    // region Ported with copy & paste
    // FIXME move to fragment?
    private val explicitlyAddedCustomSourceFilesExtensions = ArrayList<String>()

    override val customSourceFilesExtensions: Iterable<String>
        get() = Iterable {
            val fromExplicitFilters = kotlin.filter.includes.mapNotNull { pattern ->
                pattern.substringAfterLast('.')
            }
            val merged = (fromExplicitFilters + explicitlyAddedCustomSourceFilesExtensions).filterNot { extension ->
                DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS.any { extension.equals(it, ignoreCase = true) }
                        || extension.any { it == '\\' || it == '/' }
            }.distinct()
            merged.iterator()
        }

    override fun addCustomSourceFilesExtensions(extensions: List<String>) {
        explicitlyAddedCustomSourceFilesExtensions.addAll(extensions)
    }
    //endregion
}

