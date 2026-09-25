/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.swc;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

/**
 * TODO KT-82969:
 *   Accesses the SWC part of the Kotlin Gradle plugin, which is not a part of its public API.
 *   <p>
 *   Java is used on purpose: Kotlin {@code internal} declarations are plain {@code public} in the bytecode,
 *   so this bridge compiles against the bootstrap KGP without friend paths or visibility suppressions.
 *   The {@code KotlinInternalInJava} inspection is suppressed here for the same reason: the access is
 *   intentional, and the suppression is checked in, so that every developer of this repository gets the
 *   same result regardless of their local IDE settings. The KGP types are referenced by their fully
 *   qualified names, as {@code @SuppressWarnings} does not cover import statements.
 *   <p>
 *   Once the bootstrap contains the {@code @InternalKotlinGradlePluginApi} annotated declarations,
 *   this class may be dropped in favor of using them directly from Kotlin with an opt-in.
 */
@SuppressWarnings("KotlinInternalInJava")
public final class SwcBridge {
    private SwcBridge() {
    }

    /**
     * Applies {@code SwcPlugin} to the project and returns the created {@code SwcEnvSpec} extension.
     */
    public static Object applySwcPlugin(Project project) {
        project.getPlugins().apply(org.jetbrains.kotlin.gradle.targets.js.swc.SwcPlugin.class);
        return project.getExtensions()
                .getByName(org.jetbrains.kotlin.gradle.targets.js.swc.SwcEnvSpec.Companion.getEXTENSION_NAME());
    }

    /**
     * Unsets the default download base URL, so that {@code AbstractSetupTask} does not add a project-level
     * repository. The repository is declared in the root {@code settings.gradle.kts} instead, as the build
     * is configured with {@code RepositoriesMode.FAIL_ON_PROJECT_REPOS}.
     */
    public static void unsetDownloadBaseUrl(Object swcEnvSpec) {
        spec(swcEnvSpec).getDownloadBaseUrl().set((String) null);
    }

    public static void setVersion(Object swcEnvSpec, String version) {
        spec(swcEnvSpec).getVersion().set(version);
    }

    public static Provider<String> getExecutable(Object swcEnvSpec) {
        return spec(swcEnvSpec).getExecutable();
    }

    public static TaskProvider<? extends Task> getSetupTaskProvider(Project project, Object swcEnvSpec) {
        return spec(swcEnvSpec).getSwcSetupTaskProvider(project);
    }

    private static org.jetbrains.kotlin.gradle.targets.js.swc.SwcEnvSpec spec(Object swcEnvSpec) {
        return (org.jetbrains.kotlin.gradle.targets.js.swc.SwcEnvSpec) swcEnvSpec;
    }
}
