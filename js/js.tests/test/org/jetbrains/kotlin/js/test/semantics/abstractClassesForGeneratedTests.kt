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
import org.jetbrains.kotlin.js.test.MultipleModulesTranslationTest

abstract class AbstractBridgeTest : BasicBoxTest("bridges/")

abstract class AbstractFunctionCallableReferenceTest : BasicBoxTest("callableReference/function/")

abstract class AbstractPropertyCallableReferenceTest : BasicBoxTest("callableReference/property/")

abstract class AbstractCompanionObjectTest : BasicBoxTest("objectIntrinsics/")

abstract class AbstractDynamicTest : BasicBoxTest("dynamic/")

abstract class AbstractFunctionExpressionTest : BasicBoxTest("functionExpression/")

abstract class AbstractInlineEvaluationOrderTest : BasicBoxTest("inlineEvaluationOrder/")

abstract class AbstractInlineJsStdlibTest : BasicBoxTest("inlineStdlib/")

abstract class AbstractInlineJsTest : BasicBoxTest("inline/")

abstract class AbstractJsCodeTest : BasicBoxTest("jsCode/")

abstract class AbstractLabelTest : BasicBoxTest("labels/")

abstract class AbstractMultiModuleTest : BasicBoxTest("multiModule/")

abstract class AbstractInlineMultiModuleTest : MultipleModulesTranslationTest("inlineMultiModule/")

abstract class AbstractReservedWordTest : BasicBoxTest("reservedWords/")

abstract class AbstractSecondaryConstructorTest : BasicBoxTest("secondaryConstructors/")

abstract class AbstractInnerNestedTest : BasicBoxTest("innerNested/")

abstract class AbstractClassesTest : BasicBoxTest("classes/")

abstract class AbstractSuperTest : BasicBoxTest("super/")

abstract class AbstractLocalClassesTest : BasicBoxTest("localClasses/")

abstract class AbstractNonLocalReturnsTest : BasicBoxTest("inline.generated/nonLocalReturns/")

abstract class AbstractRttiTest : BasicBoxTest("rtti/")

abstract class AbstractCastTest : BasicBoxTest("expression/cast/")

abstract class AbstractLightReflectionTest : BasicBoxTest("reflection/light/")
