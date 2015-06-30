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

package org.jetbrains.kotlin.idea.refactoring.inline;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.lang.refactoring.InlineActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.intellij.codeInsight.TargetElementUtilBase.ELEMENT_NAME_ACCEPTED;
import static com.intellij.codeInsight.TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED;

public abstract class AbstractInlineTest extends JetLightCodeInsightFixtureTestCase {
    @Nullable
    private static InlineActionHandler findSuitableHandler(@NotNull PsiElement psiElement) {
        InlineActionHandler[] inlineActionHandlers = Extensions.getExtensions(InlineActionHandler.EP_NAME);
        for (InlineActionHandler handler : inlineActionHandlers) {
            if (handler.canInlineElement(psiElement)) {
                return handler;
            }
        }
        return null;
    }

    protected void doTest(@NotNull String path) throws IOException {
        File afterFile = new File(path + ".after");

        myFixture.configureByFile(path);

        boolean afterFileExists = afterFile.exists();

        final PsiElement targetElement =
                TargetElementUtil.findTargetElement(myFixture.getEditor(), ELEMENT_NAME_ACCEPTED | REFERENCED_ELEMENT_ACCEPTED);

        assertNotNull(targetElement);

        final InlineActionHandler handler = findSuitableHandler(targetElement);

        List<String> expectedErrors = InTextDirectivesUtils.findLinesWithPrefixesRemoved(myFixture.getFile().getText(), "// ERROR: ");
        if (handler != null) {
            try {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        handler.inlineElement(myFixture.getProject(), myFixture.getEditor(), targetElement);
                    }
                });

                assertTrue(afterFileExists);
                assertEmpty(expectedErrors);
                myFixture.checkResult(FileUtil.loadFile(afterFile, true));
            } catch (CommonRefactoringUtil.RefactoringErrorHintException e) {
                assertFalse(afterFileExists);
                assertEquals(1, expectedErrors.size());
                assertEquals(expectedErrors.get(0).replace("\\n", "\n"), e.getMessage());
            }
        }
        else {
            assertFalse(afterFileExists);
        }
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }
}
