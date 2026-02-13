/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.extra

/**
 * Common interface for test data management tasks.
 *
 * Defines shared `@Option` annotated properties that can be used both by:
 * - [TestDataManagerGlobalTask] - orchestrates across all modules
 * - [TestDataManagerModuleTask] - runs in a single module
 *
 * This allows running test data management either globally or per-module
 * with the same CLI options.
 *
 * **Important**: must be in sync with [TestDataManagerConfiguration].
 *
 * @see TestDataManagerConfiguration
 */
interface TestDataManagerTask : Task {
    /**
     * Mode: `check` (default) runs tests and fails on mismatches,
     * `update` runs tests and updates files on mismatches.
     */
    @get:Input
    @get:Option(option = "mode", description = "Mode: check (default) or update")
    @get:Optional
    val mode: Property<TestDataManagerMode>

    /**
     * Comma-separated list of test data paths to filter tests.
     * Each path can be a directory (runs all tests in that directory)
     * or a specific file path (runs tests for that file only).
     * If not specified, runs all tests.
     *
     * Example: `--test-data-path=dir1,dir2,file.kt`
     */
    @get:Input
    @get:Option(
        option = "test-data-path",
        description = "Comma-separated test data paths (directory or file). If not specified, runs all tests"
    )
    @get:Optional
    val testDataPath: Property<String>

    /**
     * Regex pattern for test class names.
     */
    @get:Input
    @get:Option(option = "test-class-pattern", description = "Regex pattern for test class names. If not specified, runs all tests")
    @get:Optional
    val testClassPattern: Property<String>

    /**
     * When true, runs only golden tests (tests with an empty variant chain).
     *
     * This filters out all variant-specific tests, running only the base/golden tests.
     */
    @get:Input
    @get:Option(option = "golden-only", description = "Run only golden tests (empty variant chain). If not specified, runs all tests")
    @get:Optional
    val goldenOnly: Property<Boolean>

    /**
     * When true, only runs variant tests for test data paths that had changes in the golden group.
     *
     * This optimizes `--mode=update` by tracking which golden tests actually wrote files
     * and skipping variant tests for unchanged paths.
     */
    @get:Input
    @get:Option(
        option = "incremental",
        description = "Only run variant tests for paths changed in golden group (effective with --mode=update)"
    )
    @get:Optional
    val incremental: Property<Boolean>
}

/**
 * A workaround to treat the task same way as [org.gradle.api.tasks.testing.Test]
 * to provide compatibility with IDEA's test runner.
 */
internal fun TestDataManagerTask.markAsIdeaTestTask() {
    val isIdeaActive = project.providers.systemProperty("idea.active").isPresent
    if (isIdeaActive) {
        extra["idea.internal.test"] = true
    }
}
