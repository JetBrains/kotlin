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

package org.jetbrains.k2js.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.rhino.RhinoFunctionResultChecker;

import java.util.List;

import static org.jetbrains.k2js.test.utils.JsTestUtils.getAllFilesInDir;

public abstract class MultipleFilesTranslationTest extends BasicTest {

    public MultipleFilesTranslationTest(@NotNull String main) {
        super(main);
    }

    protected void generateJsFromDir(@NotNull String dirName, @NotNull Iterable<EcmaVersion> ecmaVersions) throws Exception {
        List<String> fullFilePaths = getAllFilesInDir(getInputFilePath(dirName));
        generateJavaScriptFiles(fullFilePaths, dirName, MainCallParameters.noCall(), ecmaVersions);
    }

    protected void runMultiFileTest(@NotNull String dirName, @NotNull String packageName,
            @NotNull String functionName, @NotNull Object expectedResult) throws Exception {
        runMultiFileTests(DEFAULT_ECMA_VERSIONS, dirName, packageName, functionName, expectedResult);
    }

    protected void runMultiFileTests(
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull String dirName,
            @NotNull String packageName,
            @NotNull String functionName,
            @NotNull Object expectedResult
    ) throws Exception {
        generateJsFromDir(dirName, ecmaVersions);
        runRhinoTests(dirName + ".kt", ecmaVersions, new RhinoFunctionResultChecker(TEST_MODULE, packageName, functionName, expectedResult));
    }

    public void checkFooBoxIsTrue(@NotNull String dirName) throws Exception {
        runMultiFileTest(dirName, TEST_PACKAGE, TEST_FUNCTION, true);
    }

    public void checkFooBoxIsOk() throws Exception {
        String dir = getTestName(true);
        runMultiFileTest(dir, TEST_PACKAGE, TEST_FUNCTION, "OK");
    }
}

