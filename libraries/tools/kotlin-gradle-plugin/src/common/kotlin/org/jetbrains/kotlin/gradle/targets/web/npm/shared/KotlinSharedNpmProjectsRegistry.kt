/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerProject
import org.jetbrains.kotlin.gradle.targets.web.KotlinWebPlatformType
import org.jetbrains.kotlin.gradle.targets.web.WebIsolatedProjectsSupport
import org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin
import org.jetbrains.kotlin.gradle.targets.web.webIsolatedProjectsSupport
import org.jetbrains.kotlin.gradle.utils.isProjectIsolationEnabled
import org.jetbrains.kotlin.gradle.utils.projectCompat
import java.util.concurrent.atomic.AtomicReference

/**
 * Records the paths of the projects with JS or WasmJS targets,
 * so the root project can collect their npm dependency reports without accessing other projects.
 *
 * With Isolated Projects the root project is configured before the other projects,
 * so the root project must only read the registry lazily, when its npm dependency reports are resolved.
 *
 * The `org.jetbrains.kotlin.ecosystem` settings plugin registers the registry when the shared npm project is enabled,
 * see [isKotlinSharedNpmProjectEnabled].
 * Without the settings plugin, KGP registers it as a fallback, see [Project.sharedNpmProjectsRegistry].
 *
 * This is a configuration-time only service. With the configuration cache, tasks get a fresh, empty instance at execution time.
 * That is why the registered projects can only be read through [addRegisteredProjects], which adds them to a configuration:
 * the configuration cache stores the resolved configuration, so the registry is never read at execution time.
 */
internal abstract class KotlinSharedNpmProjectsRegistry : BuildService<BuildServiceParameters.None> {

    /**
     * The registered projects, keyed by project path, with the platforms of their targets.
     */
    private sealed interface State {
        /** Projects can still register. */
        data class Collecting(
            val projects: Map<String, Set<KotlinWebPlatformType>> = mapOf(),
        ) : State

        /**
         * The registered projects were read. Projects that register now are not collected.
         *
         * Sealed for all platforms on the first read of any platform.
         * With Isolated Projects, all projects are configured before any shared npm dependencies are resolved,
         * so tracking each platform separately is not needed.
         */
        data class Sealed(
            val projects: Map<String, Set<KotlinWebPlatformType>>,
        ) : State
    }

    private val state: AtomicReference<State> = AtomicReference(State.Collecting())

    /**
     * Register the project at [projectPath] as having a [platform] target.
     *
     * @return `false` if the registered projects were already read, so this project is not collected.
     */
    fun register(projectPath: String, platform: KotlinWebPlatformType): Boolean {
        val updated = state.updateAndGet { current ->
            requireNotNull(current) { "KotlinSharedNpmProjectsRegistry state is null." }
            when (current) {
                is State.Collecting -> {
                    val platforms = current.projects.getOrElse(projectPath) { setOf() }
                    current.copy(
                        projects = current.projects.plus(projectPath to platforms.plus(platform))
                    )
                }
                is State.Sealed -> {
                    current
                }
            }
        }

        return updated is State.Collecting
    }

    /**
     * The paths of the projects registered with a [platform] target. Seals the registry.
     *
     * Must only be called by [Companion.addRegisteredProjects].
     */
    private fun readProjectPaths(
        platform: KotlinWebPlatformType,
    ): Set<String> {
        val updated = state.updateAndGet { current ->
            requireNotNull(current) { "KotlinSharedNpmProjectsRegistry state is null." }
            when (current) {
                is State.Collecting -> {
                    val snapshot = current.projects.mapValues { (_, platforms) -> platforms.toSet() }
                    State.Sealed(snapshot)
                }
                is State.Sealed -> current
            }
        }
        require(updated is State.Sealed)
        return updated.projects.filterValues { platform in it }.keys
    }

    companion object {
        /**
         * The name of the registry registered by the settings plugin.
         *
         * Fixed, so KGP finds it from every project, whichever classloader loaded KGP.
         */
        internal const val SETTINGS_SERVICE_NAME = "KotlinSharedNpmProjectsRegistry"

        /**
         * Registers the registry under [SETTINGS_SERVICE_NAME]. Called by the settings plugin.
         */
        internal fun registerForSettings(gradle: Gradle): Provider<KotlinSharedNpmProjectsRegistry> =
            gradle.sharedServices.registerIfAbsent(SETTINGS_SERVICE_NAME, KotlinSharedNpmProjectsRegistry::class.java) {}

        /**
         * The name of the registry KGP registers when the settings plugin is not applied, see [Project.sharedNpmProjectsRegistry].
         *
         * Differs from [SETTINGS_SERVICE_NAME], so KGP can tell whether the settings plugin is applied.
         */
        internal const val FALLBACK_SERVICE_NAME = "KotlinSharedNpmProjectsRegistryFallback"

        /**
         * Lazily adds the projects registered with a [platform] target to [sharedDependencies].
         *
         * This is the only way to read the registry.
         * The projects are read when [sharedDependencies] is resolved, after Isolated Projects has configured the other projects.
         */
        internal fun addRegisteredProjects(
            registry: Provider<KotlinSharedNpmProjectsRegistry>,
            sharedDependencies: Configuration,
            platform: KotlinWebPlatformType,
            dependencies: DependencyHandler,
        ) {
            sharedDependencies.dependencies.addAllLater(
                registry.map { service ->
                    service.readProjectPaths(platform).map { registeredPath ->
                        dependencies.projectCompat(registeredPath)
                    }
                }
            )
        }
    }
}

