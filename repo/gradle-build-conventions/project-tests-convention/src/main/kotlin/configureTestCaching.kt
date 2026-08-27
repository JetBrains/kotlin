/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

internal fun Project.configureTestCaching() {
    tasks.withType<Test>().configureEach {
        val notCacheableTestProjects: List<String> = listOf(
            ":analysis:analysis-api-standalone:analysis-api-standalone-native",
            ":analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests",
            ":compiler:build-tools:kotlin-build-tools-api",
            ":compiler:build-tools:kotlin-build-tools-compat",
            ":compiler:build-tools:kotlin-build-tools-generator",
            ":compiler:fir:modularized-tests",
            ":compiler:fir:raw-fir:light-tree2fir",
            ":compiler:fir:raw-fir:psi2fir",
            ":compiler:multiplatform-parsing",
            ":compiler:test-infrastructure-utils",
            ":compiler:tests-integration",
            ":compose-compiler-gradle-plugin",
            ":examples:scripting-jvm-embeddable-host",
            ":examples:scripting-jvm-maven-deps-host",
            ":examples:scripting-jvm-simple-script-host",
            ":generators",
            ":jps:jps-common",
            ":jps:jps-plugin",
            ":kotlin-annotation-processing",
            ":kotlin-annotation-processing-base",
            ":kotlin-build-common",
            ":kotlin-compiler-client-embeddable",
            ":kotlin-compiler-embeddable",
            ":kotlin-daemon-client",
            ":kotlin-gradle-plugin",
            ":kotlin-gradle-plugin-dsl-codegen",
            ":kotlin-gradle-plugin-integration-tests",
            ":kotlin-gradle-statistics",
            ":kotlin-main-kts",
            ":kotlin-main-kts-test",
            ":kotlin-metadata-jvm",
            ":kotlin-power-assert-runtime", // TODO(KTI-3056): 'test-inputs-check' cannot be combined with 'multiplatform' projects
            ":kotlin-scripting-common",
            ":kotlin-scripting-dependencies",
            ":kotlin-scripting-dependencies-maven",
            ":kotlin-scripting-dependencies-maven-all",
            ":kotlin-scripting-ide-services-test",
            ":kotlin-scripting-jsr223-test",
            ":kotlin-scripting-jvm",
            ":kotlin-scripting-jvm-host-test",
            ":kotlin-stdlib",
            ":kotlin-stdlib-jdk8",
            ":kotlin-stdlib:samples",
            ":kotlin-test",
            ":kotlin-util-klib",
            ":kotlinx-metadata-klib",
            ":libraries:tools:abi-validation:abi-tools",
            ":libraries:tools:abi-validation:abi-tools-api",
            ":libraries:tools:abi-validation:abi-tools-tests",
            ":libraries:tools:abi-validation:kgp-integration-tests",

            ":plugins:compose-compiler-plugin:compiler-hosted:integration-tests",
            ":plugins:scripting:scripting-tests",
            ":plugins:scripting:scripting-tests:runtime",
            ":repo:auto-code-review", // Runs processes, traverses all repo files. Quick.
            ":repo:artifacts-tests",
            ":repo:codebase-tests",
            ":tools:binary-compatibility-validator",
            ":tools:ide-plugin-dependencies-validator",
            ":benchmarks",
            ":test-instrumenter"
        )
        val projectPath = project.path
        val hasTestInputCheckPlugin = plugins.hasPlugin("test-inputs-check")
        if (!hasTestInputCheckPlugin) {
            outputs.doNotCacheIf("https://youtrack.jetbrains.com/issue/KTI-112") { true }
        }
        doFirst {
            if (!hasTestInputCheckPlugin) {
                if (projectPath !in notCacheableTestProjects) {
                    throw GradleException(
                        """
                        Tests are not cacheable in: $projectPath
                        Apply id("test-inputs-check") to the project to make the tests cacheable.
                    """.trimIndent()
                    )
                }
            } else {
                if (projectPath in notCacheableTestProjects) {
                    throw GradleException("Tests are cacheable in: ${projectPath}, but we listed it in `notCacheableTestProjects`")
                }
            }
        }
    }
}
