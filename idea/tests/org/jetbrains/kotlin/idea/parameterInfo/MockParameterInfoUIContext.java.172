/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo;

import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.psi.PsiElement;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class MockParameterInfoUIContext implements ParameterInfoUIContext {
    private final PsiElement myParameterOwner;
    private final int myCurrentParameterIndex;

    private final ArrayList<String> result = new ArrayList<String>();

    MockParameterInfoUIContext(PsiElement parameterOwner, int currentParameterIndex) {
        myParameterOwner = parameterOwner;
        myCurrentParameterIndex = currentParameterIndex;
    }
    
    @Override
    public String setupUIComponentPresentation(String text, int highlightStartOffset, int highlightEndOffset,
                                             boolean isDisabled, boolean strikeout,
                                             boolean isDisabledBeforeHighlight, Color background) {
        String highlightedText;
        if (highlightStartOffset != -1 && highlightEndOffset != -1) {
            highlightedText = text.substring(0, highlightStartOffset)
                              + "<highlight>"
                              + text.substring(highlightStartOffset, highlightEndOffset)
                              + "</highlight>"
                              + text.substring(highlightEndOffset);
        }
        else {
            highlightedText = text;
        }

        String resultText = "Text: (" + highlightedText + "), " +
                            "Disabled: " + isDisabled + ", " +
                            "Strikeout: " + strikeout + ", " +
                            "Green: " + KotlinParameterInfoWithCallHandlerBase.GREEN_BACKGROUND.equals(background);
        result.add(resultText);

        // return value not used, just return something
        return resultText;
    }

    @Override
    public boolean isUIComponentEnabled() {
        return false;
    }

    @Override
    public void setUIComponentEnabled(boolean enabled) {
    }

    @Override
    public int getCurrentParameterIndex() {
        return myCurrentParameterIndex;
    }

    @Override
    public PsiElement getParameterOwner() {
        return myParameterOwner;
    }

    @Override
    public Color getDefaultParameterColor() {
        return null;
    }
    
    public String getResultText() {
        StringBuilder stringBuilder = new StringBuilder();
        Collections.sort(result);
        for (String s : result) {
            stringBuilder.append(s).append("\n");
        }
        return stringBuilder.toString().trim();
    }
}
