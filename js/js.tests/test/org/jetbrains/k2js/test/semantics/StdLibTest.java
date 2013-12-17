/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.test.rhino.RhinoFunctionNativeObjectResultChecker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StdLibTest extends SingleFileTranslationTest {

    public StdLibTest() {
        super("stdlib/");
    }

    public void testBrowserDocumentAccessCompiles() throws Exception {
        generateJavaScriptFiles("browserDocumentAccess.kt", MainCallParameters.noCall(), DEFAULT_ECMA_VERSIONS);
    }

    @Override
    protected void generateJavaScriptFiles(@NotNull String kotlinFilename,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull Iterable<EcmaVersion> ecmaVersions) throws Exception {
        List<String> files = Arrays.asList(getInputFilePath(kotlinFilename));

        generateJavaScriptFiles(files, kotlinFilename, mainCallParameters, ecmaVersions);
        runRhinoTests(kotlinFilename, ecmaVersions,
                      new RhinoFunctionNativeObjectResultChecker("test.browser", TEST_FUNCTION, "Some Dynamically Created Content!!!"));
    }

    @Override
    protected Map<String, Object> getRhinoTestVariables() throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("root");
        //root.setIdAttribute("foo", true);
        root.setAttribute("id", "foo");
        root.setIdAttribute("id", true);
        document.appendChild(root);

        // lets test it actually works
        Element foo = document.getElementById("foo");
        assertNotNull(foo);

        Map<String, Object> answer = new HashMap<String, Object>();
        answer.put("document", document);
        answer.put("Node", new DummyNode());
        return answer;
    }

    //class cannot be private because Rhino won't be able to access it then
    @SuppressWarnings({"FieldMayBeStatic", "UnusedDeclaration"})
    public static class DummyNode {
        public final short ELEMENT_NODE = Node.ELEMENT_NODE;
        public final short TEXT_NODE = Node.TEXT_NODE;
    }
}
