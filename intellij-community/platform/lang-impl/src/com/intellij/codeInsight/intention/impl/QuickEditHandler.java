// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl;

import com.intellij.injected.editor.InjectedFileChangesHandler;
import com.intellij.injected.editor.InjectedFileChangesHandlerProvider;
import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.ReadonlyFragmentModificationHandler;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorWithProviderComposite;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.impl.source.tree.injected.Place;
import com.intellij.psi.impl.source.tree.injected.changesHandler.CommonInjectedFileChangesHandler;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import javax.swing.FocusManager;
import javax.swing.*;
import java.awt.*;

/**
* @author Gregory Shrago
*/
public class QuickEditHandler implements Disposable, DocumentListener {
  private final Project myProject;
  private final QuickEditAction myAction;

  private final Editor myEditor;
  private final Document myOrigDocument;

  private final Document myNewDocument;
  private final PsiFile myNewFile;
  private final LightVirtualFile myNewVirtualFile;

  private final long myOrigCreationStamp;
  private EditorWindow mySplittedWindow;
  private boolean myCommittingToOriginal;

  private final InjectedFileChangesHandler myEditChangesHandler;

  public static final Key<String> REPLACEMENT_KEY = Key.create("REPLACEMENT_KEY");

  QuickEditHandler(Project project, @NotNull PsiFile injectedFile, final PsiFile origFile, Editor editor, QuickEditAction action) {
    myProject = project;
    myEditor = editor;
    myAction = action;
    myOrigDocument = editor.getDocument();
    Place shreds = InjectedLanguageUtil.getShreds(injectedFile);
    FileType fileType = injectedFile.getFileType();
    Language language = injectedFile.getLanguage();
    PsiLanguageInjectionHost.Shred firstShred = ContainerUtil.getFirstItem(shreds);

    PsiFileFactory factory = PsiFileFactory.getInstance(project);
    String text = InjectedLanguageManager.getInstance(project).getUnescapedText(injectedFile);
    String newFileName =
      StringUtil.notNullize(language.getDisplayName(), "Injected") + " Fragment " + "(" +
      origFile.getName() + ":" + firstShred.getHost().getTextRange().getStartOffset() + ")" + "." + fileType.getDefaultExtension();

    // preserve \r\n as it is done in MultiHostRegistrarImpl
    myNewFile = factory.createFileFromText(newFileName, language, text, true, false);
    myNewVirtualFile = ObjectUtils.assertNotNull((LightVirtualFile)myNewFile.getVirtualFile());
    myNewVirtualFile.setOriginalFile(origFile.getVirtualFile());

    assert myNewFile != null : "PSI file is null";
    assert myNewFile.getTextLength() == myNewVirtualFile.getContent().length() : "PSI / Virtual file text mismatch";

    myNewVirtualFile.setOriginalFile(origFile.getVirtualFile());
    // suppress possible errors as in injected mode
    myNewFile.putUserData(InjectedLanguageUtil.FRANKENSTEIN_INJECTION,
                          injectedFile.getUserData(InjectedLanguageUtil.FRANKENSTEIN_INJECTION));
    PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(project).getInjectionHost(injectedFile.getViewProvider());
    myNewFile.putUserData(FileContextUtil.INJECTED_IN_ELEMENT, SmartPointerManager.getInstance(project).createSmartPsiElementPointer(host));
    myNewDocument = PsiDocumentManager.getInstance(project).getDocument(myNewFile);
    assert myNewDocument != null;
    EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(myNewDocument, new MyQuietHandler());
    myOrigCreationStamp = myOrigDocument.getModificationStamp(); // store creation stamp for UNDO tracking
    myOrigDocument.addDocumentListener(this, this);
    myNewDocument.addDocumentListener(this, this);
    EditorFactory editorFactory = ObjectUtils.assertNotNull(EditorFactory.getInstance());
    // not FileEditorManager listener because of RegExp checker and alike
    editorFactory.addEditorFactoryListener(new EditorFactoryListener() {
      int useCount;

      @Override
      public void editorCreated(@NotNull EditorFactoryEvent event) {
        if (event.getEditor().getDocument() != myNewDocument) return;
        useCount++;
      }

      @Override
      public void editorReleased(@NotNull EditorFactoryEvent event) {
        if (event.getEditor().getDocument() == myOrigDocument) {
          ApplicationManager.getApplication().invokeLater(() -> closeEditor(), myProject.getDisposed());
          return;
        }
        if (event.getEditor().getDocument() != myNewDocument) return;
        if (--useCount > 0) return;
        if (Boolean.TRUE.equals(myNewVirtualFile.getUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN))) return;

        Disposer.dispose(QuickEditHandler.this);
      }
    }, this);


