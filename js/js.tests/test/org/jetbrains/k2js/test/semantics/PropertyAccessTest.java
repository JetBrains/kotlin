/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.test.utils.JsTestUtils;

import java.util.List;

public final class PropertyAccessTest extends SingleFileTranslationTest {

    public PropertyAccessTest() {
        super("propertyAccess/");
    }

    public void testAccessToInstanceProperty() throws Exception {
        fooBoxTest();
    }


    public void testTwoClassesWithProperties() throws Exception {
        fooBoxTest();
    }

    public void testBackendField() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSetter() throws Exception {
        runFunctionOutputTest("setter.kt", "foo", "f", 99.0);
    }


    public void testCustomGetter() throws Exception {
        fooBoxTest();
    }


    public void testCustomSetter() throws Exception {
        fooBoxTest();
    }

    public void testNamespacePropertyInitializer() throws Exception {
        fooBoxTest();
    }


    public void testNamespacePropertySet() throws Exception {
        fooBoxTest();
    }

    public void testNamespaceCustomAccessors() throws Exception {
        fooBoxTest();
    }


    public void testClassUsesNamespaceProperties() throws Exception {
        fooBoxTest();
    }

    public void testExtensionLiteralSafeCall() throws Exception {
        fooBoxTest();
    }

    public void testInitInstanceProperties() throws Exception {
        fooBoxTest(JsTestUtils.successOnEcmaV5());
    }

    public void testEnumerable() throws Exception {
        fooBoxTest(JsTestUtils.successOnEcmaV5());
    }

    public void testOverloadedOverriddenFunctionPropertyName() throws Exception {
        fooBoxTest(JsTestUtils.successOnEcmaV5());
    }

    @Override
    @NotNull
    protected List<String> additionalJSFiles(@NotNull EcmaVersion ecmaVersion) {
        List<String> result = Lists.newArrayList(super.additionalJSFiles(ecmaVersion));
        if (getName().equals("testEnumerable")) {
            result.add(pathToTestFiles() + "enumerate.js");
        }
        return result;
    }
}
