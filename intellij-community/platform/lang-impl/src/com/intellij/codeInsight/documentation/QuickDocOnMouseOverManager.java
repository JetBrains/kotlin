// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation;

import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.impl.EditorMouseHoverPopupControl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.psi.*;
import com.intellij.reference.SoftReference;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Serves as a facade to the 'show quick doc on mouse over an element' functionality.
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 */
public final class QuickDocOnMouseOverManager {
  private static final Logger LOG = Logger.getInstance(QuickDocOnMouseOverManager.class);

  @NotNull private final MyEditorMouseListener     myMouseListener       = new MyEditorMouseListener();
  @NotNull private final VisibleAreaListener       myVisibleAreaListener = new MyVisibleAreaListener();
  @NotNull private final CaretListener             myCaretListener       = new MyCaretListener();
  @NotNull private final DocumentListener          myDocumentListener    = new MyDocumentListener();
  @NotNull private final Alarm                     myAlarm;
  @NotNull private final Map<Document, Boolean>    myMonitoredDocuments  = ContainerUtil.createWeakMap();

  private final Map<Editor, Reference<PsiElement> /* PSI element which is located under the current mouse position */> myActiveElements
    = ContainerUtil.createWeakMap();

  /** Holds a reference (if any) to the documentation manager used last time to show an 'auto quick doc' popup. */
  @Nullable private WeakReference<DocumentationManager> myDocumentationManager;

  private           boolean             myEnabled;
  private           boolean             myApplicationActive;

  private MyShowQuickDocRequest myCurrentRequest; // accessed only in EDT

  public QuickDocOnMouseOverManager() {
    Application app = ApplicationManager.getApplication();
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, app);

    EditorFactory factory = EditorFactory.getInstance();
    if (factory != null) {
      factory.addEditorFactoryListener(new MyEditorFactoryListener(), app);
    }

