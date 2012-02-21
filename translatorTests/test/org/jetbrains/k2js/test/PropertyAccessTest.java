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
public final class PropertyAccessTest extends TranslationTest {

    final private static String MAIN = "propertyAccess/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testAccessToInstanceProperty() throws Exception {
        testFooBoxIsTrue("accessToInstanceProperty.kt");
    }


    public void testTwoClassesWithProperties() throws Exception {
        testFooBoxIsTrue("twoClassesWithProperties.kt");
    }


    public void testSetter() throws Exception {
        testFunctionOutput("setter.kt", "foo", "f", 99.0);
    }


    public void testCustomGetter() throws Exception {
        testFooBoxIsTrue("customGetter.kt");
    }


    public void testCustomSetter() throws Exception {
        testFooBoxIsTrue("customSetter.kt");
    }


    public void testSafeCall() throws Exception {
        testFooBoxIsTrue("safeCall.kt");
    }


    //TODO: place safecalls under distinkt category
    public void testSafeCallReturnsNullIfFails() throws Exception {
        testFooBoxIsTrue("safeCallReturnsNullIfFails.kt");
    }


    public void testNamespacePropertyInitializer() throws Exception {
        testFooBoxIsTrue("namespacePropertyInitializer.kt");
    }


    public void testNamespacePropertySet() throws Exception {
        testFooBoxIsTrue("namespacePropertySet.kt");
    }

    public void testNamespaceCustomAccessors() throws Exception {
        testFooBoxIsTrue("namespaceCustomAccessors.kt");
    }


    public void testClassUsesNamespaceProperties() throws Exception {
        testFooBoxIsTrue("classUsesNamespaceProperties.kt");
    }


    public void testSafeAccess() throws Exception {
        testFooBoxIsTrue("safeAccess.kt");
    }

    public void testSafeExtensionFunctionCall() throws Exception {
        testFooBoxIsOk("safeExtensionFunctionCall.kt");
    }

    public void testExtensionLiteralSafeCall() throws Exception {
        testFooBoxIsTrue("extensionLiteralSafeCall.kt");
    }

}
