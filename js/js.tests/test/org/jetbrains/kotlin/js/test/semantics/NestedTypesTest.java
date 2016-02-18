/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.js.JavaScript;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.test.SingleFileTranslationTest;

import java.io.File;
import java.util.List;

public class NestedTypesTest extends SingleFileTranslationTest {
    public NestedTypesTest() {
        super("nestedTypes/");
    }

    public void testNested() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInner() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOuterThis() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOuterCompanion() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOuterObject() throws Exception {
        checkFooBoxIsOk();
    }

    public void testNestedNative() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOuterNative() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInnerReferenceFromChild() throws Exception {
        checkFooBoxIsOk();
    }

    @NotNull
    @Override
    protected List<String> additionalJsFiles(@NotNull EcmaVersion ecmaVersion) {
        List<String> result = Lists.newArrayList(super.additionalJsFiles(ecmaVersion));

        String jsFilePath = pathToTestDir() + "native/" + getTestName(true) + JavaScript.DOT_EXTENSION;
        File jsFile = new File(jsFilePath);
        if (jsFile.exists() && jsFile.isFile()) {
            result.add(jsFilePath);
        }

        return result;
    }
}
