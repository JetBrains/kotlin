// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.ide.CopyProvider;
import com.intellij.ide.CutProvider;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.injected.editor.MarkupModelWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.LightHighlighterClient;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.TextDrawingCallback;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.UnsafeWeakList;
import com.intellij.util.ui.ButtonlessScrollBarUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.IntFunction;

class EditorWindowImpl extends com.intellij.injected.editor.EditorWindowImpl implements EditorWindow, EditorEx {
  private final DocumentWindowImpl myDocumentWindow;
  private final EditorImpl myDelegate;
  private volatile PsiFile myInjectedFile;
  private final boolean myOneLine;
  private final CaretModelWindow myCaretModelDelegate;
  private final SelectionModelWindow mySelectionModelDelegate;
  private static final Collection<EditorWindowImpl> allEditors = new UnsafeWeakList<>(); // guarded by allEditors
  private volatile boolean myDisposed;
  private final MarkupModelWindow myMarkupModelDelegate;
  private final MarkupModelWindow myDocumentMarkupModelDelegate;
  private final FoldingModelWindow myFoldingModelWindow;
  private final SoftWrapModelWindow mySoftWrapModel;
  private final InlayModelWindow myInlayModel;

  @NotNull
  static Editor create(@NotNull final DocumentWindowImpl documentRange, @NotNull final EditorImpl editor, @NotNull final PsiFile injectedFile) {
    assert documentRange.isValid();
    assert injectedFile.isValid();
    EditorWindowImpl window;
    synchronized (allEditors) {
      for (EditorWindowImpl editorWindow : allEditors) {
        if (editorWindow.getDocument() == documentRange && editorWindow.getDelegate() == editor) {
          editorWindow.myInjectedFile = injectedFile;
          if (editorWindow.isValid()) {
            return editorWindow;
          }
        }
      }
      window = new EditorWindowImpl(documentRange, editor, injectedFile, documentRange.isOneLine());
      allEditors.add(window);
    }
    window.checkValid();
    return window;
  }

  private EditorWindowImpl(@NotNull DocumentWindowImpl documentWindow,
                           @NotNull final EditorImpl delegate,
                           @NotNull PsiFile injectedFile,
                           boolean oneLine) {
    myDocumentWindow = documentWindow;
    myDelegate = delegate;
    myInjectedFile = injectedFile;
    myOneLine = oneLine;
    myCaretModelDelegate = new CaretModelWindow(myDelegate.getCaretModel(), this);
    mySelectionModelDelegate = new SelectionModelWindow(myDelegate, myDocumentWindow,this);
    myMarkupModelDelegate = new MarkupModelWindow(myDelegate.getMarkupModel(), myDocumentWindow);
    myDocumentMarkupModelDelegate = new MarkupModelWindow(myDelegate.getFilteredDocumentMarkupModel(), myDocumentWindow);
    myFoldingModelWindow = new FoldingModelWindow(delegate.getFoldingModel(), documentWindow, this);
    mySoftWrapModel = new SoftWrapModelWindow();
    myInlayModel = new InlayModelWindow();
  }

  static void disposeInvalidEditors() {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    synchronized (allEditors) {
      Iterator<EditorWindowImpl> iterator = allEditors.iterator();
      while (iterator.hasNext()) {
        EditorWindowImpl editorWindow = iterator.next();
        if (!editorWindow.isValid()) {
          disposeEditor(editorWindow);
          iterator.remove();
        }
      }
    }
  }

  private static void disposeEditor(@NotNull EditorWindow editorWindow) {
    EditorWindowImpl impl = (EditorWindowImpl)editorWindow;
    impl.dispose();

    InjectedLanguageUtil.clearCaches(impl.myInjectedFile, impl.getDocument());
  }

  static void disposeEditorFor(@NotNull DocumentWindow documentWindow) {
    synchronized (allEditors) {
      for (Iterator<EditorWindowImpl> iterator = allEditors.iterator(); iterator.hasNext(); ) {
        EditorWindowImpl editor = iterator.next();
        if (InjectionRegistrarImpl.intersect(editor.getDocument(), (DocumentWindowImpl)documentWindow)) {
          disposeEditor(editor);
          iterator.remove();
          break;
        }
      }
    }
  }


  @Override
  public boolean isValid() {
    return !isDisposed() && !myInjectedFile.getProject().isDisposed() && myInjectedFile.isValid() && myDocumentWindow.isValid();
  }

