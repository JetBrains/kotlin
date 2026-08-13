/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinJsBrowserTestMetrics
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinBrowserTestRunnerDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.KotlinPlaywrightJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.PlaywrightBrowserInstall
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.PwBrowserKind
import org.jetbrains.kotlin.gradle.targets.wasm.internal.isWasm
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import kotlin.time.toJavaDuration

internal fun PwBrowserKind.getPwInstallBrowserTaskName() = "kotlinInstallPlaywright${browserName.replaceFirstChar { it.uppercaseChar() }}"

internal val ConfigureKotlinPlaywrightTestRunner = KotlinTargetSideEffect { target ->
    if (target !is KotlinJsIrTarget) return@KotlinTargetSideEffect

    val project = target.project


    project.launchInStage(KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript) {
        val browser = target.subTargets.filterIsInstance<KotlinBrowserJsIr>().singleOrNull() ?: return@launchInStage

        val browserTestDsl = browser.test as KotlinJsBrowserTestImpl

        if (target.isWasm) {
            project.reportDiagnostic(KotlinToolingDiagnostics.NewJsTestDslNotSupportedForWasmError())
            return@launchInStage
        }

        // TODO: KT-86706 Implement different browser runners as independent test runs
        //  so it is aligned with KGP API
        val testRun = browser.testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME)
        val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
        val testTaskProvider = testRun.executionTask

        val jdBrowserRunners = browserTestDsl.allBrowserRunners.get().values

        KotlinJsBrowserTestMetrics.collectMetrics(project, jdBrowserRunners)

        // KT-87641: Register one installation task per browser type so that multiple JS targets
        // with overlapping browser configurations don't cause DuplicateTaskException.
        // locateOrRegisterTask returns the existing task when already registered by another target.
        val browserInstallTasks = jdBrowserRunners
            .map { runner -> runner.getBrowserKind() }
            .distinct()
            .map { browserType ->
                project.locateOrRegisterTask<PlaywrightBrowserInstall>(browserType.getPwInstallBrowserTaskName(), args = listOf(testCompilation)) {
                    browsers.add(browserType.browserName)
                }
            }

        testTaskProvider.configure { testTask ->
            val objects = project.objects
            val inputs = KotlinPlaywrightJsTestFramework.createInputs(objects)

            // dependsOn is required because outputDir is internal and doesn't carry task dependencies.
            // Per-browser tasks are independent and can run in parallel.
            // KT-87599: Design host-wide toolchain management
            browserInstallTasks.forEach { installTask -> testTask.dependsOn(installTask) }

            // All install tasks write to the same global browsers directory; any one is sufficient to derive the path.
            inputs.playwrightBrowsersDirectory.set(browserInstallTasks.first().flatMap { it.outputDir })

            inputs.chromiumRunners.set(
                browserTestDsl.chromiumRunners.values.map { runner ->
                    KotlinPlaywrightJsTestFramework.createChromiumInputs(objects)
                        .also { it.populateFrom(project, runner) }
                }
            )
            inputs.firefoxRunners.set(
                browserTestDsl.firefoxRunners.values.map { runner ->
                    KotlinPlaywrightJsTestFramework.createFirefoxInputs(objects)
                        .also { it.populateFrom(project, runner) }
                }
            )
            inputs.webkitRunners.set(
                browserTestDsl.webkitRunners.values.map { runner ->
                    KotlinPlaywrightJsTestFramework.createWebkitInputs(objects)
                        .also { it.populateFrom(project, runner) }
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

internal fun KotlinBrowserTestRunnerDsl.getBrowserKind(): PwBrowserKind = when (this) {
    is KotlinFirefoxTestRunner -> PwBrowserKind.FIREFOX
    is KotlinWebkitTestRunner -> PwBrowserKind.WEBKIT
    is KotlinChromiumTestRunner -> PwBrowserKind.CHROMIUM
    else -> throw IllegalArgumentException("Unsupported browser runner: ${this::class.simpleName}")
}

private fun KotlinPlaywrightJsTestFramework.BrowserRunnerInput.populateFrom(
    project: Project,
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
    customBrowserExecutable.convention(
        runner.customBrowserExecutable.map { executable ->
            if (!executable.asFile.exists()) {
                project.reportDiagnostic(
                    KotlinToolingDiagnostics.NonExistentCustomBrowserExecutable(
                        runnerName = runner.name,
                        executable = executable.asFile,
                    )
                )
            }
            executable
        }
    )
}
