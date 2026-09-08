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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.createFullyExportedSwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.createTransitiveSwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.validateSwiftExportModuleName
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import java.io.File

private const val XCODE_INTEGRATION_CONFIGURE_DSL = "export { swift { xcodeIntegration { configure() } } }"

/**
 * Applies the overrides from `xcodeIntegration { configure(dependency) { } }` to the modules collected from the
 * export graph, then reports overrides that matched nothing.
 *
 * Runs on the collected modules rather than inside the collector, so the legacy `swiftExport { }` flow is not
 * involved: the two DSLs cannot be combined, so an override can only ever come from the `export { }` DSL.
 */
internal fun Project.applySwiftExportConsumerOverrides(
    modules: Provider<List<SwiftExportedModule>>,
    overrides: Provider<Map<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>>,
    exportConfiguration: Provider<LazyResolvedConfigurationWithArtifacts>,
    apiConfiguration: Provider<LazyResolvedConfigurationWithArtifacts?>,
): Provider<List<SwiftExportedModule>> = provider {
    val overridesMap = overrides.get()
    // Declared option layers, highest precedence first. KT-87987 adds the producer source here.
    val sources = listOf(ConsumerOverridesOptionsSource(overridesMap))

    val componentByArtifact = componentByArtifact(exportConfiguration.get(), apiConfiguration.orNull)
    val exported = modules.get().map { module ->
        val component = componentByArtifact[module.artifact] ?: return@map module to null
        val declaredName = sources.declaredModuleName(component)?.also { validateSwiftExportModuleName(it) }
        val adjusted = when {
            module.shouldBeFullyExported -> createFullyExportedSwiftExportedModule(
                moduleName = declaredName ?: module.moduleName,
                flattenPackage = sources.declaredRootPackage(component) ?: module.flattenPackage,
                artifact = module.artifact,
            )
            // A transitively exported module has no root package, so a declared one has no effect here.
            else -> createTransitiveSwiftExportedModule(
                moduleName = declaredName ?: module.moduleName,
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

    exported.map { (module, _) -> module }
}

/**
 * The graph node each artifact came from. The first node reaching an artifact wins, which is also the order the
 * collector derives module names in.
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