    app.getMessageBus().connect().subscribe(
      ApplicationActivationListener.TOPIC, new ApplicationActivationListener() {
        @Override
        public void applicationActivated(@NotNull IdeFrame ideFrame) {
          myApplicationActive = true;
        }

        @Override
        public void applicationDeactivated(@NotNull IdeFrame ideFrame) {
          myApplicationActive = false;
          closeQuickDocIfPossible();
        }
      });
  }

  /**
   * Instructs the manager to enable or disable 'show quick doc automatically when the mouse goes over an editor element' mode.
   *
   * @param enabled  flag that identifies if quick doc should be automatically shown
   */
  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
    myApplicationActive = enabled;
    if (!enabled) {
      closeQuickDocIfPossible();
      myAlarm.cancelAllRequests();
    }
    EditorFactory factory = EditorFactory.getInstance();
    if (factory == null) {
      return;
    }
    for (Editor editor : factory.getAllEditors()) {
      if (enabled) {
        registerListeners(editor);
      }
      else {
        unRegisterListeners(editor);
      }
    }
  }

  private void registerListeners(@NotNull Editor editor) {
    editor.addEditorMouseListener(myMouseListener);
    editor.addEditorMouseMotionListener(myMouseListener);
    editor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
    editor.getCaretModel().addCaretListener(myCaretListener);

    Document document = editor.getDocument();
    if (myMonitoredDocuments.put(document, Boolean.TRUE) == null) {
      document.addDocumentListener(myDocumentListener);
    }
  }

  private void unRegisterListeners(@NotNull Editor editor) {
    editor.removeEditorMouseListener(myMouseListener);
    editor.removeEditorMouseMotionListener(myMouseListener);
    editor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
    editor.getCaretModel().removeCaretListener(myCaretListener);

    Document document = editor.getDocument();
    if (myMonitoredDocuments.remove(document) != null) {
      document.removeDocumentListener(myDocumentListener);
    }
  }

  private void processMouseExited() {
    myActiveElements.clear();
    myAlarm.cancelAllRequests();
  }

  private void processMouseMove(@NotNull EditorMouseEvent e) {
    if (!myApplicationActive || !myEnabled || e.getArea() != EditorMouseEventArea.EDITING_AREA) {
      // Skip if the mouse is not at the editing area.
      closeQuickDocIfPossible();
      return;
    }

    if (e.getMouseEvent().getModifiers() != 0) {
      // Don't show the control when any modifier is active (e.g. Ctrl or Alt is hold). There is a common situation that a user
      // wants to navigate via Ctrl+click or perform quick evaluate by Alt+click.
      return;
    }

    Editor editor = e.getEditor();
    if (EditorMouseHoverPopupControl.arePopupsDisabled(editor)) {
      return;
    }

    if (editor.isOneLineMode()) {
      // Don't want auto quick doc to mess at, say, editor used for debugger condition.
      return;
    }

    Project project = editor.getProject();
    if (project == null) {
      return;
    }

    DocumentationManager documentationManager = DocumentationManager.getInstance(project);
    JBPopup hint = documentationManager.getDocInfoHint();
    if (hint != null) {

      // Skip the event if the control is shown because of explicit 'show quick doc' action call.
      DocumentationManager manager = getDocManager();
      if (manager == null || !manager.isCloseOnSneeze()) {
        return;
      }

      // Skip the event if the mouse is under the opened quick doc control.
      Point hintLocation = hint.getLocationOnScreen();
      Dimension hintSize = hint.getSize();
      int mouseX = e.getMouseEvent().getXOnScreen();
      int mouseY = e.getMouseEvent().getYOnScreen();
      int resizeZoneWidth = editor.getLineHeight();
      if (mouseX >= hintLocation.x - resizeZoneWidth && mouseX <= hintLocation.x + hintSize.width + resizeZoneWidth &&
                     mouseY >= hintLocation.y - resizeZoneWidth && mouseY <= hintLocation.y + hintSize.height + resizeZoneWidth)
      {
        return;
      }
    }

    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (psiFile == null) {
      closeQuickDocIfPossible();
      return;
    }

    Point point = e.getMouseEvent().getPoint();
    if (editor instanceof EditorEx && ((EditorEx)editor).getFoldingModel().getFoldingPlaceholderAt(point) != null) {
      closeQuickDocIfPossible();
      return;
    }

    VisualPosition visualPosition = editor.xyToVisualPosition(point);
    if (editor.getSoftWrapModel().isInsideOrBeforeSoftWrap(visualPosition)) {
      closeQuickDocIfPossible();
      return;
    }

    int mouseOffset = editor.logicalPositionToOffset(editor.visualToLogicalPosition(visualPosition));
    PsiElement elementUnderMouse = psiFile.findElementAt(mouseOffset);
    if (elementUnderMouse == null || elementUnderMouse instanceof PsiWhiteSpace || elementUnderMouse instanceof PsiPlainText) {
      closeQuickDocIfPossible();
      return;
    }

    if (elementUnderMouse.equals(SoftReference.dereference(myActiveElements.get(editor)))
        && (!myAlarm.isEmpty() // Request to show documentation for the target component has been already queued.
            || hint != null)) // Documentation for the target component is being shown.
    {
      return;
    }
    allowUpdateFromContext(project, false);
    closeQuickDocIfPossible();
    myActiveElements.put(editor, new WeakReference<>(elementUnderMouse));

    myAlarm.cancelAllRequests();
    if (myCurrentRequest != null) myCurrentRequest.cancel();
    myCurrentRequest = new MyShowQuickDocRequest(documentationManager, editor, mouseOffset, elementUnderMouse);
    myAlarm.addRequest(myCurrentRequest, EditorSettingsExternalizable.getInstance().getTooltipsDelay());
  }

  private void closeQuickDocIfPossible() {
    myAlarm.cancelAllRequests();
    DocumentationManager docManager = getDocManager();
    if (docManager != null) {
      JBPopup hint = docManager.getDocInfoHint();
      if (hint != null) hint.cancel();
    }
  }

  private void allowUpdateFromContext(Project project, boolean allow) {
    DocumentationManager documentationManager = getDocManager();
    if (documentationManager != null && documentationManager.getProject(null) == project) {
      documentationManager.setAllowContentUpdateFromContext(allow);
    }
  }

  @Nullable
  private DocumentationManager getDocManager() {
    return SoftReference.dereference(myDocumentationManager);
  }

  @Nullable
  private Editor getEditor() {
    DocumentationManager manager = getDocManager();
    return manager == null ? null : manager.getEditor();
  }

  private class MyShowQuickDocRequest implements Runnable {
    @NotNull private final DocumentationManager docManager;
    @NotNull private final Editor editor;
    private final int offset;
    @NotNull private final PsiElement originalElement;
    @NotNull private final ProgressIndicator myProgressIndicator = new ProgressIndicatorBase();

    private MyShowQuickDocRequest(@NotNull DocumentationManager docManager, @NotNull Editor editor, int offset,
                                  @NotNull PsiElement originalElement) {
      this.docManager = docManager;
      this.editor = editor;
      this.offset = offset;
      this.originalElement = originalElement;
    }

    private void cancel() {
      myProgressIndicator.cancel();
    }

    @Override
    public void run() {
      Ref<PsiElement> targetElementRef = new Ref<>();

      QuickDocUtil.runInReadActionWithWriteActionPriorityWithRetries(() -> {
        if (originalElement.isValid()) {
          targetElementRef.set(docManager.findTargetElement(editor, offset, originalElement.getContainingFile(), originalElement));
        }
      }, 5000, 100, myProgressIndicator);

      Ref<String> documentationRef = new Ref<>();
      if (!targetElementRef.isNull()) {
        try {
          documentationRef.set(docManager.generateDocumentation(targetElementRef.get(), originalElement, true));
        }
        catch (Exception e) {
          LOG.info(e);
        }
      }

      ApplicationManager.getApplication().invokeLater(() -> {
        myCurrentRequest = null;

        if (editor.isDisposed() ||
            (IdeTooltipManager.getInstance().hasCurrent() || IdeTooltipManager.getInstance().hasScheduled()) &&
            !docManager.hasActiveDockedDocWindow()) {
          return;
        }

        PsiElement targetElement = targetElementRef.get();
        String documentation = documentationRef.get();
        if (targetElement == null || StringUtil.isEmpty(documentation)) {
          closeQuickDocIfPossible();
          return;
        }

        myAlarm.cancelAllRequests();

        if (!originalElement.equals(SoftReference.dereference(myActiveElements.get(editor)))) {
          return;
        }

        // Skip the request if there is a control shown as a result of explicit 'show quick doc' (Ctrl + Q) invocation.
        if (docManager.getDocInfoHint() != null && !docManager.isCloseOnSneeze()) {
          return;
        }

        editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION,
                           editor.offsetToVisualPosition(originalElement.getTextRange().getStartOffset()));
        docManager.showJavaDocInfo(editor, targetElement, originalElement, new MyCloseDocCallback(editor), documentation, true, false);
        myDocumentationManager = new WeakReference<>(docManager);
      }, ApplicationManager.getApplication().getNoneModalityState());
    }
  }

  private class MyCloseDocCallback implements Runnable {
    @NotNull
    private final Editor myEditor;

    private MyCloseDocCallback(@NotNull Editor editor) {myEditor = editor;}

    @Override
    public void run() {
      myActiveElements.clear();
      myEditor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, null);
      myDocumentationManager = null;
    }
  }

  private class MyEditorFactoryListener implements EditorFactoryListener {
    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
      if (myEnabled) {
        registerListeners(event.getEditor());
      }
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
      if (myEnabled) {
        // We do this in the 'if' block because editor logs an error on attempt to remove already released listener.
        unRegisterListeners(event.getEditor());
      }
    }
  }

  private class MyEditorMouseListener implements EditorMouseMotionListener, EditorMouseListener {
    @Override
    public void mouseExited(@NotNull EditorMouseEvent e) {
      if (Registry.is("editor.new.mouse.hover.popups")) return;
      processMouseExited();
    }

    @Override
    public void mouseMoved(@NotNull EditorMouseEvent e) {
      if (Registry.is("editor.new.mouse.hover.popups")) return;
      processMouseMove(e);
    }
  }

  private class MyVisibleAreaListener implements VisibleAreaListener {
    @Override
    public void visibleAreaChanged(@NotNull VisibleAreaEvent e) {
      if (Registry.is("editor.new.mouse.hover.popups")) return;
      Editor editor = getEditor();
      if (editor == null || editor == e.getEditor()) {
        closeQuickDocIfPossible();
      }
    }
  }

  private class MyCaretListener implements CaretListener {
    @Override
    public void caretPositionChanged(@NotNull CaretEvent e) {
      if (Registry.is("editor.new.mouse.hover.popups")) return;
      Editor editor = getEditor();
      if (editor == null || editor == e.getEditor()) {
        allowUpdateFromContext(e.getEditor().getProject(), true);
        closeQuickDocIfPossible();
      }
    }
  }

  private class MyDocumentListener implements DocumentListener {
    @Override
    public void documentChanged(@NotNull DocumentEvent e) {
      if (Registry.is("editor.new.mouse.hover.popups")) return;
      Editor editor = getEditor();
      if (editor == null || editor.getDocument() == e.getDocument()) {
        closeQuickDocIfPossible();
      }
    }
  }
}
