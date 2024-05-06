/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.*

private const val SWIFT_EXPORT_CLASSPATH = "swiftExportClasspath"
private const val SWIFT_EXPORT_CLASSPATH_RESOLVABLE = "swiftExportClasspathResolvable"
private const val SWIFT_EXPORT_EMBEDDABLE_MODULE = "swift-export-embeddable"


internal fun Project.initSwiftExportClasspathConfigurations() {
    if (project.kotlinPropertiesProvider.swiftExportEnabled) {
        maybeCreateSwiftExportClasspathDependenciesConfiguration()
        maybeCreateSwiftExportClasspathResolvableConfiguration()
    }
}

private fun Project.maybeCreateSwiftExportClasspathDependenciesConfiguration(): Configuration {
    return configurations.findDependencyScope(SWIFT_EXPORT_CLASSPATH)
        ?: project.configurations.createDependencyScope(SWIFT_EXPORT_CLASSPATH) {
            description = "Runtime classpath for the SwiftExport worker."
            defaultDependencies { dependencies ->
                dependencies.add(
                    project.dependencies.create("$KOTLIN_MODULE_GROUP:$SWIFT_EXPORT_EMBEDDABLE_MODULE:${getKotlinPluginVersion()}")
                )
            }
        }.get()
}

internal fun Project.maybeCreateSwiftExportClasspathResolvableConfiguration(): Configuration {
    return configurations.findResolvable(SWIFT_EXPORT_CLASSPATH_RESOLVABLE)
        ?: project.configurations.createResolvable(SWIFT_EXPORT_CLASSPATH_RESOLVABLE) {
            description = "Resolves the runtime classpath for the SwiftExport worker."
            attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attributes.setAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
            attributes.setAttribute(Usage.USAGE_ATTRIBUTE, usageByName(Usage.JAVA_RUNTIME))
            extendsFrom(maybeCreateSwiftExportClasspathDependenciesConfiguration())
        }
}