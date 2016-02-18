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

public final class EqualsTest extends AbstractExpressionTest {

    public EqualsTest() {
        super("equals/");
    }

    public void testCustomEqualsMethodOnAny() throws Exception {
        fooBoxTest();
    }

    public void testCustomEqualsMethod() throws Exception {
        fooBoxTest();
    }

    public void testExplicitEqualsMethod() throws Exception {
        fooBoxTest();
    }

    public void testExplicitEqualsMethodForPrimitives() throws Exception {
        fooBoxTest();
    }

    public void testStringsEqual() throws Exception {
        fooBoxTest();
    }

    public void testKt2370() throws Exception {
        fooBoxTest();
    }

    public void testEqualsNullOrUndefined() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCompareNullableListWithNull() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCompareToNullWithCustomEquals() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCompareNullablesWithCustomEquals() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEqualsBehaviorOnNull() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSuperEquals() throws Exception {
        checkFooBoxIsOk();
    }
}
