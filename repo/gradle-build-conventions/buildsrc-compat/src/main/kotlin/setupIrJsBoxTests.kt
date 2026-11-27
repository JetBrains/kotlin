/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.build.d8.D8Extension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun Test.useJsIrBoxTests(
    buildDir: Provider<Directory>,
) {
    val buildFeatures = project.serviceOf<BuildFeatures>()
    if (!buildFeatures.isolatedProjects.active.get()) {
        with(project.the<D8Extension>()) {
            setupV8()
        }
    }
    val stdLibJsClasses = project.jsRuntimeClasses("stdLibJsClasses", ":kotlin-stdlib")
    val jsDomApiCompatClasses = project.jsRuntimeClasses("jsDomApiCompatClasses", ":kotlin-dom-api-compat")
    val stdlibJsIrMinimalForTestClasses = project.jsRuntimeClasses("stdlibJsIrMinimalForTestClasses", ":kotlin-stdlib-js-ir-minimal-for-test")
    addClasspathProperty(stdLibJsClasses, "kotlin.js.full.stdlib.path")
    addClasspathProperty(jsDomApiCompatClasses, "kotlin.js.dom.api.compat")
    addClasspathProperty(stdlibJsIrMinimalForTestClasses, "kotlin.js.reduced.stdlib.path")

    systemProperty("kotlin.js.test.root.out.dir", "${buildDir.get().asFile.relativeTo(project.projectDir)}/")
}

/**
 * The configuration [name] with the unpacked JS runtime of the project [projectPath], configured only when it is created: [useJsIrBoxTests]
 * runs for every test task configured like `test` (e.g., the carrier of the test data manager), possibly after the configuration was
 * resolved, and a resolved configuration can't be modified.
 */
@OptIn(ExperimentalKotlinGradlePluginApi::class)
private fun Project.jsRuntimeClasses(name: String, projectPath: String): Configuration =
    configurations.findByName(name) ?: configurations.create(name) {
        isTransitive = false
        attributes {
            attribute(KlibPackaging.ATTRIBUTE, objects.named(KlibPackaging.NON_PACKED))
        }
        dependencies.add(project.dependencies.project(projectPath, "jsRuntimeElements"))
    }
