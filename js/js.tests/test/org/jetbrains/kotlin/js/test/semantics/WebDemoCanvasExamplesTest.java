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

import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

/*
* We can't really check that this examples work in non-browser environment, so we just check that examples compile and
* running them produce expected error.
* */
public final class WebDemoCanvasExamplesTest extends AbstractWebDemoExamplesTest {

    public WebDemoCanvasExamplesTest() {
        super("webDemoCanvasExamples/");
    }

    public void testCreatures() throws Exception {
        doTest("Creatures.kt", "$");
    }

    public void testHelloKotlin() throws Exception {
        doTest("Hello, Kotlin.kt", "$");
    }

    public void testFancyLines() throws Exception {
        doTest("Fancy lines.kt", "$");
    }

    public void testTrafficLight() throws Exception {
        doTest("Traffic light.kt", "window");
    }

    private void doTest(@NotNull String filename, @NotNull String firstUnknownSymbolEncountered) throws Exception {
        try {
            runMainAndCheckOutput(filename, "");
            fail();
        }
        catch (ScriptException e) {
            String expectedErrorMessage = "ReferenceError: \"" + firstUnknownSymbolEncountered + "\" is not defined";
            assertTrue("Unexpected error when running compiled canvas examples with rhino.\n" +
                       "Expected: " + expectedErrorMessage + "\n" +
                       "Actual: " + e.getMessage(),
                       e.getMessage().startsWith(expectedErrorMessage));
        }
    }
}
