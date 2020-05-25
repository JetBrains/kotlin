/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest.Companion.DIAGNOSTIC_IN_TESTDATA_PATTERN
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest.Companion.SPEC_LINKED_TESTDATA_PATTERN
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest.Companion.SPEC_NOT_LINED_TESTDATA_PATTERN
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.io.File

private fun loadTestData(file: File, vararg patternsToBeRemoved: Regex): String {
    var text = KotlinTestUtils.doLoadFile(file)
    patternsToBeRemoved.forEach { text = text.replace(it, "") }
    return StringUtil.convertLineSeparators(text.trim()).trimTrailingWhitespacesAndAddNewlineAtEOF()
}

fun loadTestDataWithDiagnostics(file: File) = loadTestData(file, SPEC_LINKED_TESTDATA_PATTERN, SPEC_NOT_LINED_TESTDATA_PATTERN)

fun loadTestDataWithoutDiagnostics(file: File) = loadTestData(file, DIAGNOSTIC_IN_TESTDATA_PATTERN, SPEC_LINKED_TESTDATA_PATTERN)