// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.documentation.DocFontSizePopup;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

class DocRenderItem {
  private static final Key<Collection<DocRenderItem>> OUR_ITEMS = Key.create("doc.render.items");
  private static final Key<VisibleAreaListener> VISIBLE_AREA_LISTENER = Key.create("doc.render.visible.area.listener");
  private static final Key<Disposable> LISTENERS_DISPOSABLE = Key.create("doc.render.listeners.disposable");

  final Editor editor;
  final RangeHighlighter highlighter;
  String textToRender;
  private FoldRegion foldRegion;
  private Inlay<DocRenderer> inlay;

  static boolean isValidRange(@NotNull Document document, @NotNull TextRange range) {
    CharSequence text = document.getImmutableCharSequence();
    int startOffset = range.getStartOffset();
    int endOffset = range.getEndOffset();
    int startLine = document.getLineNumber(startOffset);
    int endLine = document.getLineNumber(endOffset);
    return startLine > 0 && endLine < document.getLineCount() - 1 &&
           CharArrayUtil.containsOnlyWhiteSpaces(text.subSequence(document.getLineStartOffset(startLine), startOffset)) &&
           CharArrayUtil.containsOnlyWhiteSpaces(text.subSequence(endOffset, document.getLineEndOffset(endLine)));
  }

  static void setItemsToEditor(@NotNull Editor editor, @NotNull DocRenderPassFactory.Items itemsToSet, boolean collapseNewItems) {
    Collection<DocRenderItem> items;
    Collection<DocRenderItem> existing = editor.getUserData(OUR_ITEMS);
    if (existing == null) {
      editor.putUserData(OUR_ITEMS, items = new ArrayList<>());
    }
    else {
      items = existing;
    }
    keepScrollingPositionWhile(editor, () -> {
      List<Runnable> foldingTasks = new ArrayList<>();
      boolean updated = false;
      for (Iterator<DocRenderItem> it = items.iterator(); it.hasNext(); ) {
        DocRenderItem item = it.next();
        DocRenderPassFactory.Item matchingItem = item.isValid() ? itemsToSet.removeItem(item.getTextRange()) : null;
        if (matchingItem == null) {
          updated |= item.remove(foldingTasks);
          it.remove();
        }
        else {
          updated |= item.update(matchingItem.textToRender);
        }
      }
      Collection<DocRenderItem> newRenderItems = new ArrayList<>();
      for (DocRenderPassFactory.Item item : itemsToSet) {
        DocRenderItem newItem = new DocRenderItem(editor, item.textRange, item.textToRender);
        newRenderItems.add(newItem);
        if (collapseNewItems) {
          updated |= newItem.toggle(foldingTasks);
        }
      }
      editor.getFoldingModel().runBatchFoldingOperation(() -> foldingTasks.forEach(Runnable::run), true, false);
      newRenderItems.forEach(DocRenderItem::cleanup);
      items.addAll(newRenderItems);
      return updated;
    });
    setupListeners(editor, items.isEmpty());
  }

  private static void setupListeners(@NotNull Editor editor, boolean disable) {
    if (disable) {
      VisibleAreaListener visibleAreaListener = editor.getUserData(VISIBLE_AREA_LISTENER);
      if (visibleAreaListener != null) {
        editor.getScrollingModel().removeVisibleAreaListener(visibleAreaListener);
        editor.putUserData(VISIBLE_AREA_LISTENER, null);
      }
      Disposable listenersDisposable = editor.getUserData(LISTENERS_DISPOSABLE);
      if (listenersDisposable != null) {
        Disposer.dispose(listenersDisposable);
        editor.putUserData(LISTENERS_DISPOSABLE, null);
      }
    }
    else {
      if (editor.getUserData(VISIBLE_AREA_LISTENER) == null) {
        VisibleAreaListener visibleAreaListener = new MyVisibleAreaListener(editor);
        editor.getScrollingModel().addVisibleAreaListener(visibleAreaListener);
        editor.putUserData(VISIBLE_AREA_LISTENER, visibleAreaListener);
      }
      if (editor.getUserData(LISTENERS_DISPOSABLE) == null) {
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.setDefaultHandler((event, params) -> {
          if (editor.isDisposed()) {
            Disposer.dispose(connection);
          }
          else {
            updateInlays(editor);
          }
        });
        connection.subscribe(EditorColorsManager.TOPIC);
        connection.subscribe(LafManagerListener.TOPIC);
        editor.putUserData(LISTENERS_DISPOSABLE, connection);
      }
    }
  }

