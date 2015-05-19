/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.js.test.AbstractSingleFileTranslationWithDirectivesTest
import org.jetbrains.kotlin.js.test.MultipleModulesTranslationTest
import org.jetbrains.kotlin.js.test.SingleFileTranslationTest

private abstract class AbstractBlackBoxTest(d: String) : SingleFileTranslationTest(d) {
    override fun doTest(filename: String) = checkBlackBoxIsOkByPath(filename)
}

public abstract class AbstractBridgeTest : AbstractBlackBoxTest("bridges/")

public abstract class AbstractCallableReferenceTest(main: String) : SingleFileTranslationTest("callableReference/" + main)

public abstract class AbstractCompanionObjectTest : SingleFileTranslationTest("objectIntrinsics/")

public abstract class AbstractDynamicTest : SingleFileTranslationTest("dynamic/")

public abstract class AbstractFunctionExpressionTest : AbstractBlackBoxTest("functionExpression/")

public abstract class AbstractInlineEvaluationOrderTest : AbstractSingleFileTranslationWithDirectivesTest("inlineEvaluationOrder/")

public abstract class AbstractInlineJsStdlibTest : AbstractSingleFileTranslationWithDirectivesTest("inlineStdlib/")

public abstract class AbstractInlineJsTest : AbstractSingleFileTranslationWithDirectivesTest("inline/")

public abstract class AbstractJsCodeTest : AbstractSingleFileTranslationWithDirectivesTest("jsCode/")

public abstract class AbstractLabelTest : AbstractSingleFileTranslationWithDirectivesTest("labels/")

public abstract class AbstractMultiModuleTest : MultipleModulesTranslationTest("multiModule/")

public abstract class AbstractInlineMultiModuleTest : MultipleModulesTranslationTest("inlineMultiModule/")

public abstract class AbstractReservedWordTest : SingleFileTranslationTest("reservedWords/")
