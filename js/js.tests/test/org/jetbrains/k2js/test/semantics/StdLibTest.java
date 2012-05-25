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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.SingleFileTranslationTest;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class StdLibTest extends SingleFileTranslationTest {

    public StdLibTest() {
        super("stdlib/");
    }

    public void testDummy() {
    }

    public void TODO_testBrowserDocumentAccess() throws Exception {
        runFunctionOutputTest("browserDocumentAccess.kt", "test.browser", "foo", "Hello World!");
    }

    @Override
    protected void generateJavaScriptFiles(@NotNull String kotlinFilename,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull EnumSet<EcmaVersion> ecmaVersions) throws Exception {

        String stdlibdir = pathToTestFiles() + "../../../../libraries/stdlib/src/";
        String jscoredir = pathToTestFiles() + "../../../js.libraries/src/core/";

        String dom = jscoredir + "dom.kt";
        String jsbrowser = jscoredir + "../stdlib/browser.kt";
        String stdlibBrowser = stdlibdir + "kotlin/browser/Properties.kt";

        List<String> files = Arrays.asList(getInputFilePath(kotlinFilename), dom, jsbrowser);

        generateJavaScriptFiles(files, kotlinFilename, mainCallParameters, ecmaVersions);
    }

    @Override
    protected boolean shouldCreateOut() {
        return false;
    }
}
