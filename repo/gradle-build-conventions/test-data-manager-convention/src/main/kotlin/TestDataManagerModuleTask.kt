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
        systemProperty("$testDataManagerOptionsPrefix.mode", mode.get())
        testDataPath.orNull?.let { systemProperty("$testDataManagerOptionsPrefix.testDataPath", it) }
        testClassPattern.orNull?.let { systemProperty("$testDataManagerOptionsPrefix.testClassPattern", it) }
        goldenOnly.orNull?.let { systemProperty("$testDataManagerOptionsPrefix.goldenOnly", it) }
        incremental.orNull?.let { systemProperty("$testDataManagerOptionsPrefix.incremental", it) }

        super.exec()
    }
}
