/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.preprocessors

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices


/**
 * Marks the `box()` function as exported during CLI invocation of the previous compiler, so it can be invoked by the test runner.
 */
class JsExportBoxPreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    private val topLevelBoxRegex = Regex("(^|\n|public\\s+)fun box\\(\\)")

    override fun process(file: TestFile, content: String): String {
        return topLevelBoxRegex.replace(content) { "\n@JsExport " + it.value }
    }
}
