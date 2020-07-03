// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.documentation.DocFontSizePopup;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import com.intellij.ide.ui.LafManagerListener;
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
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.*;
import java.util.function.BooleanSupplier;

public class DocRenderItem {
  private static final Key<DocRenderItem> OUR_ITEM = Key.create("doc.render.item");
  private static final Key<Collection<DocRenderItem>> OUR_ITEMS = Key.create("doc.render.items");
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
    return endLine < document.getLineCount() - 1 &&
           CharArrayUtil.containsOnlyWhiteSpaces(text.subSequence(document.getLineStartOffset(startLine), startOffset)) &&
           CharArrayUtil.containsOnlyWhiteSpaces(text.subSequence(endOffset, document.getLineEndOffset(endLine)));
  }

  static void setItemsToEditor(@NotNull Editor editor, @NotNull DocRenderPassFactory.Items itemsToSet, boolean collapseNewItems) {
    Collection<DocRenderItem> items;
    Collection<DocRenderItem> existing = editor.getUserData(OUR_ITEMS);
    if (existing == null) {
      if (itemsToSet.isEmpty()) return;
      editor.putUserData(OUR_ITEMS, items = new ArrayList<>());
      if (DocRenderManager.isDocRenderingEnabled(editor)) {
        collapseNewItems = true;
      }
    }
    else {
      items = existing;
    }
    boolean finalCollapseNewItems = collapseNewItems;
    keepScrollingPositionWhile(editor, () -> {
      List<Runnable> foldingTasks = new ArrayList<>();
      List<DocRenderItem> itemsToUpdateInlays = new ArrayList<>();
      boolean updated = false;
      for (Iterator<DocRenderItem> it = items.iterator(); it.hasNext(); ) {
        DocRenderItem existingItem = it.next();
        DocRenderPassFactory.Item matchingNewItem = existingItem.isValid() ? itemsToSet.removeItem(existingItem.highlighter) : null;
        if (matchingNewItem == null) {
          updated |= existingItem.remove(foldingTasks);
          it.remove();
        }
        else if (matchingNewItem.textToRender != null && !matchingNewItem.textToRender.equals(existingItem.textToRender)) {
          existingItem.textToRender = matchingNewItem.textToRender;
          itemsToUpdateInlays.add(existingItem);
        }
        else {
          existingItem.updateIcon();
        }
      }
      Collection<DocRenderItem> newRenderItems = new ArrayList<>();
      for (DocRenderPassFactory.Item item : itemsToSet) {
        DocRenderItem newItem = new DocRenderItem(editor, item.textRange, finalCollapseNewItems ? null : item.textToRender);
        newRenderItems.add(newItem);
        if (finalCollapseNewItems) {
          updated |= newItem.toggle(foldingTasks);
          newItem.textToRender = item.textToRender;
          itemsToUpdateInlays.add(newItem);
        }
      }
      editor.getFoldingModel().runBatchFoldingOperation(() -> foldingTasks.forEach(Runnable::run), true, false);
      newRenderItems.forEach(DocRenderItem::cleanup);
      updateInlays(itemsToUpdateInlays, true);
      items.addAll(newRenderItems);
      return updated;
    });
    setupListeners(editor, items.isEmpty());
  }

  private static void setupListeners(@NotNull Editor editor, boolean disable) {
    if (disable) {
      Disposable listenersDisposable = editor.getUserData(LISTENERS_DISPOSABLE);
      if (listenersDisposable != null) {
        Disposer.dispose(listenersDisposable);
        editor.putUserData(LISTENERS_DISPOSABLE, null);
      }
    }
    else {
      if (editor.getUserData(LISTENERS_DISPOSABLE) == null) {
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.setDefaultHandler((event, params) -> updateInlays(editor, true));
        connection.subscribe(EditorColorsManager.TOPIC);
        connection.subscribe(LafManagerListener.TOPIC);
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

        DocRenderSelectionManager selectionManager = new DocRenderSelectionManager(editor);
        Disposer.register(connection, selectionManager);

        DocRenderMouseEventBridge mouseEventBridge = new DocRenderMouseEventBridge(selectionManager);
        editor.addEditorMouseListener(mouseEventBridge, connection);
        editor.addEditorMouseMotionListener(mouseEventBridge, connection);

        IconVisibilityController iconVisibilityController = new IconVisibilityController();
        editor.addEditorMouseListener(iconVisibilityController, connection);
        editor.addEditorMouseMotionListener(iconVisibilityController, connection);
        editor.getScrollingModel().addVisibleAreaListener(iconVisibilityController, connection);
        Disposer.register(connection, iconVisibilityController);

        editor.getScrollingModel().addVisibleAreaListener(new MyVisibleAreaListener(editor), connection);
        editor.getInlayModel().addListener(new MyInlayListener(), connection);

        editor.putUserData(LISTENERS_DISPOSABLE, connection);
      }
    }
  }

  private static void keepScrollingPositionWhile(@NotNull Editor editor, @NotNull BooleanSupplier task) {
    EditorScrollingPositionKeeper keeper = new EditorScrollingPositionKeeper(editor);
    keeper.savePosition();
    if (task.getAsBoolean()) keeper.restorePosition(false);
  }

  @Nullable
  public static DocRenderItem getItemAroundOffset(@NotNull Editor editor, int offset) {
    Collection<DocRenderItem> items = editor.getUserData(OUR_ITEMS);
    if (items == null || items.isEmpty()) return null;
    Document document = editor.getDocument();
    if (offset < 0 || offset > document.getTextLength()) return null;
    int line = document.getLineNumber(offset);
    DocRenderItem itemOnAdjacentLine = items.stream().filter(i -> {
      if (!i.isValid()) return false;
      int startLine = document.getLineNumber(i.highlighter.getStartOffset());
      int endLine = document.getLineNumber(i.highlighter.getEndOffset());
      return line >= startLine - 1 && line <= endLine + 1;
    }).min(Comparator.comparingInt(i -> i.highlighter.getStartOffset())).orElse(null);
    if (itemOnAdjacentLine != null) return itemOnAdjacentLine;

    Project project = editor.getProject();
    if (project == null) return null;
    PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
    DocRenderItem foundItem = null;
    int foundStartOffset = 0;
    for (DocRenderItem item : items) {
      if (!item.isValid()) continue;
      PsiDocCommentBase comment = item.getComment();
      if (comment == null) continue;
      PsiElement owner = comment.getOwner();
      if (owner == null) continue;
      TextRange ownerTextRange = owner.getTextRange();
      if (ownerTextRange == null || !ownerTextRange.containsOffset(offset)) continue;
      int startOffset = ownerTextRange.getStartOffset();
      if (foundItem != null && foundStartOffset >= startOffset) continue;
      foundItem = item;
      foundStartOffset = startOffset;
    }
    return foundItem;
  }

  static void resetToDefaultState(@NotNull Editor editor) {
    Collection<DocRenderItem> items = editor.getUserData(OUR_ITEMS);
    if (items == null) return;
    boolean editorSetting = DocRenderManager.isDocRenderingEnabled(editor);
    keepScrollingPositionWhile(editor, () -> {
      List<Runnable> foldingTasks = new ArrayList<>();
      boolean updated = false;
      for (DocRenderItem item : items) {
        if (item.isValid() && (item.inlay == null) == editorSetting) {
          updated |= item.toggle(foldingTasks);
        }
      }
      editor.getFoldingModel().runBatchFoldingOperation(() -> foldingTasks.forEach(Runnable::run), true, false);
      return updated;
    });
  }

  public static EditorCustomElementRenderer createDemoRenderer(@NotNull Editor editor) {
    DocRenderItem item = new DocRenderItem(editor, new TextRange(0, 0), "Rendered documentation with <a href='''>link</a>");
    return new DocRenderer(item);
  }

  private DocRenderItem(@NotNull Editor editor, @NotNull TextRange textRange, @Nullable String textToRender) {
    this.editor = editor;
    this.textToRender = textToRender;
    highlighter = editor.getMarkupModel()
      .addRangeHighlighter(null, textRange.getStartOffset(), textRange.getEndOffset(), 0, HighlighterTargetArea.EXACT_RANGE);
    updateIcon();
  }

  private boolean isValid() {
    return highlighter.isValid() &&
           highlighter.getStartOffset() < highlighter.getEndOffset() &&
           new RelevantOffsets(highlighter).match(foldRegion, inlay);
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
      RelevantOffsets offsets = new RelevantOffsets(highlighter);
      inlay = editor.getInlayModel().addBlockElement(offsets.inlayOffset, false, true, BlockInlayPriority.DOC_RENDER,
                                                     new DocRenderer(this));
      if (inlay != null) {
        Runnable foldingTask = () -> {
          // if this fails (setting 'foldRegion' to null), 'cleanup' method will fix the mess
          foldRegion = foldingModel.createFoldRegion(offsets.foldStartOffset, offsets.foldEndOffset, "", null, true);
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
      if (!DocRenderManager.isDocRenderingEnabled(editor)) {
        // the value won't be updated by DocRenderPass on document modification, so we shouldn't cache the value
        textToRender = null;
      }
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
        return DocumentationManager.getProviderFromElement(file).findDocComment(file, TextRange.create(highlighter));
      }
    }
    return null;
  }

  private static void updateInlays(@NotNull Collection<DocRenderItem> items, boolean recreateContent) {
    DocRenderItemUpdater.getInstance().updateInlays(ContainerUtil.mapNotNull(items, i -> i.inlay), recreateContent);
  }

  private static void updateInlays(@NotNull Editor editor, boolean recreateContent) {
    Collection<DocRenderItem> items = editor.getUserData(OUR_ITEMS);
    if (items != null) updateInlays(items, recreateContent);
  }

  private void updateIcon() {
    boolean iconEnabled = DocRenderDummyLineMarkerProvider.isGutterIconEnabled();
    boolean iconExists = highlighter.getGutterIconRenderer() != null;
    if (iconEnabled != iconExists) {
      if (iconEnabled) {
        highlighter.setGutterIconRenderer(new MyGutterIconRenderer(AllIcons.Gutter.JavadocRead, false));
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

  private void setIconVisible(boolean visible) {
    MyGutterIconRenderer iconRenderer = (MyGutterIconRenderer)highlighter.getGutterIconRenderer();
    if (iconRenderer != null) {
      iconRenderer.setIconVisible(visible);
      int y = editor.visualLineToY(((EditorImpl)editor).offsetToVisualLine(highlighter.getStartOffset()));
      repaintGutter(y);
    }
    if (inlay != null) {
      MyGutterIconRenderer inlayIconRenderer = (MyGutterIconRenderer)inlay.getGutterIconRenderer();
      if (inlayIconRenderer != null) {
        inlayIconRenderer.setIconVisible(visible);
        Rectangle bounds = inlay.getBounds();
        if (bounds != null) {
          repaintGutter(bounds.y);
        }
      }
    }
  }

  private void repaintGutter(int startY) {
    JComponent gutter = (JComponent)editor.getGutter();
    gutter.repaint(0, startY, gutter.getWidth(), startY + editor.getLineHeight());
  }

  @Nullable
  public String getTextToRender() {
    return textToRender;
  }

  private static class RelevantOffsets {
    private final int foldStartOffset;
    private final int foldEndOffset;
    private final int inlayOffset;

    private RelevantOffsets(@NotNull RangeHighlighter highlighter) {
      Document document = highlighter.getDocument();
      int startLine = document.getLineNumber(highlighter.getStartOffset());
      int endLine = document.getLineNumber(highlighter.getEndOffset());
      inlayOffset = foldStartOffset = document.getLineStartOffset(startLine);
      foldEndOffset = endLine < document.getLineCount() - 1 ? document.getLineStartOffset(endLine + 1) : document.getLineEndOffset(endLine);
    }

    private boolean match(FoldRegion foldRegion, Inlay inlay) {
      return foldRegion == null && inlay == null ||
             foldRegion != null && foldRegion.isValid() &&
             foldRegion.getStartOffset() == foldStartOffset && foldRegion.getEndOffset() == foldEndOffset &&
             inlay != null && inlay.isValid() && inlay.getOffset() == inlayOffset;
    }
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
        updateInlays(editor, false);
      }
    }
  }

  private static class MyInlayListener implements InlayModel.Listener {
    @Override
    public void onRemoved(@NotNull Inlay inlay) {
      EditorCustomElementRenderer renderer = inlay.getRenderer();
      if (renderer instanceof DocRenderer) {
        ((DocRenderer)renderer).dispose();
      }
    }
  }

  class MyGutterIconRenderer extends GutterIconRenderer implements DumbAware {
    private final LayeredIcon icon;

    MyGutterIconRenderer(Icon icon, boolean iconVisible) {
      this.icon = new LayeredIcon(icon);
      setIconVisible(iconVisible);
    }

    boolean isIconVisible() {
      return icon.isLayerEnabled(0);
    }

    void setIconVisible(boolean visible) {
      icon.setLayerEnabled(0, visible);
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
        DocFontSizePopup.show(() -> updateInlays(editor, true), editor.getContentComponent());
      }
    }
  }

  private static class IconVisibilityController implements EditorMouseListener, EditorMouseMotionListener, VisibleAreaListener, Disposable {
    private DocRenderItem myCurrentItem;
    private Editor myQueuedEditor;

    @Override
    public void mouseMoved(@NotNull EditorMouseEvent e) {
      doUpdate(e.getEditor(), e);
    }

    @Override
    public void mouseExited(@NotNull EditorMouseEvent e) {
      doUpdate(e.getEditor(), e);
    }

    @Override
    public void visibleAreaChanged(@NotNull VisibleAreaEvent e) {
      Editor editor = e.getEditor();
      if (((EditorImpl)editor).isCursorHidden()) return;
      if (myQueuedEditor == null) {
        myQueuedEditor = editor;
        // delay update: multiple visible area updates within same EDT event will cause only one icon update,
        // and we'll not observe the item in inconsistent state during toggling
        SwingUtilities.invokeLater(() -> {
          if (myQueuedEditor != null && !myQueuedEditor.isDisposed()) {
            doUpdate(myQueuedEditor, null);
          }
          myQueuedEditor = null;
        });
      }
    }

    private void doUpdate(@NotNull Editor editor, @Nullable EditorMouseEvent event) {
      int y = 0;
      int offset = -1;
      if (event == null) {
        PointerInfo info = MouseInfo.getPointerInfo();
        if (info != null) {
          Point screenPoint = info.getLocation();
          JComponent component = editor.getComponent();

          Point componentPoint = new Point(screenPoint);
          SwingUtilities.convertPointFromScreen(componentPoint, component);

          if (new Rectangle(component.getSize()).contains(componentPoint)) {
            Point editorPoint = new Point(screenPoint);
            SwingUtilities.convertPointFromScreen(editorPoint, editor.getContentComponent());
            y = editorPoint.y;
            offset = editor.visualPositionToOffset(new VisualPosition(editor.yToVisualLine(y), 0));
          }
        }
      }
      else {
        y = event.getMouseEvent().getY();
        offset = event.getOffset();
      }
      DocRenderItem item = offset < 0 ? null : findItem(editor, y, offset);
      if (item != myCurrentItem) {
        if (myCurrentItem != null) myCurrentItem.setIconVisible(false);
        myCurrentItem = item;
        if (myCurrentItem != null) myCurrentItem.setIconVisible(true);
      }
    }

    private static DocRenderItem findItem(Editor editor, int y, int neighborOffset) {
      Document document = editor.getDocument();
      int lineNumber = document.getLineNumber(neighborOffset);
      int searchStartOffset = document.getLineStartOffset(Math.max(0, lineNumber - 1));
      int searchEndOffset = document.getLineEndOffset(lineNumber);
      Collection<DocRenderItem> items = editor.getUserData(OUR_ITEMS);
      assert items != null;
      for (DocRenderItem item : items) {
        RangeHighlighter highlighter = item.highlighter;
        if (highlighter.isValid() && highlighter.getStartOffset() <= searchEndOffset && highlighter.getEndOffset() >= searchStartOffset) {
          int itemStartY = 0;
          int itemEndY = 0;
          if (item.inlay == null) {
            itemStartY = editor.visualLineToY(((EditorImpl)editor).offsetToVisualLine(highlighter.getStartOffset()));
            itemEndY = editor.visualLineToY(((EditorImpl)editor).offsetToVisualLine(highlighter.getEndOffset())) + editor.getLineHeight();
          }
          else {
            Rectangle bounds = item.inlay.getBounds();
            if (bounds != null) {
              itemStartY = bounds.y;
              itemEndY = bounds.y + bounds.height;
            }
          }
          if (y >= itemStartY && y < itemEndY) return item;
          break;
        }
      }
      return null;
    }

    @Override
    public void dispose() {
      myCurrentItem = null;
      myQueuedEditor = null;
    }
  }
}
