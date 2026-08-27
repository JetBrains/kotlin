/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Property
import org.gradle.api.reflect.TypeOf
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.withType

/**
 * The 'test-runtime' module is universally required for running tests properly:
 * It adds necessary extensions and rules to running tests (e.g. test-mutes or test-federation)
 */
internal fun Project.configureTestRuntime() {
    dependencies.extensions.add(
        ProjectDependency::class.java, "testRuntime", dependencies.project(":repo:test-runtime")
    )

    val testRuntimeEnabled = project.objects.property<Boolean>()
        .convention(true).value(true)

    extensions.add(
        object : TypeOf<Property<Boolean>>() {}, "testRuntimeEnabled", testRuntimeEnabled
    )

    val testRuntime = configurations.detachedConfiguration(dependencies.project(":repo:test-runtime")).apply {
        isTransitive = false
    }.incoming.files

    afterEvaluate {
        testRuntimeEnabled.disallowChanges()
        if (!testRuntimeEnabled.get()) return@afterEvaluate

        tasks.withType<Test>().configureEach {
            classpath = files(testRuntime, classpath)

            /* Ensure that the test runtime is always available on the classpath (and the extension is enabled) */
            systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
            doFirst {
                /* Check if classpath contains test federation runtime */
                if (!classpath.files.containsAll(testRuntime.files)) {
                    error("Test Runtime is not available on the classpath")
                }
            }
        }

        dependencies {
            configurations.findByName("testImplementation")?.name(project(":repo:test-runtime"))
            configurations.findByName("jvmTestImplementation")?.name(project(":repo:test-runtime"))
            configurations.findByName("testFixturesCompileOnly")?.name(project(":repo:test-runtime"))
        }
    }
}