  private void checkValid() {
    PsiUtilCore.ensureValid(myInjectedFile);
    if (!isValid()) {
      StringBuilder reason = new StringBuilder("Not valid");
      if (myDisposed) reason.append("; editorWindow: disposed");
      if (!myDocumentWindow.isValid()) reason.append("; documentWindow: invalid");
      if (myDelegate.isDisposed()) reason.append("; editor: disposed");
      if (myInjectedFile.getProject().isDisposed()) reason.append("; project: disposed");
      throw new AssertionError(reason.toString());
    }
  }

  @Override
  @NotNull
  public PsiFile getInjectedFile() {
    return myInjectedFile;
  }

  @Override
  @NotNull
  public LogicalPosition hostToInjected(@NotNull LogicalPosition hPos) {
    checkValid();
    DocumentEx hostDocument = myDelegate.getDocument();
    int hLineEndOffset = hPos.line >= hostDocument.getLineCount() ? hostDocument.getTextLength() : hostDocument.getLineEndOffset(hPos.line);
    LogicalPosition hLineEndPos = myDelegate.offsetToLogicalPosition(hLineEndOffset);
    if (hLineEndPos.column < hPos.column) {
      // in virtual space
      LogicalPosition iPos = myDocumentWindow.hostToInjectedInVirtualSpace(hPos);
      if (iPos != null) {
        return iPos;
      }
    }

    int hOffset = myDelegate.logicalPositionToOffset(hPos);
    int iOffset = myDocumentWindow.hostToInjected(hOffset);
    return offsetToLogicalPosition(iOffset);
  }

  @Override
  @NotNull
  public LogicalPosition injectedToHost(@NotNull LogicalPosition pos) {
    checkValid();

    int offset = logicalPositionToOffset(pos);
    LogicalPosition samePos = offsetToLogicalPosition(offset);

    int virtualSpaceDelta = offset < myDocumentWindow.getTextLength() && samePos.line == pos.line && samePos.column < pos.column ?
                            pos.column - samePos.column : 0;

    LogicalPosition hostPos = myDelegate.offsetToLogicalPosition(myDocumentWindow.injectedToHost(offset));
    return new LogicalPosition(hostPos.line, hostPos.column + virtualSpaceDelta);
  }

  private void dispose() {
    assert !myDisposed;
    myCaretModelDelegate.disposeModel();

    for (EditorMouseListener wrapper : myEditorMouseListeners.wrappers()) {
      myDelegate.removeEditorMouseListener(wrapper);
    }
    myEditorMouseListeners.clear();
    for (EditorMouseMotionListener wrapper : myEditorMouseMotionListeners.wrappers()) {
      myDelegate.removeEditorMouseMotionListener(wrapper);
    }
    myEditorMouseMotionListeners.clear();

    myDisposed = true;
    Disposer.dispose(myDocumentWindow);
  }

  @Override
  public void setViewer(boolean isViewer) {
    myDelegate.setViewer(isViewer);
  }

  @Override
  public boolean isViewer() {
    return myDelegate.isViewer();
  }

  @Override
  public boolean isRendererMode() {
    return myDelegate.isRendererMode();
  }

  @Override
  public void setRendererMode(final boolean isRendererMode) {
    myDelegate.setRendererMode(isRendererMode);
  }

  @Override
  public void setFile(final VirtualFile vFile) {
    myDelegate.setFile(vFile);
  }

  @Override
  public void setHeaderComponent(@Nullable JComponent header) {

  }

  @Override
  public boolean hasHeaderComponent() {
    return false;
  }

  @Override
  @Nullable
  public JComponent getHeaderComponent() {
    return null;
  }

  @Override
  public TextDrawingCallback getTextDrawingCallback() {
    return myDelegate.getTextDrawingCallback();
  }

  @Override
  @NotNull
  public SelectionModel getSelectionModel() {
    return mySelectionModelDelegate;
  }

  @Override
  @NotNull
  public MarkupModelEx getMarkupModel() {
    return myMarkupModelDelegate;
  }

  @NotNull
  @Override
  public MarkupModelEx getFilteredDocumentMarkupModel() {
    return myDocumentMarkupModelDelegate;
  }

  @Override
  @NotNull
  public FoldingModelEx getFoldingModel() {
    return myFoldingModelWindow;
  }

  @Override
  @NotNull
  public CaretModel getCaretModel() {
    return myCaretModelDelegate;
  }

