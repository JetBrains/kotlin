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
package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
class LookupPreview {
  private final List<Inlay> myInlays = new ArrayList<>();
  private final LookupImpl myLookup;

  LookupPreview(LookupImpl lookup) {
    myLookup = lookup;
  }

  void updatePreview(@Nullable LookupElement item) {
    if (!Registry.is("ide.lookup.preview.insertion")) return;

    myInlays.forEach(Disposer::dispose);
    myInlays.clear();

    String suffix = getSuffixText(item);
    Editor editor = myLookup.getTopLevelEditor();
    if (!suffix.isEmpty() && editor instanceof EditorImpl &&
        !editor.getSelectionModel().hasSelection() &&
        InplaceRefactoring.getActiveInplaceRenamer(editor) == null) {
      myLookup.performGuardedChange(() -> {
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
          ensureCaretBeforeInlays(caret);
          addInlay(suffix, caret.getOffset());
        }
      });
    }
  }

  private static void ensureCaretBeforeInlays(Caret caret) {
    LogicalPosition position = caret.getLogicalPosition();
    if (position.leansForward) {
      caret.moveToLogicalPosition(position.leanForward(false));
    }
  }

  private String getSuffixText(@Nullable LookupElement item) {
    if (item != null) {
      String itemText = StringUtil.notNullize(LookupElementPresentation.renderElement(item).getItemText());
      String prefix = myLookup.itemPattern(item);
      if (prefix.isEmpty()) {
        return itemText;
      }

      FList<TextRange> fragments = LookupCellRenderer.getMatchingFragments(prefix, itemText);
      if (fragments != null && !fragments.isEmpty()) {
        List<TextRange> list = ContainerUtil.newArrayList(fragments);
        return itemText.substring(list.get(list.size() - 1).getEndOffset());
      }
    }
    return "";
  }

  private void addInlay(String suffix, int caretOffset) {
    Inlay inlay = myLookup.getTopLevelEditor().getInlayModel().addInlineElement(caretOffset, true, createGrayRenderer(suffix));
    if (inlay != null) {
      myInlays.add(inlay);
      Disposer.register(myLookup, inlay);
    }
  }

  @NotNull
  private static EditorCustomElementRenderer createGrayRenderer(final String suffix) {
    return new EditorCustomElementRenderer() {
      @Override
      public int calcWidthInPixels(@NotNull Inlay inlay) {
        Editor editor = inlay.getEditor();
        return editor.getContentComponent().getFontMetrics(getFont(editor)).stringWidth(suffix);
      }

      @Override
      public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle r, @NotNull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();
        g.setColor(JBColor.GRAY);
        g.setFont(getFont(editor));
        g.drawString(suffix, r.x, r.y + ((EditorImpl)editor).getAscent());
      }

      private Font getFont(@NotNull Editor editor) {
        return editor.getColorsScheme().getFont(EditorFontType.PLAIN);
      }
    };
  }
}
