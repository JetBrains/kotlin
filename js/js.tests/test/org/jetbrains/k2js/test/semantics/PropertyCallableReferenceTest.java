/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.test.semantics;

public class PropertyCallableReferenceTest extends AbstractCallableReferenceTest {

    public PropertyCallableReferenceTest() {
        super("property/");
    }

    public void testTopLevelVar() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionProperty() throws Exception {
        checkFooBoxIsOk();
    }

    public void testMemberProperty() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAccessViaSubclass() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegated() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDelegatedMutable() throws Exception {
        checkFooBoxIsOk();
    }

    public void testKClassInstanceIsInitializedFirst() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOverriddenInSubclass() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleExtension() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleMember() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleMutableExtension() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleMutableMember() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleMutableTopLevel() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleTopLevel() throws Exception {
        checkFooBoxIsOk();
    }
}
