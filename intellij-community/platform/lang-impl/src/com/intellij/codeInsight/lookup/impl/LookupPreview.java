// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
        List<TextRange> list = new ArrayList<>(fragments);
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
        g.drawString(suffix, r.x, r.y + editor.getAscent());
      }

      private Font getFont(@NotNull Editor editor) {
        return editor.getColorsScheme().getFont(EditorFontType.PLAIN);
      }
    };
  }
}
