/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.runners

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.test.backend.ir.AbstractJvmIrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendInput
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.services.TestServices

class JvmIrBackendFacade(testServices: TestServices) : AbstractJvmIrBackendFacade(testServices) {
    override fun invokeBackend(inputArtifact: IrBackendInput): JvmIrCodegenFactory.BackendOutput {
        checkTestInfrastructure(inputArtifact is JvmIrBackendInput) {
            "JvmIrBackendFacade expects IrBackendInput.JvmIrBackendInput as input"
        }
        return inputArtifact.codegenFactory.generateModule(inputArtifact.state, inputArtifact.backendInput)
    }

    override val IrBackendInput.sourceFiles: Collection<KtSourceFile>
        get() = (this as JvmIrBackendInput).sourceFiles
}
