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
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsSeverity.FATAL
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.toolingDiagnosticsContext
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.usesPlatformKlibsOf
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

private const val SWIFT_EXPORT_CLASSPATH = "swiftExportClasspath"
private const val SWIFT_EXPORT_CLASSPATH_RESOLVABLE = "swiftExportClasspathResolvable"
private const val SWIFT_EXPORT_EMBEDDABLE_MODULE = "swift-export-embeddable"
internal const val SWIFT_EXPORT_MODULE_NAME_VALIDATION_PATTERN = "^[A-Za-z_][A-Za-z0-9_]*$"
internal const val SWIFT_EXPORT_MODULE_NAME_DELIMITERS_PATTERN = "[:.\\-]+"
private val swiftExportReservedModuleNames = setOf(
    "ExportedKotlinPackages",
    "KotlinRuntimeSupport",
    "KotlinRuntime",
    "KotlinCoroutineSupport",
    "KotlinStdlib",
)
private val swiftExportModuleNameDelimiters = Regex(SWIFT_EXPORT_MODULE_NAME_DELIMITERS_PATTERN)

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
    extendConfiguration: Configuration,
): Configuration =
    project.configurations.maybeCreateResolvable(exportedSwiftExportApiConfigurationName(buildType)) {
        setInvisibleIfSupported()
        description = "Swift Export dependencies configuration for $name"
        extendsFrom(extendConfiguration)
        shouldResolveConsistentlyWith(extendConfiguration)
        usesPlatformKlibsOf(this@exportedSwiftExportApiConfiguration)
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(KotlinUsages.KOTLIN_API))
        KlibPackaging.setAttributeTo(project, attributes, false)
    }

/**
 * Converts a string to a Swift-compatible module name in UpperCamelCase.
 * Colons, dots and dashes are treated as word separators.
 * For example, ":shared:core-api" becomes "SharedCoreApi" and "kotlinx.cli" becomes "KotlinxCli".
 */
internal val String.normalizedSwiftExportModuleName: String
    get() {
        return this.split(swiftExportModuleNameDelimiters)
            // Filter out empty parts that can result from leading/trailing delimiters
            // (e.g., ":api" splits into "" and "api").
            .filter { it.isNotEmpty() }
            .joinToString("") { it.capitalizeAsciiOnly() }
    }

/**
 * Reports the invalid-identifier diagnostic to the project's collector during configuration.
 * See [validateSwiftExportModuleIdentifier] for when a module name needs this validation.
 *
 * @param module the project path or coordinates of the module the name is for
 * @param dsl where to provide a name instead of the one generated from [module], `null` if the name was provided
 */
internal fun Project.validateSwiftExportModuleIdentifier(moduleName: String, module: String, dsl: String? = null) =
    kotlinToolingDiagnosticsCollector.validateSwiftExportModuleIdentifier(this, moduleName, module, dsl)

/**
 * Reports the reserved-name diagnostic to the project's collector during configuration.
 * See [validateSwiftExportModuleName] for when a module name needs this validation.
 */
internal fun Project.validateSwiftExportModuleName(moduleName: String, module: String) =
    kotlinToolingDiagnosticsCollector.validateSwiftExportModuleName(this, moduleName, module)

/**
 * Validates that a Swift module name is a valid identifier, so it can be used as the name of the generated
 * Swift module. Reported as FATAL: at that point an ERROR would only be logged and the build would go on
 * with an invalid name.
 *
 * Call this as soon as a module name becomes known, be it declared by the user in the DSL or derived from
 * a project path or dependency coordinates, so an invalid name is reported together with the DSL to fix it
 * in. Final module names handed to Swift Export do not need this validation: each of them either passed
 * through it when it was declared, or was generated by [normalizedSwiftExportModuleName], which only
 * produces valid identifiers.
 *
 * @param module the project path or coordinates of the module the name is for
 * @param dsl where to provide a name instead of the one generated from [module], `null` if the name was provided
 */
internal fun validateSwiftExportModuleIdentifier(
    moduleName: String,
    module: String,
    dsl: String? = null,
    reportDiagnostic: (ToolingDiagnostic) -> Unit,
) {
    if (!moduleName.matches(Regex(SWIFT_EXPORT_MODULE_NAME_VALIDATION_PATTERN))) {
        reportDiagnostic(KotlinToolingDiagnostics.SwiftExportInvalidModuleName(moduleName, module, dsl, FATAL))
    }
}

/**
 * Validates that a Swift module name is not reserved by Swift Export for the support modules it generates
 * itself, such as `KotlinRuntime`. Reported as FATAL: at that point an ERROR would only be logged and the
 * build would go on with a name that clashes with a generated module.
 *
 * Call this exactly once for every final module name handed to Swift Export: the root module's name and
 * every dependency's name, after consumer overrides are applied. Do not call this for names that can still
 * change: a dependency may inherit a reserved name that the consumer overrides with a safe one, and rejecting
 * the inherited name would fail an otherwise valid build. Names the user declares are also checked during
 * configuration, so a reserved name is reported as soon as it is declared.
 *
 * @param integrationDsl the DSL block of the integration that runs the export, so the diagnostic points the
 * user at that integration's `configure` block; `null` when no single integration is in play
 */
internal fun validateSwiftExportModuleName(
    moduleName: String,
    module: String,
    integrationDsl: String? = null,
    reportDiagnostic: (ToolingDiagnostic) -> Unit,
) {
    if (swiftExportReservedModuleNames.any { moduleName.equals(it, ignoreCase = true) }) {
        reportDiagnostic(KotlinToolingDiagnostics.SwiftExportReservedModuleName(moduleName, module, integrationDsl, FATAL))
    }
}

/**
 * Configuration-time counterpart of [validateSwiftExportModuleIdentifier]: reports the diagnostic through
 * the collector as ERROR, which the user sees before anything runs with the name.
 */
internal fun KotlinToolingDiagnosticsCollector.validateSwiftExportModuleIdentifier(
    project: Project,
    moduleName: String,
    module: String,
    dsl: String? = null,
) {
    if (!moduleName.matches(Regex(SWIFT_EXPORT_MODULE_NAME_VALIDATION_PATTERN))) {
        report(project.toolingDiagnosticsContext, KotlinToolingDiagnostics.SwiftExportInvalidModuleName(moduleName, module, dsl))
    }
}

/**
 * Configuration-time counterpart of [validateSwiftExportModuleName]: reports the diagnostic through the
 * collector as ERROR, which the user sees before anything runs with the name.
 */
internal fun KotlinToolingDiagnosticsCollector.validateSwiftExportModuleName(
    project: Project,
    moduleName: String,
    module: String,
) {
    if (swiftExportReservedModuleNames.any { moduleName.equals(it, ignoreCase = true) }) {
        report(project.toolingDiagnosticsContext, KotlinToolingDiagnostics.SwiftExportReservedModuleName(moduleName, module))
    }
}
