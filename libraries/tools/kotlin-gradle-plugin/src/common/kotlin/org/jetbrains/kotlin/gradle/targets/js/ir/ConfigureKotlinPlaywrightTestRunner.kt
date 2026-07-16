/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinBrowserTestRunnerDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.KotlinPlaywrightJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.PlaywrightBrowserInstall
import org.jetbrains.kotlin.gradle.targets.wasm.internal.isWasm
import org.jetbrains.kotlin.gradle.tasks.registerTask
import kotlin.time.toJavaDuration

internal val ConfigureKotlinPlaywrightTestRunner = KotlinTargetSideEffect { target ->
    if (target !is KotlinJsIrTarget) return@KotlinTargetSideEffect

    val project = target.project


    project.launchInStage(KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript) {
        val browser = target.subTargets.filterIsInstance<KotlinBrowserJsIr>().singleOrNull() ?: return@launchInStage

        val browserTestDsl = browser.test as KotlinJsBrowserTestImpl
        if (browserTestDsl.allBrowserRunners.get().isEmpty()) {
            project.logger.debug("No browser runners configured. Skipping kotlin js test task configuration")
            return@launchInStage
        }

        if (target.isWasm) {
            project.reportDiagnostic(KotlinToolingDiagnostics.NewJsTestDslNotSupportedForWasmError())
            return@launchInStage
        }

        // TODO: KT-86706 Implement different browser runners as independent test runs
        //  so it is aligned with KGP API
        val testRun = browser.testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME)
        val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
        val testTaskProvider = testRun.executionTask

        val declaredPlaywrightBrowsers = browserTestDsl.allBrowserRunners.map { runners ->
            runners.values.map { it.playwrightBrowserName() }.toSet()
        }
        val playwrightBrowserInstallTask = project.registerTask<PlaywrightBrowserInstall>(
            "kotlinInstallPlaywrightBrowsers", listOf(testCompilation)
        ) {
            it.browsers.set(project.providers.provider {
                val browsers = declaredPlaywrightBrowsers.get()
                // Ultimate marks the test task as debug during configuration; the install task runs before jsBrowserTest.
                // Debug attaches through Chromium CDP even when the declared Playwright runner is Firefox/WebKit.
                if (testTaskProvider.get().debug) browsers + "chromium" else browsers
            })
        }

        testTaskProvider.configure { testTask ->
            val objects = project.objects
            val inputs = KotlinPlaywrightJsTestFramework.createInputs(objects)

            // dependsOn is required because outputDir is internal and doesn't carry task dependencies
            // FIXME: KT-87599 Design host-wide toolchain management
            testTask.dependsOn(playwrightBrowserInstallTask)
            inputs.playwrightBrowsersDirectory.set(playwrightBrowserInstallTask.flatMap { it.outputDir })

            inputs.chromiumRunners.set(
                browserTestDsl.chromiumRunners.values.map { runner ->
                    KotlinPlaywrightJsTestFramework.createChromiumInputs(objects)
                        .also { it.populateFrom(runner) }
                }
            )
            inputs.firefoxRunners.set(
                browserTestDsl.firefoxRunners.values.map { runner ->
                    KotlinPlaywrightJsTestFramework.createFirefoxInputs(objects)
                        .also { it.populateFrom(runner) }
                }
            )
            inputs.webkitRunners.set(
                browserTestDsl.webkitRunners.values.map { runner ->
                    KotlinPlaywrightJsTestFramework.createWebkitInputs(objects)
                        .also { it.populateFrom(runner) }
                }
            )

            if (testTask.testFramework != null) {
                project.reportDiagnostic(KotlinToolingDiagnostics.DuplicateJsBrowserTestFrameworkConfiguration())
            }
            testTask.testFramework = KotlinPlaywrightJsTestFramework(
                compilation = testCompilation,
                frameworkTaskInputs = inputs,
                objects = objects,
            )
        }
    }
}

private fun KotlinBrowserTestRunnerDsl.playwrightBrowserName(): String =
    when (this) {
        is KotlinFirefoxTestRunner -> "firefox"
        is KotlinWebkitTestRunner -> "webkit"
        is KotlinChromiumTestRunner -> "chromium"
        else -> throw IllegalArgumentException("Unsupported browser runner: ${this::class.simpleName}")
    }

private fun KotlinPlaywrightJsTestFramework.BrowserRunnerInput.populateFrom(
    runner: KotlinBrowserTestRunnerDsl,
) {
    name.convention(runner.name)
    testsLocation.convention(runner.testsLocation)
    // map to java duration is necessary because Gradle Task fingerprints doesn't work with kotlin.time.Duration
    // https://github.com/gradle/gradle/issues/38444
    timeout.convention(runner.timeout.map { it.toJavaDuration() })
    headless.convention(runner.headless)
    launchArgs.convention(runner.launchArgs)
    launchEnvironmentVariables.convention(runner.launchEnvironmentVariables)
    customBrowserExecutable.convention(runner.customBrowserExecutable)
}
