package org.jetbrains.kotlin.konan.test

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

internal fun AbstractNativeSimpleTest.firIdentical(testDataFile: File) =
    InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(testDataFile), TestDirectives.FIR_IDENTICAL.name)
