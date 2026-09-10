/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.export.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedModuleMode
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.createFullyExportedSwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.createHiddenSwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.createTransitiveSwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.defaultSwiftExportModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.normalizedSwiftExportModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.validateSwiftExportModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportVisibility
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import java.io.File

private const val XCODE_INTEGRATION_CONFIGURE_DSL = "export { swift { xcodeIntegration { configure() } } }"
private const val EXPORTED_MODULE_ITSELF = "the module being exported"

/**
 * Applies the overrides from `xcodeIntegration { configure(dependency) { } }` to the collected modules, then
 * reports overrides that matched nothing and module names shared by more than one module.
 *
 * Works on the collector's output, so the legacy `swiftExport { }` flow never runs through here.
 *
 * @param rootModuleName the Swift module name of the module being exported, for collision detection
 */
internal fun Project.applySwiftExportConsumerOverrides(
    modules: Provider<List<SwiftExportedModule>>,
    overrides: Provider<Map<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>>,
    exportConfiguration: Provider<LazyResolvedConfigurationWithArtifacts>,
    apiConfiguration: Provider<LazyResolvedConfigurationWithArtifacts?>,
    rootModuleName: Provider<String>,
): Provider<List<SwiftExportedModule>> = provider {
    val overridesMap = overrides.get()
    if (overridesMap.isEmpty()) return@provider modules.get()

    // Declared option layers, highest precedence first. KT-87987 adds the producer source here.
    val sources = listOf(ConsumerOverridesOptionsSource(overridesMap))

    val componentByArtifact = componentByArtifact(exportConfiguration.get(), apiConfiguration.orNull)
    val exported = modules.get().map { module ->
        val component = componentByArtifact[module.artifact] ?: return@map module to null
        val declaredName = sources.declaredModuleName(component)?.also { validateSwiftExportModuleName(it) }
        val visibility = sources.declaredVisibility(component)
        val mode = when (visibility) {
            SwiftExportVisibility.EXPOSED -> SwiftExportedModuleMode.FULL
            SwiftExportVisibility.HIDDEN -> SwiftExportedModuleMode.HIDDEN
            null -> module.exportMode
        }
        // The collector names transitive modules from coordinates and fully exported ones from the component.
        // With a declared visibility use the latter, so the name matches `api(project(...))` regardless of scope.
        val derivedName =
            if (visibility == null) module.moduleName else (component.defaultModuleName() ?: module.moduleName)
        val adjusted = when (mode) {
            SwiftExportedModuleMode.FULL -> createFullyExportedSwiftExportedModule(
                moduleName = declaredName ?: derivedName,
                flattenPackage = sources.declaredRootPackage(component) ?: module.flattenPackage,
                artifact = module.artifact,
            )
            // A transitively exported module has no root package, so a declared one has no effect here.
            SwiftExportedModuleMode.TRANSITIVE -> createTransitiveSwiftExportedModule(
                moduleName = declaredName ?: derivedName,
                artifact = module.artifact,
            )
            // The stub module is emitted under this name. A root package makes no sense for it, as for transitive.
            SwiftExportedModuleMode.HIDDEN -> createHiddenSwiftExportedModule(
                moduleName = declaredName ?: derivedName,
                artifact = module.artifact,
            )
        }
        adjusted to component
    }

    val components = exported.mapNotNull { (_, component) -> component }
    val unmatched = overridesMap.keys.filterNot { selector -> components.any(selector::matches) }
    if (unmatched.isNotEmpty()) {
        reportDiagnostic(
            KotlinToolingDiagnostics.SwiftExportModuleResolutionError(
                unmatched.map { it.displayName },
                XCODE_INTEGRATION_CONFIGURE_DSL,
            )
        )
    }

    val owners = listOf(rootModuleName.get() to EXPORTED_MODULE_ITSELF) +
            exported.map { (module, component) -> module.moduleName to (component?.displayName ?: module.artifact.name) }
    // Ignoring case: the output directories are named after the modules, and the macOS file system is
    // case-insensitive by default.
    val duplicates = owners.groupBy { (name, _) -> name.lowercase() }
        .values
        .filter { it.size > 1 }
        .associate { group -> group.map { (name, _) -> name }.distinct().joinToString("/") to group.map { (_, owner) -> owner } }
    if (duplicates.isNotEmpty()) {
        reportDiagnostic(KotlinToolingDiagnostics.SwiftExportDuplicateModuleNames(duplicates))
    }

    exported.map { (module, _) -> module }
}

/**
 * The graph node each artifact came from. First node wins, same as in the collector.
 */
private fun componentByArtifact(
    exportConfiguration: LazyResolvedConfigurationWithArtifacts,
    apiConfiguration: LazyResolvedConfigurationWithArtifacts?,
): Map<File, SwiftExportResolvedComponent> {
    val result = LinkedHashMap<File, SwiftExportResolvedComponent>()
    fun collect(configuration: LazyResolvedConfigurationWithArtifacts, dependencies: Iterable<ResolvedDependencyResult>) {
        for (dependency in dependencies) {
            for (artifact in configuration.getArtifacts(dependency.selected)) {
                result.putIfAbsent(
                    artifact.file,
                    SwiftExportResolvedComponent(artifact.id.componentIdentifier, dependency.selected.moduleVersion),
                )
            }
        }
    }
    collect(exportConfiguration, exportConfiguration.allResolvedDependencies)
    if (apiConfiguration != null) {
        collect(apiConfiguration, apiConfiguration.root.dependencies.filterIsInstance<ResolvedDependencyResult>())
    }
    return result
}

/** What the collector would name this component if it were fully exported, `null` if that can't be derived. */
private fun SwiftExportResolvedComponent.defaultModuleName(): String? =
    defaultSwiftExportModuleName(id, moduleVersion)?.normalizedSwiftExportModuleName
