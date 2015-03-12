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

public final class ClassObjectTest extends SingleFileTranslationTest {

    public ClassObjectTest() {
        super("classObject/");
    }

    public void testSimple() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInTrait() throws Exception {
        checkFooBoxIsOk();
    }

    public void testWithInheritance() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSetVar() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAccessing() throws Exception {
        checkFooBoxIsOk();
    }

    public void testNamedClassObject() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDefaultObjectSameNamesAsInOuter() throws Exception {
        checkFooBoxIsOk();
    }
}
