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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.test.SingleFileTranslationTest;

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

    public void testSetter() throws Exception {
        checkFooBoxIsOk();
    }


    public void testCustomGetter() throws Exception {
        fooBoxTest();
    }


    public void testCustomSetter() throws Exception {
        fooBoxTest();
    }

    public void testPackagePropertyInitializer() throws Exception {
        fooBoxTest();
    }


    public void testPackagePropertySet() throws Exception {
        fooBoxTest();
    }

    public void testPackageCustomAccessors() throws Exception {
        fooBoxTest();
    }


    public void testClassUsesPackageProperties() throws Exception {
        fooBoxTest();
    }

    public void testExtensionLiteralSafeCall() throws Exception {
        fooBoxTest();
    }

    public void testInitInstanceProperties() throws Exception {
        fooBoxTest();
    }

    public void testInitValInConstructor() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnumerable() throws Exception {
        fooBoxTest();
    }

    public void testOverloadedOverriddenFunctionPropertyName() throws Exception {
        fooBoxTest();
    }

    public void testNativePropertiesNameClashes() throws Exception {
        checkFooBoxIsOk();
    }

    public void testField() throws Exception {
        checkFooBoxIsOk();
    }

    public void testPropertyAssignment() throws Exception {
        checkFooBoxIsOk();
    }

    public void testPrivatePropertyAccessFromMethod() throws Exception {
        checkFooBoxIsOk();
    }

    @Override
    @NotNull
    protected List<String> additionalJsFiles(@NotNull EcmaVersion ecmaVersion) {
        List<String> result = Lists.newArrayList(super.additionalJsFiles(ecmaVersion));
        if (getName().equals("testEnumerable")) {
            result.add(pathToTestDir() + "enumerate.js");
        }
        return result;
    }
}
