/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Root task for managing test data files across multiple modules.
 *
 * This task orchestrates test data management by:
 * 1. Publishing its `@Option` values to the shared [TestDataManagerConfiguration]
 * 2. Depending on all module tasks via proper Gradle task dependencies
 * 3. Using `mustRunAfter` constraints to ensure priority ordering
 *
 * The actual work is done by [TestDataManagerModuleTask] instances in each module.
 * This task only provides configuration and orchestration.
 *
 * ## Usage
 *
 * ```bash
 * ./gradlew manageTestDataGlobally                              # Default: check mode, all modules
 * ./gradlew manageTestDataGlobally --mode=check                 # Explicit check mode
 * ./gradlew manageTestDataGlobally --mode=update                # Update files in all modules
 * ./gradlew manageTestDataGlobally --test-data-path=dir1,dir2,file.kt
 * ./gradlew manageTestDataGlobally --test-class-pattern=.*Fir.*
 * ```
 *
 * Use `-Porg.jetbrains.kotlin.testDataManager.options.module=:path` to filter modules.
 *
 * @see TestDataManagerMode
 * @see TestDataManagerConfiguration
 * @see TestDataManagerModuleTask
 */
abstract class TestDataManagerGlobalTask : DefaultTask(), TestDataManagerTask {
    init {
        group = "verification"
        description = "Manages test data files across all modules: check for mismatches (default) or update them. " +
                "Use -Porg.jetbrains.kotlin.testDataManager.options.module=:path to filter"

        mode.convention(TestDataManagerMode.DEFAULT)
    }

    @TaskAction
    fun execute() {
        logger.lifecycle("$manageTestDataGloballyTaskName completed in ${mode.get()} mode")
    }
}
