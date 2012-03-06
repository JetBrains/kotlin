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

import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.test.semantics.TranslatorTestCaseBuilder;

/**
 * @author Pavel Talanov
 */
@SuppressWarnings("JUnitTestCaseWithNoTests")
public abstract class SuiteBuilder {

    public interface Tester {
        void performTest(@NotNull SingleFileTranslationTest test, @NotNull String filename) throws Exception;
    }

    @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
    private static class SingleFileTest extends UsefulTestCase {

        @NotNull
        private final Tester tester;

        @NotNull
        private final String name;

        @NotNull
        private final SingleFileTranslationTest actualTest;

        public SingleFileTest(@NotNull SingleFileTranslationTest actualTest, @NotNull String name, @NotNull Tester tester) {
            setName(name);
            this.tester = tester;
            this.name = name;
            this.actualTest = actualTest;
        }

        public void runTest() throws Exception {
            tester.performTest(actualTest, name);
        }
    }

    public static Test suiteForTestClass(@NotNull final SingleFileTranslationTest actualTest,
                                         @NotNull final SuiteBuilder.Tester testMethod) throws Exception {
        //TODO: tearDown not called
        actualTest.setUp();
        return TranslatorTestCaseBuilder.suiteForDirectory(actualTest.getInputPath(),
                                                           true,
                                                           new TranslatorTestCaseBuilder.NamedTestFactory() {
                                                               @NotNull
                                                               @Override
                                                               public Test createTest(@NotNull String name) {
                                                                   return new SingleFileTest(actualTest, name, testMethod);
                                                               }
                                                           });
    }
}
