/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

/**
 * @author Pavel Talanov
 */
public final class StandardClassesTest extends SingleFileTranslationTest {

    public StandardClassesTest() {
        super("standardClasses/");
    }

    public void testArray() throws Exception {
        fooBoxTest();
    }


    public void testArrayAccess() throws Exception {
        fooBoxTest();
    }


    public void testArrayIsFilledWithNulls() throws Exception {
        fooBoxTest();
    }


    public void testArrayFunctionConstructor() throws Exception {
        fooBoxTest();
    }


    public void testArraySize() throws Exception {
        fooBoxTest();
    }

    //TODO: this feature in not supported for some time
    //TODO: support it. Probably configurable.
    //    (expected = JavaScriptException.class)
    //    public void arrayThrowsExceptionOnOOBaccess() throws Exception {
    //        fooBoxTest();
    //    }


    //TODO: fails on ecma 5 because of ArrayIterator declaration: ecma 5 expects hasNext to be property while it is a function
    public void testArraysIterator() throws Exception {
        fooBoxTest(failOnEcma5());
    }
}