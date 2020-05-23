/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.es6.semantics

import org.jetbrains.kotlin.js.test.BasicIrBoxES6Test
import org.jetbrains.kotlin.js.test.ir.semantics.AbstractIrJsTypeScriptExportTest
import org.jetbrains.kotlin.test.TargetBackend

abstract class AbstractIrBoxJsES6Test : BasicIrBoxES6Test(TEST_DATA_DIR_PATH + "box/", "irBox/")

abstract class AbstractIrJsCodegenBoxES6Test : BasicIrBoxES6Test(
    "compiler/testData/codegen/box/",
    "codegen/irBox/"
)

abstract class AbstractIrJsCodegenInlineES6Test : BasicIrBoxES6Test(
    "compiler/testData/codegen/boxInline/",
    "codegen/irBoxInline/"
)

abstract class AbstractIrJsTypeScriptExportES6Test : AbstractIrJsTypeScriptExportTest(
    targetBackend = TargetBackend.JS_IR_ES6
)
