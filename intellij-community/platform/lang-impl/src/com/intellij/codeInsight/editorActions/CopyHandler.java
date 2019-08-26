// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actions.CopyAction;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

public class CopyHandler extends EditorActionHandler {
  private static final Logger LOG = Logger.getInstance(CopyHandler.class);

  private final EditorActionHandler myOriginalAction;

  public CopyHandler(final EditorActionHandler originalHandler) {
    myOriginalAction = originalHandler;
  }

  @Override
  public void doExecute(@NotNull final Editor editor, Caret caret, final DataContext dataContext) {
    assert caret == null : "Invocation of 'copy' operation for specific caret is not supported";
    final Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(editor.getComponent()));
    if (project == null){
      if (myOriginalAction != null){
        myOriginalAction.execute(editor, null, dataContext);
      }
      return;
    }
    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) {
      if (myOriginalAction != null) {
        myOriginalAction.execute(editor, null, dataContext);
      }
      return;
    }

    final SelectionModel selectionModel = editor.getSelectionModel();
    if (!selectionModel.hasSelection(true)) {
      if (Registry.is(CopyAction.SKIP_COPY_AND_CUT_FOR_EMPTY_SELECTION_KEY)) {
        return;
      }
      editor.getCaretModel().runForEachCaret(__ -> selectionModel.selectLineAtCaret());
      if (!selectionModel.hasSelection(true)) return;
      editor.getCaretModel().runForEachCaret(__ -> EditorActionUtil.moveCaretToLineStartIgnoringSoftWraps(editor));
    }

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final int[] startOffsets = selectionModel.getBlockSelectionStarts();
    final int[] endOffsets = selectionModel.getBlockSelectionEnds();

    final List<TextBlockTransferableData> transferableDatas = new ArrayList<>();

    DumbService.getInstance(project).withAlternativeResolveEnabled(() -> {
      for (CopyPastePostProcessor<? extends TextBlockTransferableData> processor : CopyPastePostProcessor.EP_NAME.getExtensionList()) {
        try {
          transferableDatas.addAll(processor.collectTransferableData(file, editor, startOffsets, endOffsets));
        }
        catch (IndexNotReadyException e) {
          LOG.debug(e);
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    });

    String text = editor.getCaretModel().supportsMultipleCarets()
                  ? EditorCopyPasteHelperImpl.getSelectedTextForClipboard(editor, transferableDatas)
                  : selectionModel.getSelectedText();
    String rawText = TextBlockTransferable.convertLineSeparators(text, "\n", transferableDatas);
    String escapedText = null;
    for (CopyPastePreProcessor processor : CopyPastePreProcessor.EP_NAME.getExtensionList()) {
      try {
        escapedText = processor.preprocessOnCopy(file, startOffsets, endOffsets, rawText);
      }
      catch (Throwable e) {
        LOG.error(e);
      }
      if (escapedText != null) {
        break;
      }
    }
    final Transferable transferable = new TextBlockTransferable(escapedText != null ? escapedText : rawText,
                                                                transferableDatas,
                                                                escapedText != null ? new RawText(rawText) : null);
    CopyPasteManager.getInstance().setContents(transferable);
    if (editor instanceof EditorEx) {
      EditorEx ex = (EditorEx)editor;
      if (ex.isStickySelection()) {
        ex.setStickySelection(false);
      }
    }
  }
}
