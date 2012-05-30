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

import closurecompiler.internal.com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.mozilla.javascript.EcmaError;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel Talanov
 */
/*
* We can't really check that this examples work in non-browser environment, so we just check that examples compile and
* running them produce expected error.
* */
public final class WebDemoCanvasExamplesTest extends SingleFileTranslationTest {

    private static final String CANVAS_HELPER = pathToTestFilesRoot() + "canvas_helper.js";

    public WebDemoCanvasExamplesTest() {
        super("webDemoCanvasExamples/");
    }

    public void testCreatures() throws Exception {
        doTest("Creatures.kt", "document");
    }

    public void testHelloKotlin() throws Exception {
        doTest("Hello, Kotlin.kt", "document");
    }

    public void testFancyLines() throws Exception {
        doTest("Fancy lines.kt", "$");
    }

    public void testTrafficLight() throws Exception {
        doTest("Traffic light.kt", "document");
    }

    private void doTest(@NotNull String filename, @NotNull String firstUnknownSymbolEncountered) throws Exception {
        try {
            checkOutput(filename, "");
            fail();
        }
        catch (EcmaError e) {
            String expectedErrorMessage = "ReferenceError: \"" + firstUnknownSymbolEncountered + "\" is not defined.";
            assertTrue("Unexpected error when running compiled canvas examples with rhino.\n" +
                       "Expected: " + expectedErrorMessage + "\n" +
                       "Actual: " + e.getMessage(),
                       e.getMessage().startsWith(expectedErrorMessage));
        }
    }

    @Override
    protected List<String> additionalJSFiles() {
        ArrayList<String> result = Lists.newArrayList(super.additionalJSFiles());
        result.add(CANVAS_HELPER);
        return result;
    }
}
