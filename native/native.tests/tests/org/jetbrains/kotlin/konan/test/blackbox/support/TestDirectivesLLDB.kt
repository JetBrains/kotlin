/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.junit.jupiter.api.Assertions
import java.io.File
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FIR_IDENTICAL
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.util.LLDBSessionSpec

internal fun parseLLDBSpec(testDataFile: File, registeredDirectives: RegisteredDirectives, settings: Settings): LLDBSessionSpec {
    val firIdentical = FIR_IDENTICAL in registeredDirectives
    val firSpecificExt = if (settings.get<PipelineType>() == PipelineType.K2 && !firIdentical) "fir." else ""
    val specFilePathWithoutExtension = testDataFile.absolutePath.removeSuffix(testDataFile.extension)
    val specFileLocation = "$specFilePathWithoutExtension${firSpecificExt}txt"
    val specFile = File(specFileLocation)
    return try {
        LLDBSessionSpec.parse(specFile.readText())
    } catch (e: Exception) {
        Assertions.fail<Nothing>("${testDataFile.absolutePath}: Cannot parse LLDB session specification: " + e.message, e)
    }
}