    InjectedFileChangesHandlerProvider changesHandlerFactory =
      InjectedFileChangesHandlerProvider.EP.forLanguage(firstShred.getHost().getLanguage());
    if (changesHandlerFactory != null) {
      myEditChangesHandler = changesHandlerFactory.createFileChangesHandler(shreds, editor, myNewDocument, injectedFile);
    }
    else {
      myEditChangesHandler = new CommonInjectedFileChangesHandler(shreds, editor, myNewDocument, injectedFile);
    }
    Disposer.register(this, myEditChangesHandler);
    initGuardedBlocks(shreds);
  }

  public boolean isValid() {
    return myNewVirtualFile.isValid() && myEditChangesHandler.isValid();
  }

  public void navigate(int injectedOffset) {
    if (myAction.isShowInBalloon()) {
      final JComponent component = myAction.createBalloonComponent(myNewFile);
      if (component != null) showBalloon(myEditor, myNewFile, component);
    }
    else {
      final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(myProject);
      final FileEditor[] editors = fileEditorManager.getEditors(myNewVirtualFile);
      if (editors.length == 0) {
        final EditorWindow curWindow = fileEditorManager.getCurrentWindow();
        mySplittedWindow = curWindow.split(SwingConstants.HORIZONTAL, false, myNewVirtualFile, true);
      }
      Editor editor = fileEditorManager.openTextEditor(new OpenFileDescriptor(myProject, myNewVirtualFile, injectedOffset), true);
      // fold missing values
      if (editor instanceof EditorEx) {
        editor.putUserData(QuickEditAction.QUICK_EDIT_HANDLER, this);
        final FoldingModelEx foldingModel = ((EditorEx)editor).getFoldingModel();
        foldingModel.runBatchFoldingOperation(() -> {
          CharSequence sequence = myNewDocument.getImmutableCharSequence();
          for (RangeMarker o : ContainerUtil.reverse(((DocumentEx)myNewDocument).getGuardedBlocks())) {
            String replacement = o.getUserData(REPLACEMENT_KEY);
            if (StringUtil.isEmpty(replacement)) continue;
            int start = o.getStartOffset();
            int end = o.getEndOffset();
            start += StringUtil.countChars(sequence, '\n', start, end, true);
            end -= StringUtil.countChars(sequence, '\n', end, start, true);
            if (start <= end) {
              FoldRegion region = foldingModel.getFoldRegion(start, end);
              if (region == null) {
                region = foldingModel.createFoldRegion(start, end, replacement, null, true);
              }
              if (region != null) region.setExpanded(false);
            }
          }
        });
      }
    }
    ApplicationManager.getApplication().invokeLater(
      () -> myEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER), ModalityState.any());
  }

  public static void showBalloon(Editor editor, PsiFile newFile, JComponent component) {
    final Balloon balloon = JBPopupFactory.getInstance().createBalloonBuilder(component)
      .setShadow(true)
      .setAnimationCycle(0)
      .setHideOnClickOutside(true)
      .setHideOnKeyOutside(true)
      .setHideOnAction(false)
      .setFillColor(UIUtil.getControlColor())
      .createBalloon();
    DumbAwareAction.create(e -> balloon.hide())
      .registerCustomShortcutSet(CommonShortcuts.ESCAPE, component);
    Disposer.register(newFile.getProject(), balloon);
    final Balloon.Position position = QuickEditAction.getBalloonPosition(editor);
    RelativePoint point = JBPopupFactory.getInstance().guessBestPopupLocation(editor);
    if (position == Balloon.Position.above) {
      final Point p = point.getPoint();
      point = new RelativePoint(point.getComponent(), new Point(p.x, p.y - editor.getLineHeight()));
    }
    balloon.show(point, position);
  }

  @Override
  public void documentChanged(@NotNull DocumentEvent e) {
    if (UndoManager.getInstance(myProject).isUndoOrRedoInProgress()) {
      // allow undo/redo up until 'creation stamp' back in time
      // and check it after action is completed
      if (e.getDocument() == myOrigDocument) {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (myOrigCreationStamp > myOrigDocument.getModificationStamp()) {
            closeEditor();
          }
        }, myProject.getDisposed());
      }
    }
    else if (e.getDocument() == myNewDocument) {
      commitToOriginal(e);
      if (!isValid()) {
        ApplicationManager.getApplication().invokeLater(() -> closeEditor(), myProject.getDisposed());
      }
    }
    else if (e.getDocument() == myOrigDocument) {
      if (myCommittingToOriginal) return;
      if (!myEditChangesHandler.handlesRange(TextRange.from(e.getOffset(), e.getOldLength()))) return;
      ApplicationManager.getApplication().invokeLater(() -> {
        Component owner = FocusManager.getCurrentManager().getFocusOwner();
        closeEditor();
        if (owner != null) owner.requestFocus();
      }, myProject.getDisposed());
    }
  }

  private void closeEditor() {
    boolean unsplit = false;
    if (mySplittedWindow != null && !mySplittedWindow.isDisposed()) {
      final EditorWithProviderComposite[] editors = mySplittedWindow.getEditors();
      if (editors.length == 1 && Comparing.equal(editors[0].getFile(), myNewVirtualFile)) {
        unsplit = true;
      }
    }
    if (unsplit) {
      ((FileEditorManagerImpl)FileEditorManager.getInstance(myProject)).closeFile(myNewVirtualFile, mySplittedWindow, false);
    }
    FileEditorManager.getInstance(myProject).closeFile(myNewVirtualFile);
  }


  private void initGuardedBlocks(Place shreds) {
    int origOffset = -1;
    int curOffset = 0;
    for (PsiLanguageInjectionHost.Shred shred : shreds) {
      Segment hostRangeMarker = shred.getHostRangeMarker();
      int start = shred.getRange().getStartOffset() + shred.getPrefix().length();
      int end = shred.getRange().getEndOffset() - shred.getSuffix().length();
      if (curOffset < start) {
        RangeMarker guard = myNewDocument.createGuardedBlock(curOffset, start);
        if (curOffset == 0 && shred == shreds.get(0)) guard.setGreedyToLeft(true);
        String padding = origOffset < 0 ? "" : myOrigDocument.getText().substring(origOffset, hostRangeMarker.getStartOffset());
        guard.putUserData(REPLACEMENT_KEY, fixQuotes(padding));
      }
      curOffset = end;
      origOffset = hostRangeMarker.getEndOffset();
    }
    if (curOffset < myNewDocument.getTextLength()) {
      RangeMarker guard = myNewDocument.createGuardedBlock(curOffset, myNewDocument.getTextLength());
      guard.setGreedyToRight(true);
      guard.putUserData(REPLACEMENT_KEY, "");
    }
  }


  private void commitToOriginal(final DocumentEvent e) {
    myCommittingToOriginal = true;
    try {
      PostprocessReformattingAspect.getInstance(myProject).disablePostprocessFormattingInside(() -> myEditChangesHandler.commitToOriginal(e));
      PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(myOrigDocument);
    }
    finally {
      myCommittingToOriginal = false;
    }
  }

  private static String fixQuotes(String padding) {
    if (padding.isEmpty()) return padding;
    if (padding.startsWith("'")) padding = '\"' + padding.substring(1);
    if (padding.endsWith("'")) padding = padding.substring(0, padding.length() - 1) + "\"";
    return padding;
  }

  @Override
  public void dispose() {
    // noop
  }

  @TestOnly
  public PsiFile getNewFile() {
    return myNewFile;
  }

  public boolean tryReuse(@NotNull PsiFile injectedFile, @NotNull TextRange hostRange) {
    return myEditChangesHandler.tryReuse(injectedFile, hostRange);
  }


  private static class MyQuietHandler implements ReadonlyFragmentModificationHandler {
    @Override
    public void handle(final ReadOnlyFragmentModificationException e) {
      //nothing
    }
  }
}
