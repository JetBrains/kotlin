/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
            verifyException("\"" + firstUnknownSymbolEncountered + "\"", e.getMessage());
        }
        catch (IllegalStateException e) {
            verifyException(firstUnknownSymbolEncountered, e.getMessage());
        }
    }

    private static void verifyException(@NotNull String firstUnknownSymbolEncountered, @NotNull String message) {
        String expectedErrorMessage = "ReferenceError: " + firstUnknownSymbolEncountered + " is not defined";
        assertTrue("Unexpected error when running compiled canvas examples with rhino.\n" +
                   "Expected message contains \"" + expectedErrorMessage + "\".\n" +
                   "Message: " + message,
                   message.contains(expectedErrorMessage));
    }
}
