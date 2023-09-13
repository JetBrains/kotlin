/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue

/**
 * Simplified [TestRun] provider. Used in Native KLIB tests.
 */
internal object SimpleTestRunProvider : BaseTestRunProvider() {
    fun getTestRun(testCase: TestCase, executable: TestExecutable): TestRun {
        assertTrue(testCase.kind != TestKind.REGULAR) { "Regular tests are not supported in ${SimpleTestRunProvider::class.java}" }
        return super.createSingleTestRun(testCase, executable)
    }
}
