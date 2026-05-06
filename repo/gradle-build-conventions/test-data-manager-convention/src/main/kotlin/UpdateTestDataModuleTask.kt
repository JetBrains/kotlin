/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Task for updating test data files in a specific module.
 *
 * Unlike [TestDataManagerModuleTask], this task:
 * 1. Has **no** `--option` CLI flags — its options are accepted only via Gradle properties (`-P`).
 * 2. Implies `mode = update` — it always rewrites mismatched test data files.
 *
 * The reason is configuration-cache (CC) friendliness. CLI `@Option` values are part of the CC
 * key, so iterating on `--test-data-path` with [TestDataManagerModuleTask] re-runs Gradle
 * configuration for each different value (which can take 1–2 minutes on this project).
 * Reading options as `-P` properties **only at execution time** keeps the CC entry stable across
 * different option values, so subsequent invocations skip reconfiguration entirely.
 *
 * ## Trade-off: opaque to Gradle's task cache
 *
 * The same mechanism that keeps the CC stable — not exposing options as `@Input` properties —
 * also hides them from Gradle's task-identity machinery. As a result, Gradle cannot reuse a
 * previous run's outcome: the task is never UP-TO-DATE, never restored from the build cache,
 * and the test runner is invoked on every invocation. In practice this matches the behavior of
 * [TestDataManagerModuleTask] (both are [JavaExec] tasks with no declared outputs that always
 * re-run), but the choice is permanent and intentional here — input-tracking the options would
 * undo the CC benefit.
 *
 * ## Options
 *
 * All options are passed as Gradle properties; they are forwarded to the test runner as
 * `-D<key>` system properties at execution time.
 *
 * | Property                                                              | Effect                                          |
 * |-----------------------------------------------------------------------|-------------------------------------------------|
 * | `org.jetbrains.kotlin.testDataManager.options.testDataPath`           | Comma-separated test data paths (dir or file)   |
 * | `org.jetbrains.kotlin.testDataManager.options.testClassPattern`       | Regex pattern for test class names              |
 * | `org.jetbrains.kotlin.testDataManager.options.goldenOnly`             | Run only golden tests (empty variant chain)     |
 * | `org.jetbrains.kotlin.testDataManager.options.incremental`            | Only run variant tests for changed golden paths |
 *
 * ## Usage
 *
 * ```bash
 * # Single module, single test data file
 * ./gradlew :analysis:analysis-api-fir:updateTestData \
 *     -Porg.jetbrains.kotlin.testDataManager.options.testDataPath=path/to/file.kt
 *
 * # All modules with the test-data-manager plugin (Gradle task-name matching)
 * ./gradlew updateTestData \
 *     -Porg.jetbrains.kotlin.testDataManager.options.testClassPattern=.*Fir.*
 * ```
 *
 * @see TestDataManagerModuleTask
 */
abstract class UpdateTestDataModuleTask : JavaExec() {
    @get:Inject
    protected abstract val providers: ProviderFactory

    init {
        group = "verification"
        description = "Updates test data files in this module. " +
                "Options via -P${testDataManagerOptionsPrefix}.{testDataPath,testClassPattern,goldenOnly,incremental}. " +
                "Configuration cache stays valid when these property values change."
        mainClass.set("org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner")
    }

    @TaskAction
    override fun exec() {
        systemProperty(TestDataManagerOption.MODE, TestDataManagerMode.UPDATE)
        forwardOption(TestDataManagerOption.TEST_DATA_PATH)
        forwardOption(TestDataManagerOption.TEST_CLASS_PATTERN)
        forwardOption(TestDataManagerOption.GOLDEN_ONLY)
        forwardOption(TestDataManagerOption.INCREMENTAL)
        super.exec()
    }

    private fun forwardOption(key: String) {
        providers.gradleProperty(key).orNull?.let { systemProperty(key, it) }
    }
}
