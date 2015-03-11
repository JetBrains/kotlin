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

package org.jetbrains.kotlin.js.test;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.test.utils.DirectiveTestUtils;
import org.jetbrains.kotlin.js.test.utils.JsTestUtils;

public abstract class AbstractSingleFileTranslationWithDirectivesTest extends SingleFileTranslationTest {
    public AbstractSingleFileTranslationWithDirectivesTest(@NotNull String main) {
        super(main);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void processJsProgram(@NotNull JsProgram program) throws Exception {
        String fileName = getInputFilePath(getTestName(true) + ".kt");
        String fileText = JsTestUtils.readFile(fileName);
        DirectiveTestUtils.processDirectives(program, fileText);
    }
}
