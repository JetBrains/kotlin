/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.test.backend.handlers.KlibArtifactHandler
import org.jetbrains.kotlin.test.backend.handlers.checkFullDiagnosticRender
import org.jetbrains.kotlin.test.backend.handlers.reportKtDiagnostics
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class JsBackendDiagnosticsHandler(testServices: TestServices) : KlibArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        reportKtDiagnostics(module, info.reporter)
        checkFullDiagnosticRender(module)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
