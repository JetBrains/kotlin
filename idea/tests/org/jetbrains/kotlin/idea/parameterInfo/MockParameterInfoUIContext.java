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

import com.intellij.codeInsight.hint.HintUtil;
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
                            "Green: " + background.equals(KotlinParameterInfoWithCallHandlerBase.GREEN_BACKGROUND);
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
    public boolean isSingleOverload() {
        return false;
    }

    @Override
    public boolean isSingleParameterInfo() {
        return false;
    }

    @Override
    public Color getDefaultParameterColor() {
        return HintUtil.INFORMATION_COLOR;
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
