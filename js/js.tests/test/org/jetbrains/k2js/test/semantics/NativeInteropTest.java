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

import java.util.List;

public final class NativeInteropTest extends SingleFileTranslationTest {

    @NotNull
    private static final String NATIVE = "native/";

    public NativeInteropTest() {
        super(NATIVE);
    }

    @NotNull
    @Override
    protected List<String> additionalJSFiles(@NotNull EcmaVersion ecmaVersion) {
        List<String> result = Lists.newArrayList(super.additionalJSFiles(ecmaVersion));
        result.add(pathToTestFiles() + NATIVE + "/" + getTestName(true) + ".js");
        //result.addAll(JsTestUtils.getAllFilesInDir(pathToTestFiles() + NATIVE));
        return result;
    }

    public void testSimple() throws Exception {
        fooBoxTest();
    }

    public void testInheritanceFromNativeClass() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClass() throws Exception {
        fooBoxTest();
    }

    public void testVararg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testUndefined() throws Exception {
        fooBoxTest();
    }

    public void testKt1519() throws Exception {
        fooBoxTest();
    }

    public void testClassObject() throws Exception {
        fooBoxTest();
    }

    public void testSimpleUndefined() throws Exception {
        fooBoxTest();
    }

    public void testLibrary() throws Exception {
        fooBoxTest();
    }

    public void testKt2209() throws Exception {
        fooBoxTest();
    }

    public void testKt2323() throws Exception {
        fooBoxTest();
    }

    public void testNativePropertyWithCustomName() throws Exception {
        checkFooBoxIsOk();
    }
}
