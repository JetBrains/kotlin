/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir.semantics

import org.jetbrains.kotlin.js.test.BasicIrBoxTest

abstract class AbstractIrCodegenWasmJsInteropJsTest : BasicIrBoxTest(
    "compiler/testData/codegen/wasmJsInterop",
    "codegen/wasmJsInteropJs"
)
