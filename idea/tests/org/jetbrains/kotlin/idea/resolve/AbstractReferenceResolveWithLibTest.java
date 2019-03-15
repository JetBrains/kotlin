/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiDelegateReference;
import com.intellij.testFramework.LightProjectDescriptor;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.test.AstAccessControl;
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

public abstract class AbstractReferenceResolveWithLibTest extends AbstractReferenceResolveTest {
    private static final String TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/resolve/referenceWithLib";

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        if (PluginTestCaseBase.isAllFilesPresentTest(getTestName(true))) {
            return null;
        }
        return new SdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH + "/" + getTestName(true) + "Src", false, true, false, false);
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
