/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.Property

/**
 * Shared configuration for [TestDataManagerTask] in orchestrated mode.
 *
 * This extension is created by the root plugin (`test-data-manager-root`) and holds
 * configuration values that are published by [TestDataManagerGlobalTask] and read by
 * [TestDataManagerModuleTask] instances when running in orchestrated mode.
 *
 * The data flow in orchestrated mode is:
 * 1. [TestDataManagerGlobalTask] receives `@Option` values from CLI
 * 2. [TestDataManagerGlobalTask] publishes values to this extension
 * 3. [TestDataManagerModuleTask] instances detect orchestrated mode and pull from this extension
 *
 * When [TestDataManagerModuleTask] is run directly (not via orchestration), this extension
 * is not used - the task uses its own `@Option` values from CLI.
 *
 * @see TestDataManagerGlobalTask
 * @see TestDataManagerModuleTask
 */
interface TestDataManagerConfiguration {
    /** @see TestDataManagerTask.mode */
    val mode: Property<TestDataManagerMode>

    /** @see TestDataManagerTask.testDataPath */
    val testDataPath: Property<String>

    /** @see TestDataManagerTask.testClassPattern */
    val testClassPattern: Property<String>

    /** @see TestDataManagerTask.goldenOnly */
    val goldenOnly: Property<Boolean>

    /** @see TestDataManagerTask.incremental */
    val incremental: Property<Boolean>

    companion object {
        const val NAME = "${testDataManagerPrefix}Configuration"
    }
}