/**
 * The registry registered by the `org.jetbrains.kotlin.ecosystem` settings plugin.
 *
 * Without the settings plugin, with [WebIsolatedProjectsSupport.Preview] and without Isolated Projects,
 * KGP registers the registry itself, as the RFC's migration fallback describes (see [applySharedNpmProjectWithoutEcosystemPlugin]).
 * Otherwise `null`.
 */
private fun Project.sharedNpmProjectsRegistry(): Provider<KotlinSharedNpmProjectsRegistry>? {
    val registration = gradle.sharedServices.registrations.findByName(KotlinSharedNpmProjectsRegistry.SETTINGS_SERVICE_NAME)
    return when {
        registration != null -> {
            if (registration.service.orNull !is KotlinSharedNpmProjectsRegistry) {
                // TODO KT-91869 improve handling of KotlinSharedNpmProjectsRegistry loading failures
                logger.info(
                    "Could not load KotlinSharedNpmProjectsRegistry in $path. " +
                            "Current classloader: ${KotlinSharedNpmProjectsRegistry::class.java.classLoader} " +
                            "service classloader: ${registration.service.orNull?.javaClass?.classLoader}"
                )
                return null
            }
            @Suppress("UNCHECKED_CAST")
            registration.service as Provider<KotlinSharedNpmProjectsRegistry>
        }
        webIsolatedProjectsSupport != WebIsolatedProjectsSupport.Preview || isProjectIsolationEnabled -> {
            null
        }
        else -> {
            gradle.sharedServices.registerIfAbsent(
                KotlinSharedNpmProjectsRegistry.FALLBACK_SERVICE_NAME,
                KotlinSharedNpmProjectsRegistry::class.java,
            ) {}
        }
    }
}

/**
 * Registers the shared npm projects registry. Called by the `org.jetbrains.kotlin.ecosystem` settings plugin.
 */
@InternalKotlinGradlePluginApi
fun registerKotlinSharedNpmProjectsRegistry(gradle: Gradle) {
    KotlinSharedNpmProjectsRegistry.registerForSettings(gradle)
}

/**
 * Whether the `org.jetbrains.kotlin.ecosystem` settings plugin applies [KotlinSharedNpmProjectPlugin] to the root project.
 *
 * Temporarily enabled by [WebIsolatedProjectsSupport.Preview], until the settings DSL replaces it (KT-91658).
 */
// TODO KT-91833 Read the property through KGP's `PropertiesProvider`, like `Project.webIsolatedProjectsSupport`.
//      With Preview set only in `local.properties`, the settings plugin doesn't apply the shared npm project, and nothing reports why.
@InternalKotlinGradlePluginApi
fun isKotlinSharedNpmProjectEnabled(providers: ProviderFactory): Boolean =
    providers.gradleProperty(PropertiesProvider.PropertyNames.KOTLIN_WEB_ISOLATED_PROJECTS_SUPPORT).orNull ==
            WebIsolatedProjectsSupport.Preview.name

/**
 * Registers this project as having a [platform] target, so the root project adds it to its shared npm dependencies.
 */
internal fun Project.registerWithSharedNpmProjectsRegistry(platform: KotlinWebPlatformType) {
    val registry = sharedNpmProjectsRegistry()?.get()
        ?: return
    if (!registry.register(path, platform)) {
        reportDiagnosticOncePerProject(KotlinToolingDiagnostics.SharedNpmProjectRegisteredTooLate(path))
    }
}

/**
 * Whether the `org.jetbrains.kotlin.ecosystem` settings plugin registered the registry.
 */
internal fun Project.hasSettingsSharedNpmProjectsRegistry(): Boolean =
    gradle.sharedServices.registrations.findByName(KotlinSharedNpmProjectsRegistry.SETTINGS_SERVICE_NAME) != null

/**
 * The RFC's migration fallback for builds without the `org.jetbrains.kotlin.ecosystem` settings plugin.
 * Only used with [WebIsolatedProjectsSupport.Preview], which opts in to the shared npm project.
 *
 * - Without Isolated Projects, reports a warning, and applies [KotlinSharedNpmProjectPlugin] to the root project.
 *   This breaks Isolated Projects, which the RFC accepts for the fallback.
 *   The root project still collects the other projects through the registry KGP registers, see [sharedNpmProjectsRegistry].
 * - With Isolated Projects, the root project cannot be accessed, so reports an error.
 *
 * Called by each project with a JS or WasmJS target.
 */
internal fun Project.applySharedNpmProjectWithoutEcosystemPlugin() {
    if (webIsolatedProjectsSupport != WebIsolatedProjectsSupport.Preview) return
    if (hasSettingsSharedNpmProjectsRegistry()) return
    val isolatedProjectsEnabled = isProjectIsolationEnabled
    reportDiagnosticOncePerBuild(KotlinToolingDiagnostics.SharedNpmProjectEcosystemPluginMissing(isolatedProjectsEnabled))
    if (isolatedProjectsEnabled) return

    // Every project with a JS or WasmJS target calls this. Applying the plugin again is a no-op.
    rootProject.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
}

/**
 * Lazily adds the projects registered with a [platform] target to [sharedDependencies].
 *
 * Without the registry, adds nothing, so the shared npm project only collects the projects declared on it.
 */
internal fun addSharedNpmProjectsFromRegistry(
    project: Project,
    sharedDependencies: Configuration,
    platform: KotlinWebPlatformType,
) {
    val registry = project.sharedNpmProjectsRegistry()
        ?: return
    KotlinSharedNpmProjectsRegistry.addRegisteredProjects(
        registry = registry,
        sharedDependencies = sharedDependencies,
        platform = platform,
        dependencies = project.dependencies,
    )
}
