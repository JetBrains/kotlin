/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Mode for the test data manager.
 *
 * Controls how [TestDataManagerTask] handles test data file mismatches.
 */
enum class TestDataManagerMode {
    /**
     * Run tests and fail on mismatches.
     *
     * This is the default mode - safe for CI.
     * Tests will fail if the actual output differs from expected files.
     */
    CHECK,

    /**
     * Run tests and update files on mismatches.
     *
     * Use explicitly when you want to update test data.
     * Files will be silently updated to match the actual output.
     */
    UPDATE
    ;

    companion object {
        val DEFAULT: TestDataManagerMode get() = CHECK
    }
}
