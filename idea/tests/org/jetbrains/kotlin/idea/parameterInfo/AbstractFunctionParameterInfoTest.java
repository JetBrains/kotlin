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

package org.jetbrains.kotlin.idea.parameterInfo;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetValueArgumentList;

public abstract class AbstractFunctionParameterInfoTest extends LightCodeInsightFixtureTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/parameterInfo/functionParameterInfo");
    }

    protected void doTest(String fileName) {
        myFixture.configureByFile(fileName);
        JetFile file = (JetFile) myFixture.getFile();
        PsiElement lastChild = file.getLastChild();
        assert lastChild != null;
        String expectedResultText = null;
        if (lastChild.getNode().getElementType().equals(JetTokens.BLOCK_COMMENT)) {
            String lastChildText = lastChild.getText();
            expectedResultText = lastChildText.substring(2, lastChildText.length() - 2).trim();
        }
        else if (lastChild.getNode().getElementType().equals(JetTokens.EOL_COMMENT)) {
            expectedResultText = lastChild.getText().substring(2).trim();
        }
        assert expectedResultText != null;
        JetFunctionParameterInfoHandler parameterInfoHandler = new JetFunctionParameterInfoHandler();
        MockCreateParameterInfoContext mockCreateParameterInfoContext = new MockCreateParameterInfoContext(file, myFixture);
        JetValueArgumentList parameterOwner = parameterInfoHandler.findElementForParameterInfo(mockCreateParameterInfoContext);

        MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(file, myFixture);

        //to update current parameter index
        JetValueArgumentList elementForUpdating = parameterInfoHandler.findElementForUpdatingParameterInfo(updateContext);
        if (elementForUpdating != null) {
            parameterInfoHandler.updateParameterInfo(elementForUpdating, updateContext);
        }

        MockParameterInfoUIContext parameterInfoUIContext =
                new MockParameterInfoUIContext(parameterOwner, updateContext.getCurrentParameter());

        for (Object item : mockCreateParameterInfoContext.getItemsToShow()) {
            //noinspection unchecked
            parameterInfoHandler.updateUI((Pair<? extends FunctionDescriptor, ResolutionFacade>)item, parameterInfoUIContext);
        }
        assertEquals(expectedResultText, parameterInfoUIContext.getResultText());
    }
}
