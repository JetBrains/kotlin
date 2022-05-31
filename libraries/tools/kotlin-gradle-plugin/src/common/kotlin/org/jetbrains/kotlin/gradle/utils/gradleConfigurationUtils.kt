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
import java.lang.RuntimeException

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
    project: Project
) {
    attributes {
        it.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.JAR))
        if (GradleVersion.current() >= GradleVersion.version("7.0")) {
            it.attribute(
                GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                project.objects.named(GradlePluginApiVersion::class.java, GradleVersion.current().version)
            )
        }
    }
}
