/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
        String taggedText;
        if (highlightStartOffset != -1 && highlightEndOffset != -1) {
            StringBuilder sb = new StringBuilder();
            if (isDisabledBeforeHighlight) {
                sb.append("<disabled>");
            }

            sb.append(text, 0, highlightStartOffset);

            if (isDisabledBeforeHighlight) {
                sb.append("</disabled>");
            }

            sb.append("<highlight>");
            sb.append(text, highlightStartOffset, highlightEndOffset);
            sb.append("</highlight>");

            sb.append(text, highlightEndOffset, text.length());

            taggedText = sb.toString();
        }
        else {
            taggedText = text;
        }

        String resultText = "Text: (" + taggedText + "), " +
                            "Disabled: " + isDisabled + ", " +
                            "Strikeout: " + strikeout + ", " +
                            "Green: " + KotlinParameterInfoWithCallHandlerBase.GREEN_BACKGROUND.equals(background);

        result.add(resultText);

        // return value not used, just return something
        return resultText;
    }

    @Override
    public void setupRawUIComponentPresentation(String htmlText) {
        throw new UnsupportedOperationException();
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
    public boolean isSingleOverload() {
        return false;
    }

    @Override
    public boolean isSingleParameterInfo() {
        return false;
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
