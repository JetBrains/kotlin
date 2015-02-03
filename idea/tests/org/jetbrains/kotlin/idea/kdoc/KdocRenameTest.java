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

package org.jetbrains.kotlin.idea.kdoc;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;

public class KdocRenameTest extends LightCodeInsightTestCase {
    @NotNull
    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/kdoc/rename/";
    }

    public void testParamReference() throws Exception {
        doTest("bar");
    }

    public void testCodeReference() throws Exception {
        doTest("xyzzy");
    }

    private void doTest(String newName) throws Exception {
        configureByFile(getTestName(false) + ".kt");
        PsiElement element = TargetElementUtilBase
                .findTargetElement(myEditor,
                                   TargetElementUtilBase.ELEMENT_NAME_ACCEPTED | TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED);
        assertNotNull(element);
        new RenameProcessor(getProject(), element, newName, true, true).run();
        checkResultByFile(getTestName(false) + ".kt.after");
    }
}
