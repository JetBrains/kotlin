/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import org.jetbrains.k2js.test.SingleFileTranslationTest;

/**
 * @author Pavel Talanov
 */
public final class PropertyAccessTest extends SingleFileTranslationTest {

    public PropertyAccessTest() {
        super("propertyAccess/");
    }

    public void testAccessToInstanceProperty() throws Exception {
        checkFooBoxIsTrue("accessToInstanceProperty.kt");
    }


    public void testTwoClassesWithProperties() throws Exception {
        checkFooBoxIsTrue("twoClassesWithProperties.kt");
    }


    public void testSetter() throws Exception {
        runFunctionOutputTest("setter.kt", "foo", "f", 99.0);
    }


    public void testCustomGetter() throws Exception {
        checkFooBoxIsTrue("customGetter.kt");
    }


    public void testCustomSetter() throws Exception {
        checkFooBoxIsTrue("customSetter.kt");
    }

    public void testNamespacePropertyInitializer() throws Exception {
        checkFooBoxIsTrue("namespacePropertyInitializer.kt");
    }


    public void testNamespacePropertySet() throws Exception {
        checkFooBoxIsTrue("namespacePropertySet.kt");
    }

    public void testNamespaceCustomAccessors() throws Exception {
        checkFooBoxIsTrue("namespaceCustomAccessors.kt");
    }


    public void testClassUsesNamespaceProperties() throws Exception {
        checkFooBoxIsTrue("classUsesNamespaceProperties.kt");
    }

    public void testExtensionLiteralSafeCall() throws Exception {
        checkFooBoxIsTrue("extensionLiteralSafeCall.kt");
    }

}
