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

import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.translate.context.Namer;

@SuppressWarnings("JUnitTestCaseWithNoTests")
public final class ExamplesTest extends SingleFileTranslationTest {

    @NotNull
    private final String filename;

    @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
    public ExamplesTest(@NotNull String filename) {
        super("examples/");
        this.filename = filename;
    }

    @Override
    public void runTest() throws Exception {
        runFunctionOutputTest(filename, Namer.getRootPackageName(), TEST_FUNCTION, "OK");
    }

    public static Test suite() throws Exception {
        return TranslatorTestCaseBuilder
                .suiteForDirectory(TEST_DATA_DIR_PATH + "examples/cases/", new TranslatorTestCaseBuilder.NamedTestFactory() {
                    @NotNull
                    @Override
                    public Test createTest(@NotNull String filename) {
                        ExamplesTest examplesTest = new ExamplesTest(filename);
                        examplesTest.setName(filename);
                        return examplesTest;
                    }
                });
    }
}
