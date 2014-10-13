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

package org.jetbrains.k2js.test;

import com.google.dart.compiler.backend.js.ast.JsNode;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.test.utils.InlineTestUtils;
import org.jetbrains.k2js.test.utils.JsTestUtils;
import org.jetbrains.k2js.test.utils.MemoizeConsumer;

public abstract class SingleFileTranslationWithDirectivesTest extends SingleFileTranslationTest {
    private final MemoizeConsumer<JsNode> nodeConsumer = new MemoizeConsumer<JsNode>();

    public SingleFileTranslationWithDirectivesTest(@NotNull String main) {
        super(main);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        nodeConsumer.consume(null);
    }

    protected void checkFooBoxIsOkWithInlineDirectives() throws Exception {
        checkFooBoxIsOk();
        processInlineDirectives();
    }

    protected void processInlineDirectives() throws Exception {
        String fileName = getInputFilePath(getTestName(true) + ".kt");
        String fileText = JsTestUtils.readFile(fileName);

        JsNode lastJsNode = nodeConsumer.getLastValue();
        assert lastJsNode != null;

        InlineTestUtils.processDirectives(lastJsNode, fileText);
    }

    @Override
    protected Consumer<JsNode> getConsumer() {
        return nodeConsumer;
    }
}
