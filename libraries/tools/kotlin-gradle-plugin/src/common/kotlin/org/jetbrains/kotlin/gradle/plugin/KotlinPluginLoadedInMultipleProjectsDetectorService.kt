/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.PluginLoadedInMultipleProjectsError
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic

/**
 * Detects whether the plugin's classes were loaded into multiple isolated [ClassLoader] instances across subprojects.
 *
 * ### Problem
 *
 * Gradle uses a hierarchical ClassLoader structure where subproject buildscript ClassLoaders inherit from the root project's
 * buildscript ClassLoader using parent-first delegation.
 *
 * - **Root-declared:** If declared in the root `build.gradle.kts` (e.g., via `plugins { kotlin(...) apply false }`), the plugin is
 *   loaded by the root ClassLoader. Subprojects delegate to the parent and share the same [Class] instance.
 * - **Subproject-declared:** If declared only in individual subprojects, the root ClassLoader lacks the plugin. Each subproject
 *   independently loads its own copy of the plugin's classes into its own ClassLoader scope, which can lead to unexpected behavior.
 *
 * ### Mechanism & Project Isolation
 *
 * 1. Every project attempts to register the service with `project.gradle.sharedServices` under a fixed string key.
 * 2. **Correct Setup (Root-declared / Single ClassLoader):**
 *    - The first project creates and registers the service instance, leading to the plugin classes being loaded by the root ClassLoader.
 *    - Subsequent projects retrieve the existing registered instance.
 *    - Because all subprojects' ClassLoaders inherit from the root ClassLoader (and thus the exact same [Class] type identity), casting 
 *      or invoking methods on the service succeeds.
 * 3. **Incorrect Setup (Subproject-declared / Multiple ClassLoaders):**
 *    - Subproject A registers the service instance using ClassLoader A's class definition.
 *    - Subproject B calls `registerIfAbsent` under the same key and receives Subproject A's service instance.
 *    - Subproject B attempts to cast or invoke a typed method on the instance expecting ClassLoader B's class definition.
 *    - Because `(Class, ClassLoaderA) != (Class, ClassLoaderB)`, the JVM's `CHECKCAST` instruction fails and throws a
 *      [ClassCastException], reliably signaling that the plugin was loaded across isolated ClassLoaders.
 *
 * ### How to use
 *
 * Call [detect] in the plugin's `apply` method.
 */
internal abstract class KotlinPluginLoadedInMultipleProjectsDetectorService : BuildService<BuildServiceParameters.None> {
    companion object {
        /**
         * Detects whether the plugin's classes were loaded into multiple isolated [ClassLoader] instances across subprojects, and
         * if it's the case, reports the [PluginLoadedInMultipleProjectsError] diagnostic, unless the
         * `kotlin.pluginLoadedInMultipleProjects.ignore` property is set to true.
         */
        fun detect(project: Project) {
            try {
                project.gradle.sharedServices
                    .registerIfAbsent(
                        "KotlinPluginLoadedInMultipleProjectsDetectorService",
                        KotlinPluginLoadedInMultipleProjectsDetectorService::class.java
                    ) {} // Switch to the overload without the configure block when minimum supported Gradle version is 8.7.
                    .get() as KotlinPluginLoadedInMultipleProjectsDetectorService
            } catch (_: ClassCastException) {
                val propertiesProvider = PropertiesProvider(project)
                if (propertiesProvider.ignorePluginLoadedInMultipleProjects != true) {
                    project.reportDiagnostic(PluginLoadedInMultipleProjectsError(loadedInProject = project.path))
                }
            }
        }
    }
}
