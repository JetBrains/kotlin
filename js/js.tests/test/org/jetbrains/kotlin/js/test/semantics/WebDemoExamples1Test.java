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

import org.jetbrains.kotlin.js.test.SingleFileTranslationTest;

public final class WebDemoExamples1Test extends SingleFileTranslationTest {

    public WebDemoExamples1Test() {
        super("webDemoExamples1/");
    }

    public void testPrintArg() throws Exception {
        checkOutput("printArg.kt", "Hello, world!", "Hello, world!");
    }


    public void testWhileLoop() throws Exception {
        checkOutput("whileLoop.kt", "guest1\nguest2\nguest3\nguest4\n", "guest1", "guest2", "guest3", "guest4");
    }


    public void testIfAsExpression() throws Exception {
        checkOutput("ifAsExpression.kt", "20\n", "10", "20");
    }


    public void testObjectOrientedHello() throws Exception {
        checkOutput("objectOrientedHello.kt", "Hello, Pavel!\n", "Pavel");
    }


    public void testMultiLanguageHello() throws Exception {
        checkOutput("multiLanguageHello.kt", "Salut!\n", "FR");
    }


    public void testNullChecks() throws Exception {
        checkOutput("nullChecks.kt", "No number supplied");
        checkOutput("nullChecks.kt", "6", "2", "3");
    }


    public void testRanges() throws Exception {
        checkOutput("ranges.kt", "OK\n" +
                                 " 1 2 3 4 5\n" +
                                 "Out: array has only 3 elements. x = 4\n" +
                                 "Yes: array contains aaa\n" +
                                 "No: array doesn't contains ddd\n", "4");

        checkOutput("ranges.kt", " 1 2 3 4 5\n" +
                                 "Out: array has only 3 elements. x = 10\n" +
                                 "Yes: array contains aaa\n" +
                                 "No: array doesn't contains ddd\n", "10");
    }


    public void testForLoop() throws Exception {
        checkOutput("forLoop.kt", "a\n" +
                                  "b\n" +
                                  "c\n" +
                                  "\n" +
                                  "a\n" +
                                  "b\n" +
                                  "c\n", "a", "b", "c");
        checkOutput("forLoop.kt", "123\n\n123\n", "123");
    }


    public void testIsCheck() throws Exception {
        checkOutput("isCheck.kt", "3\nnull\n");
    }


    public void testPatternMatching() throws Exception {
        checkOutput("patternMatching.kt", "Greeting\n" +
                                          "One\n" +
                                          "Not a string\n" +
                                          "Unknown\n");
    }
}
