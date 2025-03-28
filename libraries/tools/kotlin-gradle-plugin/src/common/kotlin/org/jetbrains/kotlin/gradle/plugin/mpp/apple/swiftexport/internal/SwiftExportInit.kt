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
import org.jetbrains.kotlin.gradle.internal.attributes.setAttributeTo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.utils.*

private const val SWIFT_EXPORT_CLASSPATH = "swiftExportClasspath"
private const val SWIFT_EXPORT_CLASSPATH_RESOLVABLE = "swiftExportClasspathResolvable"
private const val SWIFT_EXPORT_EMBEDDABLE_MODULE = "swift-export-embeddable"
internal const val SWIFT_EXPORT_MODULE_NAME_PATTERN = "^[A-Za-z0-9_]+$"

internal fun Project.initSwiftExportClasspathConfigurations() {
    maybeCreateSwiftExportClasspathDependenciesConfiguration()
    SwiftExportClasspathResolvableConfiguration
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

internal val Project.SwiftExportClasspathResolvableConfiguration: Configuration
    get() = configurations.maybeCreateResolvable(SWIFT_EXPORT_CLASSPATH_RESOLVABLE) {
        description = "Resolves the runtime classpath for the SwiftExport worker."
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(Usage.JAVA_RUNTIME))
        extendsFrom(maybeCreateSwiftExportClasspathDependenciesConfiguration())
    }

internal fun KotlinNativeTarget.exportedSwiftExportApiConfigurationName(buildType: NativeBuildType): String = disambiguateName(
    lowerCamelCaseName(buildType.configuration, "exported", "swift", "export", "api", "configuration")
)

internal fun KotlinNativeTarget.exportedSwiftExportApiConfiguration(
    buildType: NativeBuildType,
    extendConfiguration: Configuration
): Configuration =
    project.configurations.maybeCreateResolvable(exportedSwiftExportApiConfigurationName(buildType)) {
        description = "Swift Export dependencies configuration for $name"
        isVisible = false
        extendsFrom(extendConfiguration)
        shouldResolveConsistentlyWith(extendConfiguration)
        usesPlatformOf(this@exportedSwiftExportApiConfiguration)
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(KotlinUsages.KOTLIN_API))
        if (project.kotlinPropertiesProvider.useNonPackedKlibs) {
            KlibPackaging.setAttributeTo(project, attributes, false)
        }
    }

internal val String.normalizedSwiftExportModuleName get() = dashSeparatedToUpperCamelCase(this)

internal fun Project.validateSwiftExportModuleName(moduleName: String) =
    kotlinToolingDiagnosticsCollector.validateSwiftExportModuleName(this, moduleName)

internal fun KotlinToolingDiagnosticsCollector.validateSwiftExportModuleName(project: Project, moduleName: String) {
    if (!moduleName.matches(Regex(SWIFT_EXPORT_MODULE_NAME_PATTERN))) {
        report(project, KotlinToolingDiagnostics.SwiftExportInvalidModuleName(moduleName))
    }
}