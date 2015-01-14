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

package org.jetbrains.kotlin.js.test.semantics;

import org.mozilla.javascript.JavaScriptException;

public final class ArrayListTest extends JavaClassesTest {

    public ArrayListTest() {
        super("arrayList/");
    }

    public void testEmptyList() throws Exception {
        fooBoxTest();
    }

    public void testAccess() throws Exception {
        fooBoxTest();
    }

    public void testIsEmpty() throws Exception {
        fooBoxTest();
    }

    public void testArrayAccess() throws Exception {
        fooBoxTest();
    }

    public void testIterate() throws Exception {
        fooBoxTest();
    }

    public void testRemove() throws Exception {
        fooBoxTest();
    }

    public void testRemoveWithIndexOutOfBounds() throws Exception {
        fooBoxTest();
    }

    public void testToArray() throws Exception {
        fooBoxTest();
    }

    public void testIndexOOB() throws Exception {
        try {
            fooBoxTest();
            fail();
        } catch (JavaScriptException e) {
            // Ignored
        }
    }

    public void testMisc() throws Exception {
        fooBoxTest();
    }

    public void testConstructWithCapacity() throws Exception {
        fooBoxTest();
    }

    public void testConstructWithSideEffectParam() throws Exception {
        fooBoxTest();
    }

    public void testIndexOf() throws Exception {
        checkFooBoxIsOk();
    }

    public void testContainsAll() throws Exception {
        checkFooBoxIsOk();
    }

    public void testRemoveAll() throws Exception {
        checkFooBoxIsOk();
    }

    public void testRetainAll() throws Exception {
        checkFooBoxIsOk();
    }
}
