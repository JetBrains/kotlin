/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.inline.context.FunctionContext

import org.jetbrains.kotlin.js.translate.general.AstGenerationResult

class JsInliner(
    val reporter: JsConfig.Reporter,
    val config: JsConfig,
    val trace: DiagnosticSink,
    val translationResult: AstGenerationResult
) {

    init {
        // TODO Isn't there a better way to achieve this? Also there is a bug with private inline properties
        DummyAccessorInvocationTransformer().let {
            for (fragment in translationResult.newFragments) {
                it.accept<JsGlobalBlock>(fragment.declarationBlock)
                it.accept<JsGlobalBlock>(fragment.initializerBlock)
            }
        }
    }

    val functionContext = FunctionContext(this)

    val cycleReporter = InlinerCycleReporter(trace, functionContext)

    fun process() {
        for (fragment in translationResult.newFragments) {
            functionContext.scopeForFragment(fragment).process()
        }
    }
}