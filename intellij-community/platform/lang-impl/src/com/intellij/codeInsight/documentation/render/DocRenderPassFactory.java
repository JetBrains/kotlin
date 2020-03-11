// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeHighlighting.*;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class DocRenderPassFactory implements TextEditorHighlightingPassFactoryRegistrar, TextEditorHighlightingPassFactory {
  private static final Logger LOG = Logger.getInstance(DocRenderPassFactory.class);
  private static final Key<Long> MODIFICATION_STAMP = Key.create("doc.render.modification.stamp");

  @Override
  public void registerHighlightingPassFactory(@NotNull TextEditorHighlightingPassRegistrar registrar, @NotNull Project project) {
    registrar.registerTextEditorHighlightingPass(this, TextEditorHighlightingPassRegistrar.Anchor.AFTER, Pass.UPDATE_FOLDING, false, false);
  }

  @Nullable
  @Override
  public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull Editor editor) {
    long current = PsiModificationTracker.SERVICE.getInstance(file.getProject()).getModificationCount();
    Long existing = editor.getUserData(MODIFICATION_STAMP);
    return editor.getProject() == null || existing != null && existing == current ? null : new DocRenderPass(editor, file);
  }

  private static class DocRenderPass extends EditorBoundHighlightingPass {
    private Items items;

    DocRenderPass(@NotNull Editor editor, @NotNull PsiFile psiFile) {
      super(editor, psiFile, false);
    }

    @Override
    public void doCollectInformation(@NotNull ProgressIndicator progress) {
      items = calculateItemsToRender(Objects.requireNonNull(myDocument), myFile);
    }

    @Override
    public void doApplyInformationToEditor() {
      applyItemsToRender(myEditor, myProject, items, false);
    }
  }

  @NotNull
  public static Items calculateItemsToRender(@NotNull Document document, @NotNull PsiFile psiFile) {
    boolean enabled = EditorSettingsExternalizable.getInstance().isDocCommentRenderingEnabled();
    Items items = new Items();
    DocumentationManager.getProviderFromElement(psiFile).collectDocComments(psiFile, comment -> {
      TextRange range = comment.getTextRange();
      if (range != null && DocRenderItem.isValidRange(document, range)) {
        String textToRender = enabled ? calcText(comment) : null;
        items.addItem(new Item(range, textToRender));
      }
    });
    return items;
  }

  static @NotNull String calcText(@Nullable PsiDocCommentBase comment) {
    try {
      String text = null;
      if (comment != null) {
        PsiElement owner = comment.getOwner();
        if (owner != null) {
          text = DocumentationManager.getProviderFromElement(owner).generateRenderedDoc(owner);
        }
      }
      return text == null ? CodeInsightBundle.message("doc.render.not.available.text") : text;
    }
    catch (IndexNotReadyException e) {
      LOG.warn(e);
      return CodeInsightBundle.message("doc.render.dumb.mode.text");
    }
  }

  public static void applyItemsToRender(@NotNull Editor editor,
                                        @NotNull Project project,
                                        @NotNull Items items,
                                        boolean collapseNewRegions) {
    editor.putUserData(MODIFICATION_STAMP, PsiModificationTracker.SERVICE.getInstance(project).getModificationCount());
    DocRenderItem.setItemsToEditor(editor, items, collapseNewRegions);
  }

  public static class Items implements Iterable<Item> {
    private final Map<TextRange, Item> myItems = new LinkedHashMap<>();

    boolean isEmpty() {
      return myItems.isEmpty();
    }

    private void addItem(@NotNull Item item) {
      myItems.put(item.textRange, item);
    }

    @Nullable
    Item removeItem(@NotNull Segment textRange) {
      return myItems.remove(TextRange.create(textRange));
    }

    @NotNull
    @Override
    public Iterator<Item> iterator() {
      return myItems.values().iterator();
    }
  }

  static class Item {
    final TextRange textRange;
    final String textToRender;

    private Item(@NotNull TextRange textRange, @Nullable String textToRender) {
      this.textRange = textRange;
      this.textToRender = textToRender;
    }
  }
}
