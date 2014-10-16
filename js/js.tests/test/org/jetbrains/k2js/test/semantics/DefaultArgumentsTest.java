/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.test.semantics;

import org.jetbrains.k2js.test.SingleFileTranslationTest;

public class DefaultArgumentsTest extends SingleFileTranslationTest {

    public DefaultArgumentsTest() {
        super("defaultArguments/");
    }

    public void testConstructorCallWithDefArg1() throws Exception {
        checkFooBoxIsOk();
    }

    public void testConstructorCallWithDefArg2() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDefArgsWithSuperCall() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnumWithDefArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnumWithOneDefArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnumWithTwoDefArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionFunWithDefArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testFunInAbstractClassWithDefArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOverloadFunWithDefArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOverrideValWithDefaultValue() throws Exception {
        checkFooBoxIsOk();
    }

    public void testVirtualCallWithDefArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testComplexExpressionAsDefaultArgument() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDefaultArgumentsInFunctionWithExpressionAsBody() throws Exception {
        checkFooBoxIsOk();
    }
}
