// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeEditor.printing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.LineMarkersPass;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.SeparatorPlacement;
import com.intellij.psi.PsiFile;
import com.intellij.util.MathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FileSeparatorProvider {
  @NotNull
  static List<LineMarkerInfo<?>> getFileSeparators(PsiFile file, final Document document) {
    final List<LineMarkerInfo<?>> result = new ArrayList<>();
    for (LineMarkerInfo<?> lineMarkerInfo : LineMarkersPass.queryLineMarkers(file, document)) {
      if (lineMarkerInfo.separatorColor != null) {
        result.add(lineMarkerInfo);
      }
    }

    result.sort(Comparator.comparingInt(i -> getDisplayLine(i, document)));
    return result;
  }

  public static int getDisplayLine(@NotNull LineMarkerInfo<?> lineMarkerInfo, @NotNull Document document) {
    int offset = lineMarkerInfo.separatorPlacement == SeparatorPlacement.TOP ? lineMarkerInfo.startOffset : lineMarkerInfo.endOffset;
    return document.getLineNumber(MathUtil.clamp(offset, 0, document.getTextLength())) +
           (lineMarkerInfo.separatorPlacement == SeparatorPlacement.TOP ? 0 : 1);
  }
}