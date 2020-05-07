/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest.Companion.DIAGNOSTIC_IN_TESTDATA_PATTERN
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest.Companion.SPEC_LINKS_IN_TESTDATA_PATTERN
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.io.File

fun loadTestDataWithoutDiagnostics(file: File): String {
    val textWithoutDiagnostics = KotlinTestUtils.doLoadFile(file)
        .replace(DIAGNOSTIC_IN_TESTDATA_PATTERN, "")
        .replace(SPEC_LINKS_IN_TESTDATA_PATTERN, "")
    return StringUtil.convertLineSeparators(textWithoutDiagnostics.trim()).trimTrailingWhitespacesAndAddNewlineAtEOF()
}
