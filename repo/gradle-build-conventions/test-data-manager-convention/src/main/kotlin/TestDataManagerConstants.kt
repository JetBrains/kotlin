/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

internal const val manageTestDataTaskName = "manageTestData"
internal const val manageTestDataGloballyTaskName = "${manageTestDataTaskName}Globally"
internal const val updateTestDataTaskName = "updateTestData"
internal const val testDataManagerPrefix = "org.jetbrains.kotlin.testDataManager"
internal const val testDataManagerOptionsPrefix = "$testDataManagerPrefix.options"

/**
 * Single source of truth for option keys used as both:
 * - Gradle property names (with `-P` prefix on the command line) for [UpdateTestDataModuleTask], and
 * - JVM system properties read by `TestDataManagerRunner` inside the test process.
 *
 * The two are intentionally identical so a value supplied as `-P<key>=...` is forwarded
 * verbatim as `-D<key>=...` to the runner.
 */
internal object TestDataManagerOption {
    const val MODE = "$testDataManagerOptionsPrefix.mode"
    const val TEST_DATA_PATH = "$testDataManagerOptionsPrefix.testDataPath"
    const val TEST_CLASS_PATTERN = "$testDataManagerOptionsPrefix.testClassPattern"
    const val GOLDEN_ONLY = "$testDataManagerOptionsPrefix.goldenOnly"
    const val INCREMENTAL = "$testDataManagerOptionsPrefix.incremental"
    const val PROJECT_NAME = "$testDataManagerOptionsPrefix.projectName"
    const val MODULE = "$testDataManagerOptionsPrefix.module"
}