  @Override
  @NotNull
  public ScrollingModelEx getScrollingModel() {
    return myDelegate.getScrollingModel();
  }

  @Override
  @NotNull
  public SoftWrapModelEx getSoftWrapModel() {
    return mySoftWrapModel;
  }

  @Override
  @NotNull
  public EditorSettings getSettings() {
    return myDelegate.getSettings();
  }

  @NotNull
  @Override
  public InlayModel getInlayModel() {
    return myInlayModel;
  }

  @NotNull
  @Override
  public EditorKind getEditorKind() {
    return myDelegate.getEditorKind();
  }

  @Override
  public void reinitSettings() {
    myDelegate.reinitSettings();
  }

  @Override
  public void setFontSize(final int fontSize) {
    myDelegate.setFontSize(fontSize);
  }

  @Override
  public void setHighlighter(@NotNull final EditorHighlighter highlighter) {
    myDelegate.setHighlighter(highlighter);
  }

  @NotNull
  @Override
  public EditorHighlighter getHighlighter() {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    SyntaxHighlighter syntaxHighlighter =
      SyntaxHighlighterFactory.getSyntaxHighlighter(myInjectedFile.getLanguage(), getProject(), myInjectedFile.getVirtualFile());
    EditorHighlighter highlighter = HighlighterFactory.createHighlighter(syntaxHighlighter, scheme);
    highlighter.setText(getDocument().getText());
    highlighter.setEditor(new LightHighlighterClient(getDocument(), getProject()));
    return highlighter;
  }

  @Override
  public JComponent getPermanentHeaderComponent() {
    return myDelegate.getPermanentHeaderComponent();
  }

  @Override
  public void setPermanentHeaderComponent(JComponent component) {
    myDelegate.setPermanentHeaderComponent(component);
  }

  @Override
  @NotNull
  public JComponent getContentComponent() {
    return myDelegate.getContentComponent();
  }

  @NotNull
  @Override
  public EditorGutterComponentEx getGutterComponentEx() {
    return myDelegate.getGutterComponentEx();
  }

  @Override
  public void addPropertyChangeListener(@NotNull final PropertyChangeListener listener) {
    myDelegate.addPropertyChangeListener(listener);
  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener, @NotNull Disposable parentDisposable) {
    myDelegate.addPropertyChangeListener(listener, parentDisposable);
  }

  @Override
  public void removePropertyChangeListener(@NotNull final PropertyChangeListener listener) {
    myDelegate.removePropertyChangeListener(listener);
  }

  @Override
  public void setInsertMode(final boolean mode) {
    myDelegate.setInsertMode(mode);
  }

  @Override
  public boolean isInsertMode() {
    return myDelegate.isInsertMode();
  }

  @Override
  public void setColumnMode(final boolean mode) {
    myDelegate.setColumnMode(mode);
  }

  @Override
  public boolean isColumnMode() {
    return myDelegate.isColumnMode();
  }

  @Override
  @NotNull
  public VisualPosition xyToVisualPosition(@NotNull final Point p) {
    return logicalToVisualPosition(xyToLogicalPosition(p));
  }

  @NotNull
  @Override
  public VisualPosition xyToVisualPosition(@NotNull Point2D p) {
    checkValid();
    Point2D pp = p.getX() >= 0 && p.getY() >= 0 ? p : new Point2D.Double(Math.max(p.getX(), 0), Math.max(p.getY(), 0));
    LogicalPosition hostPos = myDelegate.visualToLogicalPosition(myDelegate.xyToVisualPosition(pp));
    return logicalToVisualPosition(hostToInjected(hostPos));
  }

  @Override
  @NotNull
  public VisualPosition offsetToVisualPosition(final int offset) {
    return logicalToVisualPosition(offsetToLogicalPosition(offset));
  }

