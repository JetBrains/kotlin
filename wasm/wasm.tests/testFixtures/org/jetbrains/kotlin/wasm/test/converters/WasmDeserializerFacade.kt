/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmIrLoadingPipelinePhase
import org.jetbrains.kotlin.test.backend.ir.IrDeserializerCliFacade
import org.jetbrains.kotlin.test.services.TestServices

class WasmDeserializerFacade(testServices: TestServices) :
    IrDeserializerCliFacade<WasmIrLoadingPipelinePhase, WebLoadedIrPipelineArtifact>(testServices, WasmIrLoadingPipelinePhase)
