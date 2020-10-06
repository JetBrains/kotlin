/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir.semantics

import org.jetbrains.kotlin.js.test.BasicIrBoxTest

abstract class AbstractIrBoxJsTest : BasicIrBoxTest(TEST_DATA_DIR_PATH + "box/", "irBox/")

abstract class AbstractIrJsCodegenBoxTest : BasicIrBoxTest(
    "compiler/testData/codegen/box/",
    "codegen/irBox/"
)

abstract class AbstractIrJsCodegenBoxErrorTest : BasicIrBoxTest(
    "compiler/testData/codegen/boxError/",
    "codegen/irBoxError/"
)

abstract class AbstractIrWasmBoxJsTest : BasicIrBoxTest(
    TEST_DATA_DIR_PATH + "wasmBox/",
    "irWasmBox/"
)

abstract class AbstractIrJsCodegenInlineTest : BasicIrBoxTest(
    "compiler/testData/codegen/boxInline/",
    "codegen/irBoxInline/"
)
