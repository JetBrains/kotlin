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

package org.jetbrains.k2js.test.utils;

import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.test.TranslationTest;
import org.jetbrains.k2js.test.TranslatorTestCaseBuilder;

/**
 * @author Pavel Talanov
 */
public abstract class SingleFileTest extends TranslationTest {

    @NotNull
    private final Tester tester;
    @NotNull
    private final String name;

    protected SingleFileTest(@NotNull String pathToMain, @NotNull String name, @NotNull Tester tester) {
        super(pathToMain);
        setName(name);
        this.tester = tester;
        this.name = name;
    }

    public interface Tester {
        void performTest(@NotNull TranslationTest test, @NotNull String filename) throws Exception;
    }

    public void runTest() throws Exception {
        tester.performTest(this, name);
    }

    public static Test suiteForDirectory(@NotNull final String main, @NotNull final SingleFileTest.Tester testMethod) {

        return TranslatorTestCaseBuilder.suiteForDirectory(TEST_FILES + main + casesDirectoryName(),
                                                           true,
                                                           new TranslatorTestCaseBuilder.NamedTestFactory() {
                                                               @NotNull
                                                               @Override
                                                               public Test createTest(@NotNull String name) {
                                                                   return (new SingleFileTest(main, name, testMethod) {
                                                                   });
                                                               }
                                                           });
    }
}
