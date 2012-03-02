/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class StandardClassesTest extends TranslationTest {

    public StandardClassesTest() {
        super("standardClasses/");
    }

    public void testArray() throws Exception {
        checkFooBoxIsTrue("array.kt");
    }


    public void testArrayAccess() throws Exception {
        checkFooBoxIsTrue("arrayAccess.kt");
    }


    public void testArrayIsFilledWithNulls() throws Exception {
        checkFooBoxIsTrue("arrayIsFilledWithNulls.kt");
    }


    public void testArrayFunctionConstructor() throws Exception {
        checkFooBoxIsTrue("arrayFunctionConstructor.kt");
    }


    public void testArraySize() throws Exception {
        checkFooBoxIsTrue("arraySize.kt");
    }

    //TODO: this feature in not supported for some time
    //TODO: support it. Probably configurable.
//    (expected = JavaScriptException.class)
//    public void arrayThrowsExceptionOnOOBaccess() throws Exception {
//        checkFooBoxIsTrue("arrayThrowsExceptionOnOOBaccess.kt");
//    }


    public void testArraysIterator() throws Exception {
        checkFooBoxIsTrue("arraysIterator.kt");
    }
}
