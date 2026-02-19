/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.limitLanguageAndApiVersions(version: KotlinVersion) {
    val projectsUsedInIntelliJKotlinPlugin: Array<String> by rootProject.extra
    val kotlinApiVersionForProjectsUsedInIntelliJKotlinPlugin: String by rootProject.extra

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            if (project.path !in projectsUsedInIntelliJKotlinPlugin ||
                KotlinVersion.fromVersion(kotlinApiVersionForProjectsUsedInIntelliJKotlinPlugin) > version
            ) {
                // check the `configureKotlinCompilationOptions` in `common-configurations.gradle.kts` out
                apiVersion.set(version)
            }
            languageVersion.set(version)
            freeCompilerArgs.add("-Xsuppress-version-warnings")
        }
    }
}