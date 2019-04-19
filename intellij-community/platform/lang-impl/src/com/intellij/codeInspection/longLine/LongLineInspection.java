/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInspection.longLine;

import com.intellij.application.options.CodeStyle;
import com.intellij.application.options.CodeStyleSchemesConfigurable;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.DataManager;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

public class LongLineInspection extends LocalInspectionTool {
  @Nullable
  @Override
  public JComponent createOptionsPanel() {
    final HyperlinkLabel codeStyleHyperlink = new HyperlinkLabel("Edit Code Style settings");
    codeStyleHyperlink.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        DataManager.getInstance().getDataContextFromFocus().doWhenDone((Consumer<DataContext>)context -> {
          if (context != null) {
            final Settings settings = Settings.KEY.getData(context);
            if (settings != null) {
              settings.select(settings.find(CodeStyleSchemesConfigurable.class));
            }
            else {
              ShowSettingsUtil.getInstance()
                .showSettingsDialog(CommonDataKeys.PROJECT.getData(context), CodeStyleSchemesConfigurable.class);
            }
          }
        });
      }
    });
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(codeStyleHyperlink, BorderLayout.NORTH);
    return panel;
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    PsiFile file = holder.getFile();
    final int codeStyleRightMargin = CodeStyle.getSettings(file).getRightMargin(file.getLanguage());

    final VirtualFile vFile = file.getVirtualFile();
    if (vFile instanceof VirtualFileWindow) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }
    final Document document = FileDocumentManager.getInstance().getDocument(vFile);
    if (document == null) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }

    return new PsiElementVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        int length = element.getTextLength();
        if (element.getTextLength() != 0 && element.getFirstChild() == null && !ignoreFor(element)) {
          int offset = element.getTextOffset();
          int endOffset = offset + length;

          int startLine = document.getLineNumber(offset);
          if (offset > document.getLineStartOffset(startLine) + codeStyleRightMargin) {
            startLine++;
          }

          int endLine = document.getLineNumber(endOffset - 1);

          for (int l = startLine; l <= endLine; l++) {
            int lineEndOffset = document.getLineEndOffset(l);
            int lineMarginOffset = document.getLineStartOffset(l) + codeStyleRightMargin;
            if (lineEndOffset > lineMarginOffset) {
              int highlightingStartOffset = lineMarginOffset - offset;
              int highlightingEndOffset = Math.min(endOffset, lineEndOffset) - offset;
              if (highlightingStartOffset < highlightingEndOffset) {
                TextRange exceedingRange = new TextRange(highlightingStartOffset, highlightingEndOffset);
                holder.registerProblem(element,
                                       exceedingRange,
                                       String.format("Line is longer than allowed by code style (> %s columns)", codeStyleRightMargin));
              }
            }
          }
        }
      }
    };
  }

  private static boolean ignoreFor(@Nullable PsiElement element) {
    if (element == null) return false;
    for (LongLineInspectionPolicy policy : LongLineInspectionPolicy.EP_NAME.getExtensions()) {
      if (policy.ignoreLongLineFor(element)) {
        return true;
      }
    }
    return false;
  }
}
