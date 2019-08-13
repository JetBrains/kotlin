// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.EventDispatcher;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

public class EditorTracker implements ProjectComponent {
  private static final Logger LOG = Logger.getInstance(EditorTracker.class);

  private final Project myProject;
  private final WindowManager myWindowManager;
  private final EditorFactory myEditorFactory;

  private final Map<Window, List<Editor>> myWindowToEditorsMap = new HashMap<>();
  private final Map<Window, WindowAdapter> myWindowToWindowFocusListenerMap = new HashMap<>();
  private final Map<Editor, Window> myEditorToWindowMap = new HashMap<>();
  private List<Editor> myActiveEditors = Collections.emptyList(); // accessed in EDT only

  private final EventDispatcher<EditorTrackerListener> myDispatcher = EventDispatcher.create(EditorTrackerListener.class);

  private IdeFrameImpl myIdeFrame;
  private Window myActiveWindow;

  public EditorTracker(Project project, WindowManager windowManager, EditorFactory editorFactory) {
    myProject = project;
    myWindowManager = windowManager;
    myEditorFactory = editorFactory;
  }

  @Override
  public void projectOpened() {
    myIdeFrame = ((WindowManagerEx)myWindowManager).getFrame(myProject);
    myProject.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        if (myIdeFrame == null || myIdeFrame.getFocusOwner() == null) return;
        setActiveWindow(myIdeFrame);
      }
    });

    final MyEditorFactoryListener myEditorFactoryListener = new MyEditorFactoryListener();
    myEditorFactory.addEditorFactoryListener(myEditorFactoryListener, myProject);
    Disposer.register(myProject, () -> myEditorFactoryListener.executeOnRelease(null));
  }

  private void editorFocused(Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Window window = myEditorToWindowMap.get(editor);
    if (window == null) return;

    List<Editor> list = myWindowToEditorsMap.get(window);
    int index = list.indexOf(editor);
    LOG.assertTrue(index >= 0);
    if (list.isEmpty()) return;

    for (int i = index - 1; i >= 0; i--) {
      list.set(i + 1, list.get(i));
    }
    list.set(0, editor);

    setActiveWindow(window);
  }

  private void registerEditor(Editor editor) {
    unregisterEditor(editor);

    final Window window = windowByEditor(editor);
    if (window == null) return;

    myEditorToWindowMap.put(editor, window);
    List<Editor> list = myWindowToEditorsMap.get(window);
    if (list == null) {
      list = new ArrayList<>();
      myWindowToEditorsMap.put(window, list);

      if (!(window instanceof IdeFrameImpl)) {
        WindowAdapter listener =  new WindowAdapter() {
          @Override
          public void windowGainedFocus(WindowEvent e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("windowGainedFocus:" + window);
            }

            setActiveWindow(window);
          }

          @Override
          public void windowLostFocus(WindowEvent e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("windowLostFocus:" + window);
            }

            setActiveWindow(null);
          }

          @Override
          public void windowClosed(WindowEvent event) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("windowClosed:" + window);
            }

            setActiveWindow(null);
          }
        };
        myWindowToWindowFocusListenerMap.put(window, listener);
        window.addWindowFocusListener(listener);
        window.addWindowListener(listener);
        if (window.isFocused()) {  // windowGainedFocus is missed; activate by force
          setActiveWindow(window);
        }
      }
    }
    list.add(editor);

    if (myActiveWindow == window) {
      setActiveWindow(window); // to fire event
    }
  }

  private void unregisterEditor(Editor editor) {
    Window oldWindow = myEditorToWindowMap.get(editor);
    if (oldWindow != null) {
      myEditorToWindowMap.remove(editor);
      List<Editor> editorsList = myWindowToEditorsMap.get(oldWindow);
      boolean removed = editorsList.remove(editor);
      LOG.assertTrue(removed);

      if (editorsList.isEmpty()) {
        myWindowToEditorsMap.remove(oldWindow);
        final WindowAdapter listener = myWindowToWindowFocusListenerMap.remove(oldWindow);
        if (listener != null) {
          oldWindow.removeWindowFocusListener(listener);
          oldWindow.removeWindowListener(listener);
        }
      }
    }
  }

  private Window windowByEditor(Editor editor) {
    Window window = SwingUtilities.windowForComponent(editor.getComponent());
    if (window instanceof IdeFrameImpl) {
      if (window != myIdeFrame) return null;
    }
    return window;
  }

  @NotNull
  public List<Editor> getActiveEditors() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return myActiveEditors;
  }

  private void setActiveWindow(Window window) {
    myActiveWindow = window;
    List<Editor> editors = editorsByWindow(myActiveWindow);
    setActiveEditors(editors);
  }

  @NotNull
  private List<Editor> editorsByWindow(Window window) {
    List<Editor> list = myWindowToEditorsMap.get(window);
    if (list == null) return Collections.emptyList();
    List<Editor> filtered = new SmartList<>();
    for (Editor editor : list) {
      if (editor.getContentComponent().isShowing()) {
        filtered.add(editor);
      }
    }
    return filtered;
  }

  public void setActiveEditors(@NotNull List<Editor> editors) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myActiveEditors = editors;

    if (LOG.isDebugEnabled()) {
      LOG.debug("active editors changed:");
      for (Editor editor : editors) {
        PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
        LOG.debug("    " + psiFile);
      }
    }

    myDispatcher.getMulticaster().activeEditorsChanged(editors);
  }

  void addEditorTrackerListener(@NotNull Disposable parentDisposable, @NotNull EditorTrackerListener listener) {
    myDispatcher.addListener(listener,parentDisposable);
  }

  private class MyEditorFactoryListener implements EditorFactoryListener {
    private final Map<Editor, Runnable> myExecuteOnEditorRelease = new HashMap<>();

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
      final Editor editor = event.getEditor();
      if (editor.getProject() != null && editor.getProject() != myProject || myProject.isDisposed()) return;
      final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
      if (psiFile == null) return;

      final JComponent component = editor.getComponent();
      final JComponent contentComponent = editor.getContentComponent();

      final HierarchyListener hierarchyListener = __ -> registerEditor(editor);
      component.addHierarchyListener(hierarchyListener);

      final FocusListener focusListener = new FocusListener() {
        @Override
        public void focusGained(@NotNull FocusEvent e) {
          editorFocused(editor);
        }

        @Override
        public void focusLost(@NotNull FocusEvent e) {
        }
      };
      contentComponent.addFocusListener(focusListener);

      myExecuteOnEditorRelease.put(event.getEditor(), () -> {
        component.removeHierarchyListener(hierarchyListener);
        contentComponent.removeFocusListener(focusListener);
      });
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
      final Editor editor = event.getEditor();
      if (editor.getProject() != null && editor.getProject() != myProject) return;
      unregisterEditor(editor);
      executeOnRelease(editor);
    }

    private void executeOnRelease(Editor editor) {
      if (editor == null) {
        for (Runnable r : myExecuteOnEditorRelease.values()) {
          r.run();
        }
        myExecuteOnEditorRelease.clear();
      }
      else {
        final Runnable runnable = myExecuteOnEditorRelease.get(editor);
        if (runnable != null) {
          runnable.run();
          myExecuteOnEditorRelease.remove(editor);
        }
      }
    }
  }
}
