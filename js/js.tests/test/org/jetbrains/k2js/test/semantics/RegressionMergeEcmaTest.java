/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

public class RegressionMergeEcmaTest extends SingleFileTranslationTest {
    public RegressionMergeEcmaTest() {
        super("mergeEcma/");
    }

    public void testObjectWithMethods() throws Exception {
        fooBoxTest();
    }

    public void testEcmaDelegate() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEcmaObject() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEcmaSimple() throws Exception {
        checkFooBoxIsOk();
    }

    public void testMethodOverride() throws Exception {
        checkFooBoxIsOk();
    }

    public void testMethodInClass() throws Exception {
        checkFooBoxIsOk();
    }

    public void testGetSetProperty() throws Exception {
        checkFooBoxIsOk();
    }

    public void testRootValInit() throws Exception {
        checkFooBoxIsOk();
    }

    //TODO
    public void TestRootPackageValInit() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClassInitializer() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionClass() throws Exception {
        checkFooBoxIsOk();
    }
}
