/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.wasm.test.BasicWasmBoxTest

abstract class AbstractIrCodegenBoxWasmTest : BasicWasmBoxTest(
    "compiler/testData/codegen/box/",
    "codegen/wasmBox/"
)

abstract class AbstractIrCodegenBoxInlineWasmTest : BasicWasmBoxTest(
    "compiler/testData/codegen/boxInline/",
    "codegen/wasmBoxInline/"
)

abstract class AbstractIrCodegenWasmJsInteropWasmTest : BasicWasmBoxTest(
    "compiler/testData/codegen/wasmJsInterop",
    "codegen/wasmJsInteropJs"
)

abstract class AbstractJsTranslatorWasmTest : BasicWasmBoxTest(
    "js/js.translator/testData/box/",
    "js.translator/wasmBox"
)

abstract class AbstractJsTranslatorUnitWasmTest : BasicWasmBoxTest(
    "js/js.translator/testData/box/",
    "js.translator/wasmBox",
    startUnitTests = true
)