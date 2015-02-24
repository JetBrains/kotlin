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

public class EnumTest extends SingleFileTranslationTest {
    public EnumTest() {
        super("enum/");
    }

    public void testSimpleEnum() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnumWithInheritance() throws Exception {
        checkFooBoxIsOk();
    }

    public void testStandardMethods() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAccessing() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnumInheritedFromTrait() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSuperCallInEnumLiteral() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnumIsComparable() throws Exception {
        checkFooBoxIsOk();
    }

    public void testNativeEnum() throws Exception {
        checkFooBoxIsOk();
    }
}
