// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeHighlighting.*;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.intellij.lang.documentation.DocumentationMarkup.*;

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
    if (!Registry.is("editor.render.doc.comments")) return null;
    long current = PsiModificationTracker.SERVICE.getInstance(file.getProject()).getModificationCount();
    Long existing = editor.getUserData(MODIFICATION_STAMP);
    return existing != null && existing == current ? null : new DocRenderPass(editor, file);
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
    Items items = new Items();
    SyntaxTraverser.psiTraverser(psiFile).filter(PsiDocCommentBase.class).forEach(comment -> {
      TextRange range = comment.getTextRange();
      if (range != null && DocRenderItem.isValidRange(document, range)) {
        String textToRender = calcText(comment);
        if (textToRender != null) {
          items.addItem(new Item(range, textToRender));
        }
      }
    });
    return items;
  }

  @Nullable
  private static String calcText(@NotNull PsiDocCommentBase comment) {
    try {
      PsiElement owner = comment.getOwner();
      if (owner == null) return null;
      String doc = DocumentationManager.getProviderFromElement(owner).generateDoc(owner, null);
      if (doc == null) return null;
      // remove definition (it's already visible in the source code)
      int definitionPos = doc.indexOf(DEFINITION_START);
      if (definitionPos >= 0) {
        int definitionEnd = doc.indexOf(DEFINITION_END, definitionPos + DEFINITION_START.length());
        if (definitionEnd > 0) {
          doc = doc.substring(0, definitionPos) + doc.substring(definitionEnd + DEFINITION_END.length());
        }
      }
      // remove information about non-code annotations
      int sectionPos = 0;
      while ((sectionPos = doc.indexOf(SECTION_HEADER_START, sectionPos)) >= 0) {
        int sectionNamePos = sectionPos + SECTION_HEADER_START.length();
        int separatorPos = doc.indexOf(SECTION_SEPARATOR, sectionNamePos);
        if (separatorPos < 0) break;
        int endPos = doc.indexOf(SECTION_END, separatorPos + SECTION_SEPARATOR.length());
        if (endPos < 0) break;
        int endEndPos = endPos + SECTION_END.length();
        if (doc.substring(sectionNamePos, separatorPos).contains("annotations")) {
          doc = doc.substring(0, sectionPos) + doc.substring(endEndPos);
        }
        else {
          sectionPos = endEndPos;
        }
      }
      return isWhitespaceAndTags(doc) ? null : doc;
    }
    catch (IndexNotReadyException e) {
      LOG.debug(e);
      return CodeInsightBundle.message("doc.render.dumb.mode.text");
    }
  }

  private static boolean isWhitespaceAndTags(@NotNull String text) {
    boolean tag = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (tag) {
        if (c == '>') {
          tag = false;
        }
      }
      else {
        if (c == '<') {
          tag = true;
        }
        else {
          if (c > ' ') return false;
        }
      }
    }
    return true;
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

    private Item(@NotNull TextRange textRange, @NotNull String textToRender) {
      this.textRange = textRange;
      this.textToRender = textToRender;
    }
  }
}
