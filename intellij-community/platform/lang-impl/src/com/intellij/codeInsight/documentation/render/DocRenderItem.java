// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.documentation.DocFontSizePopup;
import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.function.BooleanSupplier;

class DocRenderItem {
  private static final Key<DocRenderItem> OUR_ITEM = Key.create("doc.render.item");
  private static final Key<Collection<DocRenderItem>> OUR_ITEMS = Key.create("doc.render.items");
  private static final Key<VisibleAreaListener> VISIBLE_AREA_LISTENER = Key.create("doc.render.visible.area.listener");
  private static final Key<Disposable> LISTENERS_DISPOSABLE = Key.create("doc.render.listeners.disposable");

  final Editor editor;
  final RangeHighlighter highlighter;
  String textToRender;
  private FoldRegion foldRegion;
  Inlay<DocRenderer> inlay;

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
      if (itemsToSet.isEmpty()) return;
      editor.putUserData(OUR_ITEMS, items = new ArrayList<>());
    }
    else {
      items = existing;
    }
    keepScrollingPositionWhile(editor, () -> {
      List<Runnable> foldingTasks = new ArrayList<>();
      List<DocRenderItem> itemsToUpdateInlays = new ArrayList<>();
      boolean updated = false;
      for (Iterator<DocRenderItem> it = items.iterator(); it.hasNext(); ) {
        DocRenderItem item = it.next();
        DocRenderPassFactory.Item matchingItem = item.isValid() ? itemsToSet.removeItem(item.highlighter) : null;
        if (matchingItem == null) {
          updated |= item.remove(foldingTasks);
          it.remove();
        }
        else if (matchingItem.textToRender != null && !matchingItem.textToRender.equals(item.textToRender)) {
          item.textToRender = matchingItem.textToRender;
          itemsToUpdateInlays.add(item);
        }
        else {
          item.updateIcon();
        }
      }
      Collection<DocRenderItem> newRenderItems = new ArrayList<>();
      for (DocRenderPassFactory.Item item : itemsToSet) {
        DocRenderItem newItem = new DocRenderItem(editor, item.textRange, collapseNewItems ? null : item.textToRender);
        newRenderItems.add(newItem);
        if (collapseNewItems) {
          updated |= newItem.toggle(foldingTasks);
          newItem.textToRender = item.textToRender;
          itemsToUpdateInlays.add(newItem);
        }
      }
      editor.getFoldingModel().runBatchFoldingOperation(() -> foldingTasks.forEach(Runnable::run), true, false);
      newRenderItems.forEach(DocRenderItem::cleanup);
      updated |= updateInlays(itemsToUpdateInlays);
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
        connection.setDefaultHandler((event, params) -> updateInlays(editor));
        connection.subscribe(EditorColorsManager.TOPIC);
        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
          @Override
          public void editorReleased(@NotNull EditorFactoryEvent event) {
            if (event.getEditor() == editor) {
              // this ensures renderers are not kept for the released editors
              setItemsToEditor(editor, new DocRenderPassFactory.Items(), false);
            }
          }
        }, connection);
        editor.getCaretModel().addCaretListener(new MyCaretListener(), connection);

        DocRenderMouseEventBridge mouseEventBridge = new DocRenderMouseEventBridge();
        editor.addEditorMouseListener(mouseEventBridge, connection);
        editor.addEditorMouseMotionListener(mouseEventBridge, connection);

        editor.putUserData(LISTENERS_DISPOSABLE, connection);
      }
    }
  }

  private static void keepScrollingPositionWhile(@NotNull Editor editor, @NotNull BooleanSupplier task) {
    EditorScrollingPositionKeeper keeper = new EditorScrollingPositionKeeper(editor);
    keeper.savePosition();
    if (task.getAsBoolean()) keeper.restorePosition(false);
  }

  static DocRenderItem getItemAroundOffset(@NotNull Editor editor, int offset) {
    Collection<DocRenderItem> items = editor.getUserData(OUR_ITEMS);
    if (items == null || items.isEmpty()) return null;
    Document document = editor.getDocument();
    if (offset < 0 || offset > document.getTextLength()) return null;
    int line = document.getLineNumber(offset);
    return items.stream().filter(i -> {
      if (!i.isValid()) return false;
      int startLine = document.getLineNumber(i.highlighter.getStartOffset());
      int endLine = document.getLineNumber(i.highlighter.getEndOffset());
      return line >= startLine - 1 && line <= endLine + 1;
    }).min(Comparator.comparingInt(i -> i.highlighter.getStartOffset())).orElse(null);
  }

  private DocRenderItem(@NotNull Editor editor, @NotNull TextRange textRange, @Nullable String textToRender) {
    this.editor = editor;
    this.textToRender = textToRender;
    assert editor.getProject() != null;
    highlighter = editor.getMarkupModel()
      .addRangeHighlighter(textRange.getStartOffset(), textRange.getEndOffset(), 0, null, HighlighterTargetArea.EXACT_RANGE);
    updateIcon();
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

  boolean toggle(@Nullable Collection<Runnable> foldingTasks) {
    if (!(editor instanceof EditorEx)) return false;
    FoldingModelEx foldingModel = ((EditorEx)editor).getFoldingModel();
    if (foldRegion == null) {
      if (textToRender == null && foldingTasks == null) {
        generateHtmlInBackgroundAndToggle();
        return false;
      }
      int inlayOffset = calcInlayOffset();
      inlay = editor.getInlayModel().addBlockElement(inlayOffset, true, true, BlockInlayPriority.DOC_RENDER, new DocRenderer(this));
      if (inlay != null) {
        int foldStartOffset = calcFoldStartOffset();
        int foldEndOffset = calcFoldEndOffset();
        Runnable foldingTask = () -> {
          // if this fails (setting 'foldRegion' to null), 'cleanup' method will fix the mess
          foldRegion = foldingModel.createFoldRegion(foldStartOffset, foldEndOffset, "", null, true);
          if (foldRegion != null) foldRegion.putUserData(OUR_ITEM, this);
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

  private void generateHtmlInBackgroundAndToggle() {
    ReadAction.nonBlocking(() -> {
      return DocRenderPassFactory.calcText(getComment());
    }).withDocumentsCommitted(Objects.requireNonNull(editor.getProject()))
      .coalesceBy(this)
      .finishOnUiThread(ModalityState.any(), html -> {
        textToRender = html;
        toggle(null);
      }).submit(AppExecutorUtil.getAppExecutorService());
  }

  PsiDocCommentBase getComment() {
    if (highlighter.isValid()) {
      PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(Objects.requireNonNull(editor.getProject()));
      PsiFile file = psiDocumentManager.getPsiFile(editor.getDocument());
      if (file != null) {
        return PsiTreeUtil.getParentOfType(file.findElementAt(highlighter.getStartOffset()), PsiDocCommentBase.class, false);
      }
    }
    return null;
  }

  private static boolean updateInlays(@NotNull Collection<DocRenderItem> items) {
    return DocRenderItemUpdater.getInstance().updateInlays(ContainerUtil.mapNotNull(items, i -> i.inlay));
  }

  private static void updateInlays(@NotNull Editor editor) {
    keepScrollingPositionWhile(editor, () -> {
      Collection<DocRenderItem> items = editor.getUserData(OUR_ITEMS);
      return items != null && updateInlays(items);
    });
  }

  private void updateIcon() {
    boolean iconEnabled = DocRenderDummyLineMarkerProvider.isGutterIconEnabled();
    boolean iconExists = highlighter.getGutterIconRenderer() != null;
    if (iconEnabled != iconExists) {
      if (iconEnabled) {
        highlighter.setGutterIconRenderer(new MyGutterIconRenderer(AllIcons.Gutter.JavadocRead));
      }
      else {
        highlighter.setGutterIconRenderer(null);
      }
      if (inlay != null) inlay.update();
    }
  }

  AnAction createToggleAction() {
    return new ToggleRenderingAction(this);
  }

  private static class MyCaretListener implements CaretListener {
    @Override
    public void caretPositionChanged(@NotNull CaretEvent event) {
      onCaretUpdate(event);
    }

    @Override
    public void caretAdded(@NotNull CaretEvent event) {
      onCaretUpdate(event);
    }

    private static void onCaretUpdate(@NotNull CaretEvent event) {
      Caret caret = event.getCaret();
      if (caret == null) return;
      int caretOffset = caret.getOffset();
      FoldRegion foldRegion = caret.getEditor().getFoldingModel().getCollapsedRegionAtOffset(caretOffset);
      if (foldRegion != null && caretOffset > foldRegion.getStartOffset()) {
        DocRenderItem item = foldRegion.getUserData(OUR_ITEM);
        if (item != null) item.toggle(null);
      }
    }
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

  class MyGutterIconRenderer extends GutterIconRenderer implements DumbAware {
    private final Icon icon;

    MyGutterIconRenderer(Icon icon) {
      this.icon = icon;
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
      return icon;
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
      AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_TOGGLE_RENDERED_DOC);
      if (action == null) return null;
      String actionText = action.getTemplateText();
      if (actionText == null) return null;
      return XmlStringUtil.wrapInHtml(actionText + HelpTooltip.getShortcutAsHtml(KeymapUtil.getFirstKeyboardShortcutText(action)));
    }

    @Nullable
    @Override
    public AnAction getClickAction() {
      return createToggleAction();
    }

    @Override
    public ActionGroup getPopupMenuActions() {
      return ObjectUtils.tryCast(ActionManager.getInstance().getAction(IdeActions.GROUP_DOC_COMMENT_GUTTER_ICON_CONTEXT_MENU),
                                 ActionGroup.class);
    }
  }

  private static class ToggleRenderingAction extends DumbAwareAction {
    private final DocRenderItem item;

    private ToggleRenderingAction(DocRenderItem item) {
      copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_TOGGLE_RENDERED_DOC));
      this.item = item;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      if (item.isValid()) {
        item.toggle(null);
      }
    }
  }

  static class ChangeFontSize extends DumbAwareAction {
    ChangeFontSize() {
      super(CodeInsightBundle.messagePointer("javadoc.adjust.font.size"));
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Editor editor = e.getData(CommonDataKeys.EDITOR);
      if (editor != null) {
        DocFontSizePopup.show(() -> updateInlays(editor), editor.getContentComponent());
      }
    }
  }
}
