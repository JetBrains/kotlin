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
 *         <p/>
 *         This class contains tests that do not fall in any particular category
 *         most probably because that functionality has very little support
 */
public final class MiscTest extends AbstractExpressionTest {
    final private static String MAIN = "misc/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testLocalPropertys() throws Exception {
        testFunctionOutput("localProperty.jet", "foo", "box", 50);
    }

    public void testIntRange() throws Exception {
        // checkOutput("intRange.kt", " ");
        checkFooBoxIsTrue("intRange.kt");
    }


    public void testSafecallComputesExpressionOnlyOnce() throws Exception {
        checkFooBoxIsTrue("safecallComputesExpressionOnlyOnce.kt");
    }
}
