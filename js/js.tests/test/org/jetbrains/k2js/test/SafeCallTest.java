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
public final class SafeCallTest extends TranslationTest {

    final private static String MAIN = "safeCall/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testSafeAccess() throws Exception {
        checkFooBoxIsTrue("safeAccess.kt");
    }

    public void testSafeExtensionFunctionCall() throws Exception {
        checkFooBoxIsOk("safeExtensionFunctionCall.kt");
    }

    public void testSafeCall() throws Exception {
        checkFooBoxIsTrue("safeCall.kt");
    }

    public void testSafeCallReturnsNullIfFails() throws Exception {
        checkFooBoxIsTrue("safeCallReturnsNullIfFails.kt");
    }
}
