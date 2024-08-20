/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.plugin.GradlePluginApiVersion
import org.gradle.util.GradleVersion

fun Project.addExtendsFromRelation(extendingConfigurationName: String, extendsFromConfigurationName: String, forced: Boolean = true) {
    if (extendingConfigurationName == extendsFromConfigurationName) return

    val extending = configurations.findByName(extendingConfigurationName)
        ?: if (forced) throw RuntimeException("Configuration $extendingConfigurationName does not exist.")
        else return

    extending.extendsFrom(configurations.getByName(extendsFromConfigurationName))
}

fun NamedDomainObjectProvider<Configuration>.extendsFrom(other: NamedDomainObjectProvider<Configuration>) {
    if (name == other.name) return
    configure { extending -> extending.extendsFrom(other.get()) }
}

internal fun Configuration.addGradlePluginMetadataAttributes(
    project: Project,
) {
    attributes {
        it.setAttribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
        it.setAttribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        it.setAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.JAR))
        it.setAttribute(
            GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
            project.objects.named(GradleVersion.current().version)
        )
    }
}

/**
 * Extends the dependencies of this configuration by adding the specified [configurations]'s dependencies only.
 *
 * @param project The project on which the configuration is applied.
 * @param configurations The configurations whose projects' dependencies will be added to this configuration.
 */
internal fun Configuration.copyDependenciesLazy(project: Project, vararg configurations: Configuration) {
    return dependencies.addAllLater(project.listProvider {
        configurations.flatMap { it.allDependencies }
    })
}