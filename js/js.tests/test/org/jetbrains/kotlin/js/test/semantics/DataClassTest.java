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

import org.jetbrains.kotlin.js.test.SingleFileTranslationTest;

public class DataClassTest extends SingleFileTranslationTest {
    public DataClassTest() {
        super("dataClass/");
    }

    public void testComponents() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCopy() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEquals() throws Exception {
        checkFooBoxIsOk();
    }

    public void testHashcode() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTostring() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOverride() throws Exception {
        checkFooBoxIsOk();
    }

    public void testKeyrole() throws Exception {
        checkFooBoxIsOk();
    }
}
