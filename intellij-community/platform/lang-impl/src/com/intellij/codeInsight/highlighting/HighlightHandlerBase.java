// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.highlighting;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author msk
 */
public abstract class HighlightHandlerBase {
  public static void setupFindModel(final Project project) {
    final FindManager findManager = FindManager.getInstance(project);
    FindModel model = findManager.getFindNextModel();
    if (model == null) {
      model = findManager.getFindInFileModel();
    }
    model.setSearchHighlighters(true);
    findManager.setFindWasPerformed();
    findManager.setFindNextModel(model);
  }

  public static String getLineTextErrorStripeTooltip(Document document, int offset, boolean escape) {
    final int lineNumber = document.getLineNumber(offset);
    int lineStartOffset = document.getLineStartOffset(lineNumber);
    int lineEndOffset = document.getLineEndOffset(lineNumber);
    int lineFragmentEndOffset = Math.min(lineStartOffset + 140, lineEndOffset);
    String lineText = document.getImmutableCharSequence().subSequence(lineStartOffset, lineFragmentEndOffset).toString();
    if (lineFragmentEndOffset != lineEndOffset) {
      lineText = lineText.trim() + "...";
    }
    return "  " + (escape ? StringUtil.escapeXmlEntities(lineText.trim()) : lineText.trim()) + "  ";
  }
}
