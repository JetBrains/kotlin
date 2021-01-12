// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl.libraries;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.TitledHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.BasicUndoableAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.RenameHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public class RenameLibraryHandler implements RenameHandler, TitledHandler {
  private static final Logger LOG = Logger.getInstance(RenameLibraryHandler.class);

  @Override
  public boolean isAvailableOnDataContext(@NotNull DataContext dataContext) {
    Library library = LangDataKeys.LIBRARY.getData(dataContext);
    return library != null;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    LOG.assertTrue(false);
  }

  @Override
  public void invoke(@NotNull final Project project, PsiElement @NotNull [] elements, @NotNull DataContext dataContext) {
    final Library library = LangDataKeys.LIBRARY.getData(dataContext);
    LOG.assertTrue(library != null);
    Messages.showInputDialog(project,
                             IdeBundle.message("prompt.enter.new.library.name"),
                             IdeBundle.message("title.rename.library"),
                             Messages.getQuestionIcon(),
                             library.getName(),
                             new MyInputValidator(project, library));
  }

  @Override
  public String getActionTitle() {
    return IdeBundle.message("title.rename.library");
  }

  private static class MyInputValidator implements InputValidator {
    @NotNull
    private final Project myProject;
    @NotNull
    private final Library myLibrary;
    MyInputValidator(@NotNull Project project, @NotNull Library library) {
      myProject = project;
      myLibrary = library;
    }

    @Override
    public boolean checkInput(String inputString) {
      return inputString != null && !inputString.isEmpty() && myLibrary.getTable().getLibraryByName(inputString) == null;
    }

    @Override
    public boolean canClose(final String inputString) {
      final String oldName = myLibrary.getName();
      final Library.ModifiableModel modifiableModel = renameLibrary(inputString);
      final Ref<Boolean> success = Ref.create(Boolean.TRUE);
      CommandProcessor.getInstance().executeCommand(myProject, () -> {
        UndoableAction action = new BasicUndoableAction() {
          @Override
          public void undo() {
            final Library.ModifiableModel modifiableModel1 = renameLibrary(oldName);
            modifiableModel1.commit();
          }

          @Override
          public void redo() {
            final Library.ModifiableModel modifiableModel1 = renameLibrary(inputString);
            modifiableModel1.commit();
          }
        };
        UndoManager.getInstance(myProject).undoableActionPerformed(action);
        ApplicationManager.getApplication().runWriteAction(() -> modifiableModel.commit());
      }, IdeBundle.message("command.renaming.module", oldName), null);
      return success.get().booleanValue();
    }

    @NotNull
    private Library.ModifiableModel renameLibrary(String inputString) {
      final Library.ModifiableModel modifiableModel = myLibrary.getModifiableModel();
      modifiableModel.setName(inputString);
      return modifiableModel;
    }
  }

}
