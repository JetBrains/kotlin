/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.semantics

import org.jetbrains.kotlin.js.test.BasicBoxTest
import org.jetbrains.kotlin.js.test.BasicIrBoxTest

abstract class BorrowedInlineTest(relativePath: String) : BasicBoxTest(
        "compiler/testData/codegen/boxInline/$relativePath",
        "codegen/boxInline/$relativePath"
) {
    init {
        additionalCommonFileDirectories += BasicBoxTest.TEST_DATA_DIR_PATH + relativePath + "/_commonFiles/"
    }
}

abstract class AbstractNonLocalReturnsTest : BorrowedInlineTest("nonLocalReturns/")

abstract class AbstractPropertyAccessorsInlineTests : BorrowedInlineTest("property/")

abstract class AbstractNoInlineTests : BorrowedInlineTest("noInline/")

abstract class AbstractCallableReferenceInlineTests : BorrowedInlineTest("callableReference/")

abstract class AbstractEnumValuesInlineTests : BorrowedInlineTest("enum/")

abstract class AbstractInlineDefaultValuesTests : BorrowedInlineTest("defaultValues/")

abstract class AbstractInlineSuspendTests : BorrowedInlineTest("suspend/")

abstract class AbstractJsInlineContractsTests : BorrowedInlineTest("contracts/")

abstract class AbstractBoxJsTest : BasicBoxTest(
        BasicBoxTest.TEST_DATA_DIR_PATH + "box/",
        "box/"
) {
    override val runMinifierByDefault: Boolean = true
}

abstract class AbstractJsCodegenBoxTest : BasicBoxTest(
        "compiler/testData/codegen/box/",
        "codegen/box/"
)

abstract class AbstractJsLegacyPrimitiveArraysBoxTest : BasicBoxTest(
        "compiler/testData/codegen/box/arrays/",
        "codegen/box/arrays-legacy-primitivearrays/",
        typedArraysEnabled = false
)

abstract class AbstractSourceMapGenerationSmokeTest : BasicBoxTest(
        BasicBoxTest.TEST_DATA_DIR_PATH + "sourcemap/",
        "sourcemap/",
        generateSourceMap = true,
        generateNodeJsRunner = false
)