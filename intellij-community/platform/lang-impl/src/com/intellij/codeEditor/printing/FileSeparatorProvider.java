// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeEditor.printing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.LineMarkersPass;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.SeparatorPlacement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileSeparatorProvider {
  @NotNull
  public static List<LineMarkerInfo<PsiElement>> getFileSeparators(PsiFile file, final Document document) {
    final List<LineMarkerInfo<PsiElement>> result = new ArrayList<>();
    for (LineMarkerInfo<PsiElement> lineMarkerInfo : LineMarkersPass.queryLineMarkers(file, document)) {
      if (lineMarkerInfo.separatorColor != null) {
        result.add(lineMarkerInfo);
      }
    }

    Collections.sort(result, Comparator.comparingInt(i -> getDisplayLine(i, document)));
    return result;
  }

  public static int getDisplayLine(@NotNull LineMarkerInfo lineMarkerInfo, @NotNull Document document) {
    int offset = lineMarkerInfo.separatorPlacement == SeparatorPlacement.TOP ? lineMarkerInfo.startOffset : lineMarkerInfo.endOffset;
    return document.getLineNumber(Math.min(document.getTextLength(), Math.max(0, offset))) +
           (lineMarkerInfo.separatorPlacement == SeparatorPlacement.TOP ? 0 : 1);
  }
}