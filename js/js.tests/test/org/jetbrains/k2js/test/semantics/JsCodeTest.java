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

import org.jetbrains.k2js.test.SingleFileTranslationWithDirectivesTest;

public final class JsCodeTest extends SingleFileTranslationWithDirectivesTest {

    public JsCodeTest() {
        super("jsCode/");
    }

    public void testQuotes() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLiteral() throws Exception {
        checkFooBoxIsOk();
    }

    public void testFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testObject() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOperators() throws Exception {
        checkFooBoxIsOk();
    }

    public void testIf() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSwitch() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDoWhile() throws Exception {
        checkFooBoxIsOk();
    }

    public void testWhile() throws Exception {
        checkFooBoxIsOk();
    }

    public void testFor() throws Exception {
        checkFooBoxIsOk();
    }

    public void testForIn() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInvocation() throws Exception {
        checkFooBoxIsOk();
    }

    public void testBreak() throws Exception {
        checkFooBoxIsOk();
    }

    public void testContinue() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLabel() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLabelSiblingClash() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testLabelNestedClash() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testTryCatchFinally() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCatchScope() throws Exception {
        checkFooBoxIsOk();
    }

    public void testObjectScopes() throws Exception {
        checkFooBoxIsOk();
    }
}