  static void keepScrollingPositionWhile(@NotNull Editor editor, @NotNull BooleanSupplier task) {
    EditorScrollingPositionKeeper keeper = new EditorScrollingPositionKeeper(editor, true);
    keeper.savePosition();
    if (task.getAsBoolean()) keeper.restorePosition(false);
  }

  private DocRenderItem(@NotNull Editor editor, @NotNull TextRange textRange, @NotNull String textToRender) {
    this.editor = editor;
    this.textToRender = textToRender;
    highlighter = editor.getMarkupModel()
      .addRangeHighlighter(textRange.getStartOffset(), textRange.getEndOffset(), 0, null, HighlighterTargetArea.EXACT_RANGE);
    AnAction toggleAction = new ToggleRenderingAction(this);
    highlighter.setGutterIconRenderer(new MyGutterIconRenderer(toggleAction));
  }

  private Segment getTextRange() {
    return highlighter;
  }

  private int calcFoldStartOffset() {
    Document document = highlighter.getDocument();
    int startLine = document.getLineNumber(highlighter.getStartOffset());
    return startLine == 0 ? 0 : document.getLineEndOffset(startLine - 1);
  }

  private int calcFoldEndOffset() {
    return highlighter.getEndOffset();
  }

  private int calcInlayOffset() {
    Document document = highlighter.getDocument();
    int endOffset = highlighter.getEndOffset();
    int endLine = document.getLineNumber(endOffset);
    return endLine < document.getLineCount() - 1 ? document.getLineStartOffset(endLine + 1) : endOffset;
  }

  private boolean isValid() {
    if (!highlighter.isValid() || highlighter.getStartOffset() >= highlighter.getEndOffset()) return false;
    return foldRegion == null && inlay == null ||
           foldRegion != null && foldRegion.isValid() &&
           foldRegion.getStartOffset() == calcFoldStartOffset() && foldRegion.getEndOffset() == calcFoldEndOffset() &&
           inlay != null && inlay.isValid() && inlay.getOffset() == calcInlayOffset();
  }

  private void cleanup() {
    if (foldRegion == null && inlay != null && inlay.isValid()) {
      Disposer.dispose(inlay);
      inlay = null;
    }
  }

  private boolean remove(@NotNull Collection<Runnable> foldingTasks) {
    boolean updated = false;
    highlighter.dispose();
    if (foldRegion != null && foldRegion.isValid()) {
      foldingTasks.add(() -> foldRegion.getEditor().getFoldingModel().removeFoldRegion(foldRegion));
      updated = true;
    }
    if (inlay != null && inlay.isValid()) {
      Disposer.dispose(inlay);
      updated = true;
    }
    return updated;
  }

  private boolean update(@NotNull String textToRender) {
    boolean updated = false;
    if (!textToRender.equals(this.textToRender)) {
      this.textToRender = textToRender;
      if (inlay != null) {
        inlay.putUserData(DocRenderer.RECREATE_COMPONENT, Boolean.TRUE);
        inlay.update();
        updated = true;
      }
    }
    return updated;
  }

