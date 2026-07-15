/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

/**
 * Task for checking test data files in a specific module: it runs the module's tests and **fails**
 * on any mismatch without modifying anything ([Mode.CHECK]).
 *
 * This is the verification-only counterpart of [UpdateTestDataModuleTask]. Like it, options are
 * accepted only via `-P` Gradle properties — see [AbstractTestDataModuleTask] for the rationale and
 * the full option list.
 *
 * There is no global orchestrator: Gradle's task-name matching runs `checkTestData` in every module
 * with the `test-data-manager` plugin when invoked from the repo root.
 *
 * ## Usage
 *
 * ```bash
 * # Check a single test data file in one module
 * ./gradlew :analysis:analysis-api-fir:checkTestData \
 *     -Porg.jetbrains.kotlin.testDataManager.options.testDataPath=path/to/file.kt
 *
 * # Check across all modules with the plugin (Gradle task-name matching)
 * ./gradlew checkTestData \
 *     -Porg.jetbrains.kotlin.testDataManager.options.testClassPattern=.*Fir.*
 * ```
 *
 * @see AbstractTestDataModuleTask
 * @see UpdateTestDataModuleTask
 */
abstract class CheckTestDataModuleTask : AbstractTestDataModuleTask() {
    override val mode get() = Mode.CHECK

    init {
        description = "Checks test data files in this module and fails on mismatches without modifying them. " +
                "Options via -P${testDataManagerOptionsPrefix}.{testDataPath,testClassPattern,goldenOnly,incremental}."
    }
}
