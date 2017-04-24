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

import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

public class MockUpdateParameterInfoContext implements UpdateParameterInfoContext {
    private int myCurrentParameter = -1;
    private PsiFile myFile;
    private JavaCodeInsightTestFixture myFixture;

    MockUpdateParameterInfoContext(PsiFile file, JavaCodeInsightTestFixture fixture) {
        myFile = file;
        myFixture = fixture;
    }
    

    @Override
    public void removeHint() {
    }

    @Override
    public void setParameterOwner(PsiElement o) {
    }

    @Override
    public PsiElement getParameterOwner() {
        return null;
    }

    @Override
    public void setHighlightedParameter(Object parameter) {
    }

    @Override
    public Object getHighlightedParameter() {
        return null;
    }

    @Override
    public void setCurrentParameter(int index) {
        myCurrentParameter = index;
    }
    
    public int getCurrentParameter() {
        return myCurrentParameter;
    }

    @Override
    public boolean isUIComponentEnabled(int index) {
        return false;
    }

    @Override
    public void setUIComponentEnabled(int index, boolean b) {
    }

    @Override
    public int getParameterListStart() {
        return 0;
    }

    @Override
    public Object[] getObjectsToView() {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public Project getProject() {
        return null;
    }

    @Override
    public PsiFile getFile() {
        return myFile;
    }

    @Override
    public int getOffset() {
        return myFixture.getCaretOffset();
    }

    @NotNull
    @Override
    public Editor getEditor() {
        return myFixture.getEditor();
    }
}
