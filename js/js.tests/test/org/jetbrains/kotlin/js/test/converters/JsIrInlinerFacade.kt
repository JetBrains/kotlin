/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.ENABLE_IR_INLINER_BEFORE_KLIB_WRITING
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrInlinerFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class JsIrInlinerFacade(
    testServices: TestServices,
) : IrInlinerFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.directives.contains(ENABLE_IR_INLINER_BEFORE_KLIB_WRITING)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        // TODO: KT-68756: Invoke lowering prefix and IR Inliner
        return inputArtifact
    }
}
