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

public class DelegationTest extends SingleFileTranslationTest {

    public DelegationTest() {
        super("delegation/");
    }

    public void testDelegation2() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegation3() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegation4() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationGenericArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationMethodsWithArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationByInh() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationChain() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationByExprWithArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationByArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationByNewInstance() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationByFunExpr() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationByIfExpr() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationEvaluationOrder1() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationEvaluationOrder2() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationExtFun1() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationExtFun2() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegationExtProp() throws Exception {
        checkFooBoxIsOk();
    }
}
