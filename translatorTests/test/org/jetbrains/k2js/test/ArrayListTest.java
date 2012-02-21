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

import org.mozilla.javascript.JavaScriptException;

/**
 * @author Pavel Talanov
 */
public final class ArrayListTest extends JavaClassesTest {

    final private static String MAIN = "arrayList/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testEmptyList() throws Exception {
        testFooBoxIsTrue("emptyList.kt");
    }

    public void testAccess() throws Exception {
        testFooBoxIsTrue("access.kt");
    }

    public void testIsEmpty() throws Exception {
        testFooBoxIsTrue("isEmpty.kt");
    }

    public void testArrayAccess() throws Exception {
        testFooBoxIsTrue("arrayAccess.kt");
    }

    public void testIterate() throws Exception {
        testFooBoxIsTrue("iterate.kt");
    }

    public void testRemove() throws Exception {
        testFooBoxIsTrue("remove.kt");
    }

    public void testIndexOOB() throws Exception {
        try {
            testFooBoxIsTrue("indexOOB.kt");
            fail();
        } catch (JavaScriptException e) {

        }
    }
}
