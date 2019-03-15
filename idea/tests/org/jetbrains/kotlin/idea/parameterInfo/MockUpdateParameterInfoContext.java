/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo;

import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderEx;
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
    public boolean isPreservedOnHintHidden() {
        return false;
    }

    @Override
    public void setPreservedOnHintHidden(boolean value) {

    }

    @Override
    public boolean isInnermostContext() {
        return false;
    }

    @Override
    public UserDataHolderEx getCustomContext() {
        return null;
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

    //@Override since 183
    //add `@Override` when BUNCH: 182 is dropped
    public boolean isSingleParameterInfo() {
        return false;
    }
}