  private boolean toggle(@Nullable Collection<Runnable> foldingTasks) {
    if (!(editor instanceof EditorEx)) return false;
    FoldingModelEx foldingModel = ((EditorEx)editor).getFoldingModel();
    if (foldRegion == null) {
      int inlayOffset = calcInlayOffset();
      inlay = editor.getInlayModel().addBlockElement(inlayOffset, true, true, BlockInlayPriority.DOC_RENDER, new DocRenderer(this));
      if (inlay != null) {
        int foldStartOffset = calcFoldStartOffset();
        int foldEndOffset = calcFoldEndOffset();
        Runnable foldingTask = () -> {
          // if this fails (setting 'foldRegion' to null), 'cleanup' method will fix the mess
          foldRegion = foldingModel.createFoldRegion(foldStartOffset, foldEndOffset, "", null, true);
        };
        if (foldingTasks == null) {
          foldingModel.runBatchFoldingOperation(foldingTask, true, false);
          cleanup();
        }
        else {
          foldingTasks.add(foldingTask);
        }
      }
    }
    else {
      Runnable foldingTask = () -> {
        for (FoldRegion region : foldingModel.getAllFoldRegions()) {
          if (region.getStartOffset() >= foldRegion.getStartOffset() && region.getEndOffset() <= foldRegion.getEndOffset()) {
            region.setExpanded(true);
          }
        }
        foldingModel.removeFoldRegion(foldRegion);
        foldRegion = null;
      };
      if (foldingTasks == null) {
        foldingModel.runBatchFoldingOperation(foldingTask, true, false);
      }
      else {
        foldingTasks.add(foldingTask);
      }
      Disposer.dispose(inlay);
      inlay = null;
    }
    return true;
  }

  private static void updateInlays(@NotNull Editor editor) {
    keepScrollingPositionWhile(editor, () -> {
      AtomicBoolean updated = new AtomicBoolean();
      editor.getInlayModel().getBlockElementsInRange(0, editor.getDocument().getTextLength(), DocRenderer.class).forEach(inlay -> {
        updated.set(true);
        inlay.putUserData(DocRenderer.RECREATE_COMPONENT, Boolean.TRUE);
        inlay.update();
      });
      return updated.get();
    });
  }

  private static class MyVisibleAreaListener implements VisibleAreaListener {
    private int lastWidth;
    private AffineTransform lastFrcTransform;

    private MyVisibleAreaListener(@NotNull Editor editor) {
      lastWidth = DocRenderer.calcInlayWidth(editor);
      lastFrcTransform = getTransform(editor);
    }

    private static AffineTransform getTransform(Editor editor) {
      return FontInfo.getFontRenderContext(editor.getContentComponent()).getTransform();
    }

    @Override
    public void visibleAreaChanged(@NotNull VisibleAreaEvent e) {
      if (e.getNewRectangle().isEmpty()) return; // ignore switching between tabs
      Editor editor = e.getEditor();
      int newWidth = DocRenderer.calcInlayWidth(editor);
      AffineTransform transform = getTransform(editor);
      if (newWidth != lastWidth || !Objects.equals(transform, lastFrcTransform)) {
        lastWidth = newWidth;
        lastFrcTransform = transform;
        updateInlays(editor);
      }
    }
  }

  private static class MyGutterIconRenderer extends GutterIconRenderer implements DumbAware {
    private final AnAction action;

    private MyGutterIconRenderer(AnAction action) {
      this.action = action;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof MyGutterIconRenderer;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @NotNull
    @Override
    public Icon getIcon() {
      return AllIcons.Toolwindows.Documentation;
    }

    @NotNull
    @Override
    public Alignment getAlignment() {
      return Alignment.RIGHT;
    }

    @Override
    public boolean isNavigateAction() {
      return true;
    }

    @Nullable
    @Override
    public String getTooltipText() {
      return CodeInsightBundle.message("doc.toggle.rendered.presentation");
    }

    @Nullable
    @Override
    public AnAction getClickAction() {
      return action;
    }
  }

  private static class ToggleRenderingAction extends AnAction {
    private final DocRenderItem item;

    private ToggleRenderingAction(DocRenderItem item) {
      super(CodeInsightBundle.message("doc.toggle.rendered.presentation"));
      this.item = item;
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      if (item.isValid()) {
        item.toggle(null);
      }
    }
  }

  static class ChangeFontSize extends AnAction {
    ChangeFontSize() {
      super(CodeInsightBundle.message("javadoc.adjust.font.size"));
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Editor editor = e.getData(CommonDataKeys.EDITOR);
      if (editor != null) {
        DocFontSizePopup.show(() -> updateInlays(editor));
      }
    }
  }
}
