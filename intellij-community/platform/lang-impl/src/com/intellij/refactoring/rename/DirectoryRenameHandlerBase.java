// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename;

import com.intellij.ide.TitledHandler;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.RefactoringBundle;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public abstract class DirectoryRenameHandlerBase implements RenameHandler, TitledHandler {
  private static final Logger LOG = Logger.getInstance(DirectoryRenameHandlerBase.class);

  @Override
  public String getActionTitle() {
    return RefactoringBundle.message("rename.directory.title");
  }

  @Override
  public boolean isAvailableOnDataContext(@NotNull final DataContext dataContext) {
    PsiDirectory directory = adjustForRename(dataContext, PsiElementRenameHandler.getElement(dataContext));
    if (directory != null) {
      final VirtualFile virtualFile = directory.getVirtualFile();
      final Project project = directory.getProject();
      if (Comparing.equal(project.getBaseDir(), virtualFile)) return false;
      if (ProjectRootManager.getInstance(project).getFileIndex().isInContent(virtualFile)) {
        return true;
      }
    }
    return false;
  }

  protected PsiDirectory adjustForRename(DataContext dataContext, PsiElement element) {
    if (element instanceof PsiDirectoryContainer) {
      final Module module = LangDataKeys.MODULE.getData(dataContext);
      if (module != null) {
        PsiDirectory[] directories = ((PsiDirectoryContainer)element).getDirectories(GlobalSearchScope.moduleScope(module));
        Optional<PsiDirectory> directoryWithPackage = Arrays.stream(directories).filter(this::isSuitableDirectory).findFirst();
        return directoryWithPackage.orElse(null);
      }
    }
    return element instanceof PsiDirectory && isSuitableDirectory((PsiDirectory)element) ? (PsiDirectory)element : null;
  }

  protected abstract boolean isSuitableDirectory(PsiDirectory directory);

  @Override
  public boolean isRenaming(@NotNull final DataContext dataContext) {
    return isAvailableOnDataContext(dataContext);
  }

  @Override
  public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file, final DataContext dataContext) {
    PsiElement element = adjustForRename(dataContext, PsiElementRenameHandler.getElement(dataContext));
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    final PsiElement nameSuggestionContext = file.findElementAt(editor.getCaretModel().getOffset());
    doRename(element, project, nameSuggestionContext, editor);
  }

  @Override
  public void invoke(@NotNull final Project project, @NotNull final PsiElement[] elements, final DataContext dataContext) {
    PsiElement element = elements.length == 1 ? elements[0] : null;
    if (element == null) element = PsiElementRenameHandler.getElement(dataContext);
    final PsiElement nameSuggestionContext = element;
    element = adjustForRename(dataContext, element);
    LOG.assertTrue(element != null);
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    doRename(element, project, nameSuggestionContext, editor);
  }

  protected abstract void doRename(PsiElement element, Project project, PsiElement nameSuggestionContext, Editor editor);
}
