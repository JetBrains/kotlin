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

package org.jetbrains.k2js.test.semantics;

import com.google.dart.compiler.backend.js.ast.JsNode;
import com.intellij.util.Consumer;
import org.jetbrains.k2js.test.MultipleFilesTranslationTest;
import org.jetbrains.k2js.test.utils.DirectiveTestUtils;
import org.jetbrains.k2js.test.utils.JsTestUtils;
import org.jetbrains.k2js.test.utils.MemoizeConsumer;

import java.util.List;

import static org.jetbrains.k2js.test.utils.JsTestUtils.getAllFilesInDir;

public final class InlineMultiFileTest extends MultipleFilesTranslationTest {
    private final MemoizeConsumer<JsNode> nodeConsumer = new MemoizeConsumer<JsNode>();

    public InlineMultiFileTest() {
        super("inlineMultiFile/");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        nodeConsumer.consume(null);
    }

    public void testInlineMultiFileSimple() throws Exception {
        checkFooBoxIsOk();
        processInlineDirectives();
    }

    public void testBuilders() throws Exception {
        checkFooBoxIsOk();
    }

    public void testBuildersAndLambdaCapturing() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAnonymousObjectOnCallSite() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAnonymousObjectOnCallSiteSuperParams() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAnonymousObjectOnDeclarationSite() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAnonymousObjectOnDeclarationSiteSuperParams() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTrait() throws Exception {
        checkFooBoxIsOk();
        processInlineDirectives();
    }

    public void testUse() throws Exception {
        checkFooBoxIsOk();
    }

    public void testWith() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTryCatch() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTryCatch2() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTryCatchFinally() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLambdaCloning() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLambdaInLambda2() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLambdaInLambdaNoInline() throws Exception {
        checkFooBoxIsOk();
    }

    public void testRegeneratedLambdaName() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSameCaptured() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureChain() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineInDefaultParameter() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDefaultMethod() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleDefaultMethod() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCaptureInlinable() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCaptureInlinableAndOther() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCaptureThisAndReceiver() throws Exception {
        checkFooBoxIsOk();
    }

    public void testGenerics() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleCapturingInClass() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleCapturingInPackage() throws Exception {
        checkFooBoxIsOk();
    }

    private void processInlineDirectives() throws Exception {
        String dir = getTestName(true);
        List<String> fileNames = getAllFilesInDir(getInputFilePath(dir));

        for (String fileName : fileNames) {
            String fileText = JsTestUtils.readFile(fileName);

            JsNode lastJsNode = nodeConsumer.getLastValue();
            assert lastJsNode != null;

            DirectiveTestUtils.processDirectives(lastJsNode, fileText);
        }
    }

    @Override
    protected Consumer<JsNode> getConsumer() {
        return nodeConsumer;
    }

}
