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

public class DelegatePropertyTest extends SingleFileTranslationTest {
    public DelegatePropertyTest() {
        super("delegateProperty/");
    }

    public void testSimple() throws Exception {
        checkFooBoxIsOk();
    }

    public void testPropertyMetadata() throws Exception {
        checkFooBoxIsOk();
    }

    public void testWithGenerics() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegateWithPropertyAccess() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegateByTopLevelFun() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegateByTopLevelProperty() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegateByExtensionProperty() throws Exception {
        checkFooBoxIsOk();
    }

    public void testGetAsExtensionFun() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSetAsExtensionFun() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTopLevelVal() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTopLevelVar() throws Exception {
        checkFooBoxIsOk();
    }

}
