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

import junit.framework.Test;
import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
//TODO: this class has strange behaviour. Should be refactored.
public final class Suite extends TranslationTest {

    private String name;
    private final SingleFileTester tester;
    private final String testMain;

    public Suite(@NotNull String testName,
                 @NotNull String suiteDirName,
                 @NotNull final SingleFileTester tester) {
        this.name = testName;
        this.tester = tester;
        this.testMain = suiteDirName;
        setName(name);
    }

    public Suite() {
        this("dummy", "dummy", new SingleFileTester() {
            @Override
            public void performTest(@NotNull Suite test, @NotNull String filename) throws Exception {
                //do nothing
            }
        });
    }

    //NOTE: just to avoid warning
    public void testNothing() {
    }

    @Override
    protected boolean shouldCreateOut() {
        return false;
    }

    @Override
    protected String mainDirectory() {
        return testMain;
    }

    public void runTest() throws Exception {
        tester.performTest(this, name);
    }

    public static Test suiteForDirectory(@NotNull final String mainName, @NotNull final SingleFileTester testMethod) {

        return TranslatorTestCaseBuilder.suiteForDirectory(TranslationTest.TEST_FILES,
                                                           mainName + casesDirectoryName(),
                                                           true,
                                                           new TranslatorTestCaseBuilder.NamedTestFactory() {
                                                               @NotNull
                                                               @Override
                                                               public Test createTest(@NotNull String name) {
                                                                   return (new Suite(name, mainName, testMethod));
                                                               }
                                                           });
    }

    protected static interface SingleFileTester {
        void performTest(@NotNull Suite test, @NotNull String filename) throws Exception;
    }
}
