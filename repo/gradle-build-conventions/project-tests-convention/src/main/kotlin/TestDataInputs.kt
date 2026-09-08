/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.testing.Test

/**
 * Testdata which a single [Test] task reads, as opposed to the testdata of the whole project.
 *
 * By default a test task snapshots every directory registered via [ProjectTestsExtension.testData],
 * which is the right thing to do for a task running all the tests of a project. A task running the
 * tests of one testdata directory only (see [ProjectTestsExtension.testDataShards]) has to narrow
 * that down, otherwise editing any testdata file in the project would invalidate it.
 */
abstract class TestDataInputs {
    /**
     * Testdata files snapshotted as an input of the task.
     *
     * This affects up-to-date checks and the build cache key only. The testdata roots handed to the
     * test JVM come from [ProjectTestsExtension.testDataMap] and are deliberately left alone, so
     * that path resolution at runtime doesn't depend on how inputs happen to be declared: both
     * `ForTestCompileRuntime.transformTestDataPath` and `SystemPropertyTestDataRootConfigurator`
     * resolve a testdata path by matching it against those roots, and silently fall through to the
     * unchanged relative path when none of them matches.
     *
     * As a consequence, under-declaring here doesn't break tests, it makes a task stale-cacheable
     * instead. Use the `test-inputs-check` plugin, which detects reads of undeclared files, to
     * verify a narrowed declaration.
     */
    abstract val files: ConfigurableFileCollection
}

private const val TEST_DATA_INPUTS_EXTENSION_NAME = "testDataInputs"

/**
 * Returns the [TestDataInputs] of the task, creating it on the first call.
 *
 * Both the convention plugin and [ProjectTestsExtension.testDataShards] need it, and the order in
 * which their configuration actions run for a given task is not something to rely on, so neither of
 * them may assume that the other one has created it already.
 */
internal fun Test.testDataInputs(): TestDataInputs =
    extensions.findByType(TestDataInputs::class.java)
        ?: extensions.create(TEST_DATA_INPUTS_EXTENSION_NAME, TestDataInputs::class.java)