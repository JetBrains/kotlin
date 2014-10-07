/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.test.rhino.RhinoResultChecker;

import java.io.File;
import java.util.List;

// TODO: should be dropped with derived classes?
abstract class StdLibTestBase extends SingleFileTranslationTest {
    protected StdLibTestBase() {
        super("stdlib/");
    }

    @Nullable
    protected abstract RhinoResultChecker getResultChecker();

    protected void performStdLibTest(
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull String sourceDir,
            @NotNull String... stdLibFiles
    ) throws Exception {
        List<String> files = filesFromDir(sourceDir, stdLibFiles);
        String testFileName = getTestName(true) + ".kt";

        generateJavaScriptFiles(files, testFileName, MainCallParameters.noCall(), ecmaVersions);

        RhinoResultChecker checker = getResultChecker();
        if (checker != null) {
            runRhinoTests(testFileName, ecmaVersions, checker);
        }
    }

    @NotNull
    private static List<String> filesFromDir(@NotNull String sourceDir, @NotNull String[] stdLibFiles) {
        List<String> files = Lists.newArrayList();
        File stdlibDir = new File(sourceDir);
        assertTrue("Cannot find stdlib source: " + stdlibDir, stdlibDir.exists());
        for (String file : stdLibFiles) {
            files.add(new File(stdlibDir, file).getPath());
        }
        return files;
    }
}
