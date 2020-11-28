/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.wasm.semantics

import org.jetbrains.kotlin.js.test.BasicWasmBoxTest

abstract class AbstractIrCodegenBoxWasmTest : BasicWasmBoxTest(
    "compiler/testData/codegen/box/",
    "codegen/wasmBox/"
)