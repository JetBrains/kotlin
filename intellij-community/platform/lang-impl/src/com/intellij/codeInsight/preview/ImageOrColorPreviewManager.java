// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.preview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

final class ImageOrColorPreviewManager implements Disposable, EditorMouseMotionListener, EditorFactoryListener {
  private static final Logger LOG = Logger.getInstance(ImageOrColorPreviewManager.class);

  private static final Key<KeyListener> EDITOR_LISTENER_ADDED = Key.create("previewManagerListenerAdded");

  private final Alarm alarm = new Alarm();

  /**
   * this collection should not keep strong references to the elements
   * @link getPsiElementsAt()
   */
  @Nullable
  private Collection<PsiElement> myElements;

  @Override
  public void editorCreated(@NotNull EditorFactoryEvent event) {
    registerListeners(event.getEditor());
  }

  @Override
  public void editorReleased(@NotNull EditorFactoryEvent event) {
    Editor editor = event.getEditor();
    if (editor.isOneLineMode()) {
      return;
    }

    KeyListener keyListener = EDITOR_LISTENER_ADDED.get(editor);
    if (keyListener != null) {
      EDITOR_LISTENER_ADDED.set(editor, null);
      editor.getContentComponent().removeKeyListener(keyListener);
      editor.removeEditorMouseMotionListener(this);
    }
  }

  private void registerListeners(final Editor editor) {
    if (editor.isOneLineMode()) {
      return;
    }

    Project project = editor.getProject();
    if (project == null || project.isDisposed()) {
      return;
    }

    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (psiFile == null || psiFile instanceof PsiCompiledElement || !isSupportedFile(psiFile)) {
      return;
    }

    editor.addEditorMouseMotionListener(this);

    KeyListener keyListener = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT && !editor.isOneLineMode()) {
          PointerInfo pointerInfo = MouseInfo.getPointerInfo();
          if (pointerInfo != null) {
            Point location = pointerInfo.getLocation();
            SwingUtilities.convertPointFromScreen(location, editor.getContentComponent());
            alarm.cancelAllRequests();
            alarm.addRequest(new PreviewRequest(location, editor, true), 100);
          }
        }
      }
    };
    editor.getContentComponent().addKeyListener(keyListener);

    EDITOR_LISTENER_ADDED.set(editor, keyListener);
  }

  private static boolean isSupportedFile(PsiFile psiFile) {
    for (PsiFile file : psiFile.getViewProvider().getAllFiles()) {
      for (ElementPreviewProvider provider : ElementPreviewProvider.EP_NAME.getExtensionList()) {
        if (provider.isSupportedFile(file)) {
          return true;
        }
      }
    }
    return false;
  }

  @NotNull
  private static Collection<PsiElement> getPsiElementsAt(Point point, Editor editor) {
    if (editor.isDisposed()) {
      return Collections.emptySet();
    }

    Project project = editor.getProject();
    if (project == null || project.isDisposed()) {
      return Collections.emptySet();
    }

    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    final Document document = editor.getDocument();
    PsiFile psiFile = documentManager.getPsiFile(document);
    if (psiFile == null || psiFile instanceof PsiCompiledElement || !psiFile.isValid()) {
      return Collections.emptySet();
    }

    final Set<PsiElement> elements = ContainerUtil.createWeakSet();
    final int offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(point));
    if (documentManager.isCommitted(document)) {
      ContainerUtil.addIfNotNull(elements, InjectedLanguageUtil.findElementAtNoCommit(psiFile, offset));
    }
    for (PsiFile file : psiFile.getViewProvider().getAllFiles()) {
      ContainerUtil.addIfNotNull(elements, file.findElementAt(offset));
    }

    return elements;
  }

  @Override
  public void dispose() {
    alarm.cancelAllRequests();
    myElements = null;
  }

  @Override
  public void mouseMoved(@NotNull EditorMouseEvent event) {
    Editor editor = event.getEditor();
    if (editor.isOneLineMode()) {
      return;
    }

    alarm.cancelAllRequests();
    Point point = event.getMouseEvent().getPoint();
    Collection<PsiElement> elements = myElements;
    if (elements == null && event.getMouseEvent().isShiftDown()) {
      alarm.addRequest(new PreviewRequest(point, editor, false), 100);
    }
    else if (elements != null && !getPsiElementsAt(point, editor).equals(elements)) {
      myElements = null;
      for (ElementPreviewProvider provider : ElementPreviewProvider.EP_NAME.getExtensionList()) {
        try {
          for (PsiElement element : elements) {
            provider.hide(element, editor);
          }
        }
        catch (Exception e) {
          LOG.error(e);
        }
      }
    }
  }

  private final class PreviewRequest implements Runnable {
    private final Point point;
    private final Editor editor;
    private final boolean keyTriggered;

    PreviewRequest(Point point, Editor editor, boolean keyTriggered) {
      this.point = point;
      this.editor = editor;
      this.keyTriggered = keyTriggered;
    }

    @Override
    public void run() {
      Collection<PsiElement> elements = getPsiElementsAt(point, editor);
      if (elements.equals(myElements)) return;
      for (PsiElement element : elements) {
        if (element == null || !element.isValid()) {
          return;
        }
        if (PsiDocumentManager.getInstance(element.getProject()).isUncommited(editor.getDocument()) ||
            DumbService.getInstance(element.getProject()).isDumb()) {
          return;
        }

        for (ElementPreviewProvider provider : ElementPreviewProvider.EP_NAME.getExtensions()) {
          if (!provider.isSupportedFile(element.getContainingFile())) continue;

          try {
            provider.show(element, editor, point, keyTriggered);
          }
          catch (ProcessCanceledException e) {
            throw e;
          }
          catch (Exception e) {
            LOG.error(e);
          }
        }
      }
      myElements = elements;
    }
  }
}