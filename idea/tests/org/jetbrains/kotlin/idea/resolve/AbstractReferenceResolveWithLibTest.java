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

package org.jetbrains.kotlin.idea.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiDelegateReference;
import com.intellij.testFramework.LightProjectDescriptor;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.test.AstAccessControl;
import org.jetbrains.kotlin.idea.test.JdkAndMockLibraryProjectDescriptor;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

public abstract class AbstractReferenceResolveWithLibTest extends AbstractReferenceResolveTest {
    private static final String TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/resolve/referenceWithLib";

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        if (PluginTestCaseBase.isAllFilesPresentTest(getTestName(true))) {
            return null;
        }
        return new JdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH + "/" + getTestName(true) + "Src", false, true, false, false);
    }

    @Nullable
    @Override
    public PsiReference wrapReference(@Nullable final PsiReference reference) {
        if (reference == null) return null;

        if (InTextDirectivesUtils.isDirectiveDefined(myFixture.getFile().getText(), AstAccessControl.INSTANCE.getALLOW_AST_ACCESS_DIRECTIVE())) {
            return reference;
        }

        return new PsiDelegateReference(reference) {
            @Nullable
            @Override
            public PsiElement resolve() {
                return AstAccessControl.INSTANCE.execute(false, getTestRootDisposable(), myFixture, new Function0<PsiElement>() {
                    @Override
                    public PsiElement invoke() {
                        return reference.resolve();
                    }
                });
            }

            @Override
            public String toString() {
                return reference.toString();
            }
        };
    }
}
