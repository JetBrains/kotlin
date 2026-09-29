/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.preprocessors

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultDirectives
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.AbstractLauncherAdditionalSourceProvider.Companion.isGroupedNonIsolatedBatch


/**
 * Marks the `box()` function as exported during CLI invocation of the previous compiler, so it can be invoked by the test runner.
 *
 * A test of a grouped batch is left as it is: its `box()` is called by the launcher of the batch, by the fully
 * qualified name, so it is the entry point of the launcher that is exported.
 */
class JsExportBoxPreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    private val topLevelBoxRegex = Regex("(^|\n|public\\s+)fun box\\(\\)")

    override fun process(file: TestFile, content: String): String {
        return topLevelBoxRegex.replace(content) { "\n@JsExport " + it.value }
    }

    @TestInfrastructureInternals
    override fun processModule(module: TestModule, filesContent: MutableMap<TestFile, String>) {
        if (testServices.isGroupedNonIsolatedBatch(testServices.defaultDirectives, testServices.moduleStructure)) return
        super.processModule(module, filesContent)
    }
}
