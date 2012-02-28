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
public final class TraitTest extends TranslationTest {

    final private static String MAIN = "trait/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testTraitAddsFunctionsToClass() throws Exception {
        checkFooBoxIsTrue("traitAddsFunctionsToClass.kt");
    }


    public void testClassDerivesFromClassAndTrait() throws Exception {
        checkFooBoxIsTrue("classDerivesFromClassAndTrait.kt");
    }


    public void testClassDerivesFromTraitAndClass() throws Exception {
        checkFooBoxIsTrue("classDerivesFromTraitAndClass.kt");
    }


    public void testExample() throws Exception {
        checkFooBoxIsTrue("example.kt");
    }


    public void testTraitExtendsTrait() throws Exception {
        checkFooBoxIsTrue("traitExtendsTrait.kt");
    }


    public void testTraitExtendsTwoTraits() throws Exception {
        checkFooBoxIsTrue("traitExtendsTwoTraits.kt");
    }


    public void testFunDelegation() throws Exception {
        checkFooBoxIsOk("funDelegation.jet");
    }


    public void testDefinitionOrder() throws Exception {
        checkFooBoxIsTrue("definitionOrder.kt");
    }
}
