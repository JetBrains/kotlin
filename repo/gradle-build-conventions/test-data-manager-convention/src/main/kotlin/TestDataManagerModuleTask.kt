/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction

/**
 * Task for running test data management in a specific module.
 *
 * This task can be run in two ways:
 * 1. **Directly** on a single module: `./gradlew :module:manageTestData --mode=update`
 * 2. **Via orchestration** from [TestDataManagerGlobalTask]: `./gradlew manageTestDataGlobally`
 *
 * When run via orchestration, the task pulls configuration from the shared
 * [TestDataManagerConfiguration] extension. When run directly, it uses
 * its own `@Option` values.
 *
 * For **update** workflows specifically, prefer [UpdateTestDataModuleTask] (`updateTestData`):
 * its options are passed as `-P` Gradle properties so the configuration cache stays valid
 * across option changes — important for fast iteration.
 *
 * ## Usage
 *
 * ```bash
 * # Direct invocation on a single module
 * ./gradlew :analysis:analysis-api-fir:manageTestData --mode=update
 * ./gradlew :analysis:analysis-api-fir:manageTestData --test-data-path=path/to/file.kt
 *
 * # Global orchestration (via test-data-manager-root plugin)
 * ./gradlew manageTestDataGlobally --mode=update
 * ```
 *
 * @see UpdateTestDataModuleTask for the CC-friendly update-only variant
 * @see TestDataManagerGlobalTask
 * @see TestDataManagerConfiguration
 */
abstract class TestDataManagerModuleTask : JavaExec(), TestDataManagerTask {
    init {
        group = "verification"
        description = "Manages test data files in this module: check for mismatches (default) or update them"
        mainClass.set("org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner")
    }

    @TaskAction
    override fun exec() {
        systemProperty(TestDataManagerOption.MODE, mode.get())
        testDataPath.orNull?.let { systemProperty(TestDataManagerOption.TEST_DATA_PATH, it) }
        testClassPattern.orNull?.let { systemProperty(TestDataManagerOption.TEST_CLASS_PATTERN, it) }
        goldenOnly.orNull?.let { systemProperty(TestDataManagerOption.GOLDEN_ONLY, it) }
        incremental.orNull?.let { systemProperty(TestDataManagerOption.INCREMENTAL, it) }

        super.exec()
    }
}
