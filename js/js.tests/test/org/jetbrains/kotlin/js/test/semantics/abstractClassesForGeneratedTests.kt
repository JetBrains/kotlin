/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.test.semantics

import org.jetbrains.kotlin.js.test.BasicBoxTest

abstract class JsBasicBoxTest(relativePath: String) : BasicBoxTest(
        BasicBoxTest.TEST_DATA_DIR_PATH + relativePath + "cases/",
        BasicBoxTest.TEST_DATA_DIR_PATH + relativePath + "out/"
) {
    init {
        additionalCommonFileDirectories += BasicBoxTest.TEST_DATA_DIR_PATH + relativePath + "/_commonFiles/"
    }
}

abstract class BorrowedTest(relativePath: String) : BasicBoxTest(
        "compiler/testData/codegen/box/" + relativePath,
        BasicBoxTest.TEST_DATA_DIR_PATH + relativePath + "out/"
) {
    init {
        additionalCommonFileDirectories += BasicBoxTest.TEST_DATA_DIR_PATH + relativePath + "/_commonFiles/"
    }
}

abstract class AbstractBoxJsTest() : BasicBoxTest(
        BasicBoxTest.TEST_DATA_DIR_PATH + "box/",
        BasicBoxTest.TEST_DATA_DIR_PATH + "out/"
)

abstract class AbstractBridgeTest : BorrowedTest("bridges/")

abstract class AbstractCompanionObjectTest : JsBasicBoxTest("objectIntrinsics/")

abstract class AbstractFunctionExpressionTest : JsBasicBoxTest("functionExpression/")

abstract class AbstractReservedWordTest : JsBasicBoxTest("reservedWords/")

abstract class AbstractSecondaryConstructorTest : JsBasicBoxTest("secondaryConstructors/")

abstract class AbstractInnerNestedTest : JsBasicBoxTest("innerNested/")

abstract class AbstractClassesTest : JsBasicBoxTest("classes/")

abstract class AbstractSuperTest : JsBasicBoxTest("super/")

abstract class AbstractLocalClassesTest : JsBasicBoxTest("localClasses/")

abstract class AbstractNonLocalReturnsTest : JsBasicBoxTest("inline.generated/nonLocalReturns/")
