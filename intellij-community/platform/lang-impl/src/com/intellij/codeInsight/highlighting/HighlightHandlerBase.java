/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
  static void setupFindModel(final Project project) {
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
