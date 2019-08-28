// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.dnd.FileCopyPasteUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.util.List;

import static com.intellij.ide.actions.CopyReferenceUtil.*;

class CopyReferencePopupAction extends DumbAwareAction {
  public static final DataFlavor ourFlavor = FileCopyPasteUtil.createJvmDataFlavor(CopyReferenceFQNTransferable.class);

  @Override
  public void update(@NotNull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    List<PsiElement> elements = getElementsToCopy(editor, dataContext);

    boolean enabled = !elements.isEmpty();
    Presentation presentation = e.getPresentation();
    presentation.setEnabled(enabled);
    presentation.setVisible(!ActionPlaces.isPopupPlace(e.getPlace()) || enabled);
    presentation.setText(elements.size() > 1 ? "References" : "Reference");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    List<PsiElement> elements = getElementsToCopy(editor, dataContext);

    if (elements.isEmpty()) {
      return;
    }

    String copy = doElementsCopy(elements, editor);
    if (copy != null) {
      CopyPasteManager.getInstance().setContents(new CopyReferenceFQNTransferable(copy));
      setStatusBarText(project, IdeBundle.message("message.reference.to.fqn.has.been.copied", copy));
      highlight(editor, project, elements);
    }
  }

  public static String doElementsCopy(@NotNull List<? extends PsiElement> elements, @Nullable Editor editor) {
    if (elements.isEmpty()) return null;

    return StreamEx.of(elements).map(element -> elementToFqn(element, editor)).filter(fqn -> fqn != null).joining("\n");
  }
}