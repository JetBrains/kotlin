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

public final class RangeTest extends SingleFileTranslationTest {

    public RangeTest() {
        super("range/");
    }

    public void testCreatingProgressions() throws Exception {
        fooBoxTest();
    }

    public void testExplicitRange() throws Exception {
        fooBoxTest();
    }


    public void testRangeSugarSyntax() throws Exception {
        fooBoxTest();
    }


    public void testIntInRange() throws Exception {
        fooBoxTest();
    }

    public void testIteratingOverRanges() throws Exception {
        fooBoxTest();
    }

    public void testIntUpTo() throws Exception {
        fooBoxTest();
    }

    public void testRangeToDoesNotIterate() throws Exception {
        fooBoxTest();
    }

    public void testRangeEquals() throws Exception {
        checkFooBoxIsOk();
    }

    public void testIntDownTo() throws Exception {
        checkFooBoxIsOk();
    }

    public void testReverse() throws Exception {
        checkFooBoxIsOk();
    }
}
