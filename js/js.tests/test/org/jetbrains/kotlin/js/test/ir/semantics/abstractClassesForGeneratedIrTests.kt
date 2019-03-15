/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir.semantics

import org.jetbrains.kotlin.js.test.BasicBoxTest
import org.jetbrains.kotlin.js.test.BasicIrBoxTest

abstract class AbstractIrBoxJsTest : BasicIrBoxTest(BasicBoxTest.TEST_DATA_DIR_PATH + "box/", "irBox/")

abstract class AbstractIrJsCodegenBoxTest : BasicIrBoxTest(
    "compiler/testData/codegen/box/",
    "codegen/irBox/"
)

abstract class BorrowedIrInlineTest(relativePath: String) : BasicIrBoxTest(
    "compiler/testData/codegen/boxInline/$relativePath",
    "codegen/irBoxInline/$relativePath"
) {
    init {
        additionalCommonFileDirectories += BasicBoxTest.TEST_DATA_DIR_PATH + relativePath + "/_commonFiles/"
    }
}

abstract class AbstractIrNonLocalReturnsTest : BorrowedIrInlineTest("nonLocalReturns/")

abstract class AbstractIrPropertyAccessorsInlineTests : BorrowedIrInlineTest("property/")

abstract class AbstractIrNoInlineTests : BorrowedIrInlineTest("noInline/")

abstract class AbstractIrCallableReferenceInlineTests : BorrowedIrInlineTest("callableReference/")

abstract class AbstractIrEnumValuesInlineTests : BorrowedIrInlineTest("enum/")

abstract class AbstractIrInlineDefaultValuesTests : BorrowedIrInlineTest("defaultValues/")

abstract class AbstractIrInlineSuspendTests : BorrowedIrInlineTest("suspend/")

abstract class AbstractIrJsInlineContractsTests : BorrowedIrInlineTest("contracts/")