/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

/**
 * Task for updating test data files in a specific module: it runs the module's tests and rewrites
 * mismatched test data files ([Mode.UPDATE]).
 *
 * This is the update counterpart of [CheckTestDataModuleTask]. Like it, options are accepted only via
 * `-P` Gradle properties — see [AbstractTestDataModuleTask] for the rationale and the full option list.
 *
 * There is no global orchestrator: Gradle's task-name matching runs `updateTestData` in every module
 * with the `test-data-manager` plugin when invoked from the repo root.
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
 * @see AbstractTestDataModuleTask
 * @see CheckTestDataModuleTask
 */
abstract class UpdateTestDataModuleTask : AbstractTestDataModuleTask() {
    override val mode get() = Mode.UPDATE

    init {
        description = "Updates test data files in this module. " +
                "Options via -P${testDataManagerOptionsPrefix}.{testDataPath,testClassPattern,goldenOnly,incremental}."
    }
}
