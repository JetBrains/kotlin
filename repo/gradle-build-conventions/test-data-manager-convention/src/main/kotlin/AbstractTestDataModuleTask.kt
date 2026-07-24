/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Base class for the `-P`-driven test data manager tasks: [CheckTestDataModuleTask] (`checkTestData`)
 * and [UpdateTestDataModuleTask] (`updateTestData`).
 *
 * Both tasks are [JavaExec] tasks that run the module's tests via `TestDataManagerRunner` with a
 * **fixed** mode. Their options are accepted **only** as Gradle properties (`-P`), never as `--option`
 * CLI flags.
 *
 * ## Why `-P` options instead of `@Option`
 *
 * Only values **consumed during the configuration phase** are part of Gradle's configuration-cache
 * (CC) key. `@Option` CLI flags are applied while the task is *configured*, so iterating on, e.g.,
 * `--test-data-path` via `--option` re-runs configuration (1–2 minutes on this project) for each
 * distinct value. The options here are instead declared as `@Input` properties fed from `-P` Gradle
 * properties (see the plugin's `wireOptions`), resolved **lazily at execution time**. A lazily
 * resolved provider is not a configuration input, so the CC entry stays stable across option values
 * and subsequent invocations skip reconfiguration; on CC reuse the provider is re-evaluated, so the
 * current `-P` value takes effect rather than a stale cached one.
 *
 * ## Trade-off: never UP-TO-DATE, not build-cacheable
 *
 * The options are tracked `@Input`s, but these tasks declare **no outputs**. Gradle needs declared
 * outputs both for UP-TO-DATE checks and for the build cache, so the tasks always re-run: the runner
 * is invoked on every invocation regardless of input values. This is intentional — they are
 * [JavaExec] tasks that always execute their tests anyway. Making them cacheable would require
 * modeling the managed test data files as task outputs, which is out of scope here.
 *
 * ## Options
 *
 * All options are passed as Gradle properties; they are forwarded to the test runner as `-D<key>`
 * system properties at execution time.
 *
 * | Property                                                              | Effect                                          |
 * |-----------------------------------------------------------------------|-------------------------------------------------|
 * | `org.jetbrains.kotlin.testDataManager.options.testDataPath`           | Comma-separated test data paths (dir or file)   |
 * | `org.jetbrains.kotlin.testDataManager.options.testClassPattern`       | Regex pattern for test class names              |
 * | `org.jetbrains.kotlin.testDataManager.options.goldenOnly`             | Run only golden tests (empty variant chain)     |
 * | `org.jetbrains.kotlin.testDataManager.options.incremental`            | Only run variant tests for changed golden paths |
 *
 * @see CheckTestDataModuleTask
 * @see UpdateTestDataModuleTask
 */
abstract class AbstractTestDataModuleTask : JavaExec() {
    /**
     * The fixed mode this task runs in; forwarded to the runner. Each concrete subclass pins it.
     *
     * `@Internal` because the mode is a constant of the task type, not a user-supplied input.
     */
    @get:Internal
    protected abstract val mode: Mode

    /** Comma-separated test data paths (directory or file) to filter tests. */
    @get:[Input Optional]
    abstract val testDataPath: Property<String>

    /** Regex pattern for test class names. */
    @get:[Input Optional]
    abstract val testClassPattern: Property<String>

    /** When `true`, runs only golden tests (empty variant chain). */
    @get:[Input Optional]
    abstract val goldenOnly: Property<Boolean>

    /** When `true`, only runs variant tests for golden paths that changed (effective in update mode). */
    @get:[Input Optional]
    abstract val incremental: Property<Boolean>

    init {
        group = "verification"
        mainClass.set("org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner")
    }

    /**
     * Forwards the fixed [mode] and every set option to the test runner as `-D` system properties,
     * then delegates to [JavaExec.exec].
     *
     * All options are forwarded regardless of [mode] for symmetry; the runner ignores options that are
     * irrelevant to the current mode (e.g. `incremental` is effective only in update mode).
     */
    @TaskAction
    override fun exec() {
        systemProperty(TestDataManagerOption.MODE, mode)
        forwardOption(TestDataManagerOption.TEST_DATA_PATH, testDataPath)
        forwardOption(TestDataManagerOption.TEST_CLASS_PATTERN, testClassPattern)
        forwardOption(TestDataManagerOption.GOLDEN_ONLY, goldenOnly)
        forwardOption(TestDataManagerOption.INCREMENTAL, incremental)
        super.exec()
    }

    private fun forwardOption(key: String, value: Provider<*>) {
        value.orNull?.let { systemProperty(key, it) }
    }

    /**
     * How a test data manager task handles test data file mismatches:
     * - [CHECK] fails on mismatches without modifying anything.
     * - [UPDATE] rewrites mismatched files.
     *
     * The forwarded name is parsed by `TestDataManagerRunner` inside the test process.
     */
    protected enum class Mode {
        CHECK,
        UPDATE,
    }
}