  @Override
  @NotNull
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    return logicalToVisualPosition(offsetToLogicalPosition(offset).leanForward(leanForward));
  }

  @Override
  @NotNull
  public LogicalPosition offsetToLogicalPosition(final int offset) {
    checkValid();
    int lineNumber = myDocumentWindow.getLineNumber(offset);
    int lineStartOffset = myDocumentWindow.getLineStartOffset(lineNumber);
    int column = calcLogicalColumnNumber(offset-lineStartOffset, lineNumber, lineStartOffset);
    return new LogicalPosition(lineNumber, column);
  }

  @NotNull
  @Override
  public EditorColorsScheme createBoundColorSchemeDelegate(@Nullable EditorColorsScheme customGlobalScheme) {
    return myDelegate.createBoundColorSchemeDelegate(customGlobalScheme);
  }

  @Override
  @NotNull
  public LogicalPosition xyToLogicalPosition(@NotNull final Point p) {
    checkValid();
    LogicalPosition hostPos = myDelegate.xyToLogicalPosition(p);
    return hostToInjected(hostPos);
  }

  @Override
  @NotNull
  public Point logicalPositionToXY(@NotNull final LogicalPosition pos) {
    checkValid();
    LogicalPosition hostPos = injectedToHost(pos);
    return myDelegate.logicalPositionToXY(hostPos);
  }

  @Override
  @NotNull
  public Point visualPositionToXY(@NotNull final VisualPosition pos) {
    checkValid();
    return logicalPositionToXY(visualToLogicalPosition(pos));
  }

  @NotNull
  @Override
  public Point2D visualPositionToPoint2D(@NotNull VisualPosition pos) {
    checkValid();
    LogicalPosition hostLogical = injectedToHost(visualToLogicalPosition(pos));
    VisualPosition hostVisual = myDelegate.logicalToVisualPosition(hostLogical);
    return myDelegate.visualPositionToPoint2D(hostVisual);
  }

  @Override
  public void repaint(final int startOffset, final int endOffset) {
    checkValid();
    myDelegate.repaint(myDocumentWindow.injectedToHost(startOffset), myDocumentWindow.injectedToHost(endOffset));
  }

  @Override
  @NotNull
  public DocumentWindowImpl getDocument() {
    return myDocumentWindow;
  }

  @Override
  @NotNull
  public JComponent getComponent() {
    return myDelegate.getComponent();
  }

  private final ListenerWrapperMap<EditorMouseListener> myEditorMouseListeners = new ListenerWrapperMap<>();
  @Override
  public void addEditorMouseListener(@NotNull final EditorMouseListener listener) {
    checkValid();
    EditorMouseListener wrapper = new EditorMouseListener() {
      @Override
      public void mousePressed(@NotNull EditorMouseEvent e) {
        listener.mousePressed(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }

      @Override
      public void mouseClicked(@NotNull EditorMouseEvent e) {
        listener.mouseClicked(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }

      @Override
      public void mouseReleased(@NotNull EditorMouseEvent e) {
        listener.mouseReleased(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }

      @Override
      public void mouseEntered(@NotNull EditorMouseEvent e) {
        listener.mouseEntered(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }

      @Override
      public void mouseExited(@NotNull EditorMouseEvent e) {
        listener.mouseExited(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }
    };
    myEditorMouseListeners.registerWrapper(listener, wrapper);

    myDelegate.addEditorMouseListener(wrapper);
  }

  @Override
  public void removeEditorMouseListener(@NotNull final EditorMouseListener listener) {
    EditorMouseListener wrapper = myEditorMouseListeners.removeWrapper(listener);
    // HintManager might have an old editor instance
    if (wrapper != null) {
      myDelegate.removeEditorMouseListener(wrapper);
    }
  }

  private final ListenerWrapperMap<EditorMouseMotionListener> myEditorMouseMotionListeners = new ListenerWrapperMap<>();
  @Override
  public void addEditorMouseMotionListener(@NotNull final EditorMouseMotionListener listener) {
    checkValid();
    EditorMouseMotionListener wrapper = new EditorMouseMotionListener() {
      @Override
      public void mouseMoved(@NotNull EditorMouseEvent e) {
        listener.mouseMoved(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }

      @Override
      public void mouseDragged(@NotNull EditorMouseEvent e) {
        listener.mouseDragged(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }
    };
    myEditorMouseMotionListeners.registerWrapper(listener, wrapper);
    myDelegate.addEditorMouseMotionListener(wrapper);
  }

  @Override
  public void removeEditorMouseMotionListener(@NotNull final EditorMouseMotionListener listener) {
    EditorMouseMotionListener wrapper = myEditorMouseMotionListeners.removeWrapper(listener);
    if (wrapper != null) {
      myDelegate.removeEditorMouseMotionListener(wrapper);
    }
  }

  @Override
  public boolean isDisposed() {
    return myDisposed || myDelegate.isDisposed();
  }

  @Override
  public void setBackgroundColor(final Color color) {
    myDelegate.setBackgroundColor(color);
  }

  @NotNull
  @Override
  public Color getBackgroundColor() {
    return myDelegate.getBackgroundColor();
  }

  @Override
  public int getMaxWidthInRange(final int startOffset, final int endOffset) {
    return myDelegate.getMaxWidthInRange(startOffset, endOffset);
  }

  @Override
  public int getLineHeight() {
    return myDelegate.getLineHeight();
  }

  @Override
  public Dimension getContentSize() {
    return myDelegate.getContentSize();
  }

  @NotNull
  @Override
  public JScrollPane getScrollPane() {
    return myDelegate.getScrollPane();
  }

  @Override
  public void setBorder(Border border) {
    myDelegate.setBorder(border);
  }

  @Override
  public Insets getInsets() {
    return myDelegate.getInsets();
  }

  @Override
  public int logicalPositionToOffset(@NotNull final LogicalPosition pos) {
    int lineStartOffset = myDocumentWindow.getLineStartOffset(pos.line);
    return calcOffset(pos.column, pos.line, lineStartOffset);
  }

  private int calcLogicalColumnNumber(int offsetInLine, int lineNumber, int lineStartOffset) {
    if (myDocumentWindow.getTextLength() == 0) return 0;

    if (offsetInLine==0) return 0;
    int end = myDocumentWindow.getLineEndOffset(lineNumber);
    if (offsetInLine > end- lineStartOffset) offsetInLine = end - lineStartOffset;

    CharSequence text = myDocumentWindow.getCharsSequence();
    return EditorUtil.calcColumnNumber(this, text, lineStartOffset, lineStartOffset +offsetInLine);
  }

  private int calcOffset(int col, int lineNumber, int lineStartOffset) {
    CharSequence text = myDocumentWindow.getImmutableCharSequence();
    int tabSize = EditorUtil.getTabSize(myDelegate);
    int end = myDocumentWindow.getLineEndOffset(lineNumber);
    int currentColumn = 0;
    for (int i = lineStartOffset; i < end; i++) {
      char c = text.charAt(i);
      if (c == '\t') {
        currentColumn = (currentColumn / tabSize + 1) * tabSize;
      }
      else {
        currentColumn++;
      }
      if (col < currentColumn) return i;
    }
    return end;
  }

  // assuming there is no folding in injected documents
  @Override
  @NotNull
  public VisualPosition logicalToVisualPosition(@NotNull final LogicalPosition pos) {
    checkValid();
    return new VisualPosition(pos.line, pos.column);
  }

  @Override
  @NotNull
  public LogicalPosition visualToLogicalPosition(@NotNull final VisualPosition pos) {
    checkValid();
    return new LogicalPosition(pos.line, pos.column);
  }

  @NotNull
  @Override
  public DataContext getDataContext() {
    return myDelegate.getDataContext();
  }

  @Override
  public EditorMouseEventArea getMouseEventArea(@NotNull final MouseEvent e) {
    return myDelegate.getMouseEventArea(e);
  }

  @Override
  public boolean setCaretVisible(final boolean b) {
    return myDelegate.setCaretVisible(b);
  }

  @Override
  public boolean setCaretEnabled(boolean enabled) {
    return myDelegate.setCaretEnabled(enabled);
  }

  @Override
  public void addFocusListener(@NotNull final FocusChangeListener listener) {
    myDelegate.addFocusListener(listener);
  }

  @Override
  public void addFocusListener(@NotNull FocusChangeListener listener, @NotNull Disposable parentDisposable) {
    myDelegate.addFocusListener(listener, parentDisposable);
  }

  @Override
  public Project getProject() {
    return myDelegate.getProject();
  }

  @Override
  public boolean isOneLineMode() {
    return myOneLine;
  }

  @Override
  public void setOneLineMode(final boolean isOneLineMode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmbeddedIntoDialogWrapper() {
    return myDelegate.isEmbeddedIntoDialogWrapper();
  }

  @Override
  public void setEmbeddedIntoDialogWrapper(final boolean b) {
    myDelegate.setEmbeddedIntoDialogWrapper(b);
  }

  @Override
  public VirtualFile getVirtualFile() {
    return myDelegate.getVirtualFile();
  }

  @Override
  public CopyProvider getCopyProvider() {
    return myDelegate.getCopyProvider();
  }

  @Override
  public CutProvider getCutProvider() {
    return myDelegate.getCutProvider();
  }

  @Override
  public PasteProvider getPasteProvider() {
    return myDelegate.getPasteProvider();
  }

  @Override
  public DeleteProvider getDeleteProvider() {
    return myDelegate.getDeleteProvider();
  }

  @Override
  public void setColorsScheme(@NotNull final EditorColorsScheme scheme) {
    myDelegate.setColorsScheme(scheme);
  }

  @Override
  @NotNull
  public EditorColorsScheme getColorsScheme() {
    return myDelegate.getColorsScheme();
  }

  @Override
  public void setVerticalScrollbarOrientation(final int type) {
    myDelegate.setVerticalScrollbarOrientation(type);
  }

  @Override
  public int getVerticalScrollbarOrientation() {
    return myDelegate.getVerticalScrollbarOrientation();
  }

  @Override
  public void setVerticalScrollbarVisible(final boolean b) {
    myDelegate.setVerticalScrollbarVisible(b);
  }

  @Override
  public void setHorizontalScrollbarVisible(final boolean b) {
    myDelegate.setHorizontalScrollbarVisible(b);
  }

  @Override
  public boolean processKeyTyped(@NotNull final KeyEvent e) {
    return myDelegate.processKeyTyped(e);
  }

  @Override
  @NotNull
  public EditorGutter getGutter() {
    return myDelegate.getGutter();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final EditorWindowImpl that = (EditorWindowImpl)o;

    DocumentWindow thatWindow = that.getDocument();
    return myDelegate.equals(that.myDelegate) && myDocumentWindow.equals(thatWindow);
  }

  @Override
  public int hashCode() {
    return myDocumentWindow.hashCode();
  }

  @NotNull
  @Override
  public Editor getDelegate() {
    return myDelegate;
  }

  @NotNull
  @Override
  public IndentsModel getIndentsModel() {
    return myDelegate.getIndentsModel();
  }

  @Override
  public void setPlaceholder(@Nullable CharSequence text) {
    myDelegate.setPlaceholder(text);
  }

  @Override
  public void setPlaceholderAttributes(@Nullable TextAttributes attributes) {
    myDelegate.setPlaceholderAttributes(attributes);
  }

  @Override
  public void setShowPlaceholderWhenFocused(boolean show) {
    myDelegate.setShowPlaceholderWhenFocused(show);
  }

  @Override
  public boolean isStickySelection() {
    return myDelegate.isStickySelection();
  }

  @Override
  public void setStickySelection(boolean enable) {
    myDelegate.setStickySelection(enable);
  }

  @Override
  public boolean isPurePaintingMode() {
    return myDelegate.isPurePaintingMode();
  }

  @Override
  public void setPurePaintingMode(boolean enabled) {
    myDelegate.setPurePaintingMode(enabled);
  }

  @Override
  public void registerLineExtensionPainter(IntFunction<Collection<LineExtensionInfo>> lineExtensionPainter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerScrollBarRepaintCallback(@Nullable ButtonlessScrollBarUI.ScrollbarRepaintCallback callback) {
    myDelegate.registerScrollBarRepaintCallback(callback);
  }

  @Override
  public void setPrefixTextAndAttributes(@Nullable String prefixText, @Nullable TextAttributes attributes) {
    myDelegate.setPrefixTextAndAttributes(prefixText, attributes);
  }

  @Override
  public int getPrefixTextWidthInPixels() {
    return myDelegate.getPrefixTextWidthInPixels();
  }

  @Override
  public String toString() {
    return super.toString() + "[disposed=" + myDisposed + "; valid=" + isValid() + "]";
  }

  @Override
  public int getExpectedCaretOffset() {
    return myDocumentWindow.hostToInjected(myDelegate.getExpectedCaretOffset());
  }

  @Override
  public void setContextMenuGroupId(@Nullable String groupId) {
    myDelegate.setContextMenuGroupId(groupId);
  }

  @Nullable
  @Override
  public String getContextMenuGroupId() {
    return myDelegate.getContextMenuGroupId();
  }

  @Override
  public void installPopupHandler(@NotNull EditorPopupHandler popupHandler) {
    myDelegate.installPopupHandler(popupHandler);
  }

  @Override
  public void uninstallPopupHandler(@NotNull EditorPopupHandler popupHandler) {
    myDelegate.installPopupHandler(popupHandler);
  }

  @Override
  public void setCustomCursor(@NotNull Object requestor, @Nullable Cursor cursor) {
    myDelegate.setCustomCursor(requestor, cursor);
  }

  @Override
  public int getAscent() {
    return myDelegate.getAscent();
  }
}
