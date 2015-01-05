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

public final class TraitTest extends SingleFileTranslationTest {

    public TraitTest() {
        super("trait/");
    }

    public void testTraitAddsFunctionsToClass() throws Exception {
        fooBoxTest();
    }

    public void testClassDerivesFromClassAndTrait() throws Exception {
        fooBoxTest();
    }

    public void testClassDerivesFromTraitAndClass() throws Exception {
        fooBoxTest();
    }

    public void testExample() throws Exception {
        fooBoxTest();
    }

    public void testTraitExtendsTrait() throws Exception {
        fooBoxTest();
    }

    public void testTraitExtendsTwoTraits() throws Exception {
        fooBoxTest();
    }

    public void testFunDelegation() throws Exception {
        checkFooBoxIsOk("funDelegation.kt");
    }

    public void testDefinitionOrder() throws Exception {
        fooBoxTest();
    }

    public void testCheckImplementationCharacteristics() throws Exception {
        checkFooBoxIsOk();
    }
}
