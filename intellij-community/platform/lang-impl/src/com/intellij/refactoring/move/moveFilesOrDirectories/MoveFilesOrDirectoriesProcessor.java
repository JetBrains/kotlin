/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.refactoring.move.moveFilesOrDirectories;

import com.intellij.ide.util.EditorHelper;
import com.intellij.lang.FileASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.paths.PsiDynaReference;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringEventData;
import com.intellij.refactoring.move.FileReferenceContextUtil;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.NonCodeUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class MoveFilesOrDirectoriesProcessor extends BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance(MoveFilesOrDirectoriesProcessor.class);

  protected final PsiElement[] myElementsToMove;
  protected final boolean mySearchForReferences;
  protected final boolean mySearchInComments;
  protected final boolean mySearchInNonJavaFiles;
  private final PsiDirectory myNewParent;
  private final MoveCallback myMoveCallback;
  private NonCodeUsageInfo[] myNonCodeUsages;
  private final Map<PsiFile, List<UsageInfo>> myFoundUsages = new HashMap<>();

  public MoveFilesOrDirectoriesProcessor(Project project,
                                         PsiElement[] elements,
                                         PsiDirectory newParent,
                                         boolean searchInComments,
                                         boolean searchInNonJavaFiles,
                                         MoveCallback moveCallback,
                                         Runnable prepareSuccessfulCallback) {
    this(project, elements, newParent, true, searchInComments, searchInNonJavaFiles, moveCallback, prepareSuccessfulCallback);
  }

  public MoveFilesOrDirectoriesProcessor(Project project,
                                         PsiElement[] elements,
                                         PsiDirectory newParent,
                                         boolean searchForReferences,
                                         boolean searchInComments,
                                         boolean searchInNonJavaFiles,
                                         MoveCallback moveCallback,
                                         Runnable prepareSuccessfulCallback) {
    super(project, prepareSuccessfulCallback);
    myElementsToMove = elements;
    myNewParent = newParent;
    mySearchForReferences = searchForReferences;
    mySearchInComments = searchInComments;
    mySearchInNonJavaFiles = searchInNonJavaFiles;
    myMoveCallback = moveCallback;
  }

  @Override
  @NotNull
  protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
    return new MoveFilesOrDirectoriesViewDescriptor(myElementsToMove, myNewParent);
  }

  @Override
  @NotNull
  protected UsageInfo[] findUsages() {
    ArrayList<UsageInfo> result = new ArrayList<>();
    for (int i = 0; i < myElementsToMove.length; i++) {
      PsiElement element = myElementsToMove[i];
      if (mySearchForReferences) {
        for (PsiReference reference : ReferencesSearch.search(element, GlobalSearchScope.projectScope(myProject))) {
          result.add(new MyUsageInfo(reference.getElement(), i, reference));
        }
      }
      findElementUsages(result, element);
    }

    return result.toArray(UsageInfo.EMPTY_ARRAY);
  }

  private void findElementUsages(ArrayList<? super UsageInfo> result, PsiElement element) {
    if (!mySearchForReferences) {
      return;
    }
    if (element instanceof PsiFile) {
      final List<UsageInfo> usages = MoveFileHandler.forElement((PsiFile)element)
        .findUsages(((PsiFile)element), myNewParent, mySearchInComments, mySearchInNonJavaFiles);
      if (usages != null) {
        result.addAll(usages);
        myFoundUsages.put((PsiFile)element, usages);
      }
    }
    else if (element instanceof PsiDirectory) {
      for (PsiElement childElement : element.getChildren()) {
        findElementUsages(result, childElement);
      }
    }
  }

  @Override
  protected void refreshElements(@NotNull PsiElement[] elements) {
    LOG.assertTrue(elements.length == myElementsToMove.length);
    System.arraycopy(elements, 0, myElementsToMove, 0, elements.length);
  }

  @Override
  protected void performPsiSpoilingRefactoring() {
    if (myNonCodeUsages != null) {
      RenameUtil.renameNonCodeUsages(myProject, myNonCodeUsages);
    }
  }

  @Override
  protected void performRefactoring(@NotNull UsageInfo[] usages) {
    // If files are being moved then I need to collect some information to delete these
    // files from CVS. I need to know all common parents of the moved files and relative
    // paths.

    // Move files with correction of references.

    try {

      Map<PsiFile, FileASTNode> movedFiles = new LinkedHashMap<>();
      final Map<PsiElement, PsiElement> oldToNewMap = new HashMap<>();
      for (final PsiElement element : myElementsToMove) {
        final RefactoringElementListener elementListener = getTransaction().getElementListener(element);

        if (element instanceof PsiDirectory) {
          if (mySearchForReferences) encodeDirectoryFiles(element, movedFiles);
          MoveFilesOrDirectoriesUtil.doMoveDirectory((PsiDirectory)element, myNewParent);
          for (PsiElement psiElement : element.getChildren()) {
            processDirectoryFiles(movedFiles, oldToNewMap, psiElement);
          }
        }
        else if (element instanceof PsiFile) {
          final PsiFile movedFile = (PsiFile)element;
          FileASTNode node = movedFile.getNode();
          if (mySearchForReferences) FileReferenceContextUtil.encodeFileReferences(element);
          MoveFileHandler.forElement(movedFile).prepareMovedFile(movedFile, myNewParent, oldToNewMap);

          PsiFile moving = myNewParent.findFile(movedFile.getName());
          if (moving == null) {
            MoveFilesOrDirectoriesUtil.doMoveFile(movedFile, myNewParent);
          }
          moving = myNewParent.findFile(movedFile.getName());
          if (moving != null) {
            movedFiles.put(moving, moving.getNode());
            ObjectUtils.reachabilityFence(node);
          }
        }

        elementListener.elementMoved(element);
      }
      // sort by offset descending to process correctly several usages in one PsiElement [IDEADEV-33013]
      CommonRefactoringUtil.sortDepthFirstRightLeftOrder(usages);

      DumbService.getInstance(myProject).completeJustSubmittedTasks();

      // fix references in moved files to outer files
      for (PsiFile movedFile : movedFiles.keySet()) {
        MoveFileHandler.forElement(movedFile).updateMovedFile(movedFile);
        if (mySearchForReferences) FileReferenceContextUtil.decodeFileReferences(movedFile);
      }

      retargetUsages(usages, oldToNewMap);

      // Perform CVS "add", "remove" commands on moved files.

      if (myMoveCallback != null) {
        myMoveCallback.refactoringCompleted();
      }
      if (MoveFilesOrDirectoriesDialog.isOpenInEditor()) {
        List<PsiFile> justFiles = new ArrayList<>(movedFiles.keySet());
        ApplicationManager.getApplication().invokeLater(() ->
          EditorHelper.openFilesInEditor(justFiles.stream().filter(PsiElement::isValid).toArray(PsiFile[]::new))
        );
      }

    }
    catch (IncorrectOperationException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        LOG.info(e);
        ApplicationManager.getApplication().invokeLater(
          () -> Messages.showMessageDialog(myProject, cause.getMessage(), RefactoringBundle.message("error.title"), Messages.getErrorIcon()));
      }
      else {
        LOG.error(e);
      }
    }
  }

  @Nullable
  @Override
  protected String getRefactoringId() {
    return "refactoring.move";
  }

  @Nullable
  @Override
  protected RefactoringEventData getBeforeData() {
    RefactoringEventData data = new RefactoringEventData();
    data.addElements(myElementsToMove);
    return data;
  }

  @Nullable
  @Override
  protected RefactoringEventData getAfterData(@NotNull UsageInfo[] usages) {
    RefactoringEventData data = new RefactoringEventData();
    data.addElement(myNewParent);
    return data;
  }

  private static void encodeDirectoryFiles(PsiElement psiElement, Map<PsiFile, FileASTNode> movedFiles) {
    if (psiElement instanceof PsiFile) {
      movedFiles.put((PsiFile)psiElement, ((PsiFile)psiElement).getNode());
      FileReferenceContextUtil.encodeFileReferences(psiElement);
    }
    else if (psiElement instanceof PsiDirectory) {
      for (PsiElement element : psiElement.getChildren()) {
        encodeDirectoryFiles(element, movedFiles);
      }
    }
  }

  private static void processDirectoryFiles(Map<PsiFile, FileASTNode> movedFiles, Map<PsiElement, PsiElement> oldToNewMap, PsiElement psiElement) {
    if (psiElement instanceof PsiFile) {
      final PsiFile movedFile = (PsiFile)psiElement;
      movedFiles.put(movedFile, movedFile.getNode());
      MoveFileHandler.forElement(movedFile).prepareMovedFile(movedFile, movedFile.getParent(), oldToNewMap);
    }
    else if (psiElement instanceof PsiDirectory) {
      for (PsiElement element : psiElement.getChildren()) {
        processDirectoryFiles(movedFiles, oldToNewMap, element);
      }
    }
  }

  protected void retargetUsages(UsageInfo[] usages, Map<PsiElement, PsiElement> oldToNewMap) {
    final List<NonCodeUsageInfo> nonCodeUsages = new ArrayList<>();
    for (UsageInfo usageInfo : usages) {
      if (usageInfo instanceof MyUsageInfo) {
        final MyUsageInfo info = (MyUsageInfo)usageInfo;
        final PsiElement element = myElementsToMove[info.myIndex];

        if (info.getReference() instanceof FileReference || info.getReference() instanceof PsiDynaReference) {
          final PsiElement usageElement = info.getElement();
          if (usageElement != null) {
            final PsiFile usageFile = usageElement.getContainingFile();
            final PsiFile psiFile = usageFile.getViewProvider().getPsi(usageFile.getViewProvider().getBaseLanguage());
            if (psiFile != null && psiFile.equals(element)) {
              continue;  // already processed in MoveFilesOrDirectoriesUtil.doMoveFile
            }
          }
        }
        final PsiElement refElement = info.myReference.getElement();
        if (refElement.isValid()) {
          info.myReference.bindToElement(element);
        }
      } else if (usageInfo instanceof NonCodeUsageInfo) {
        nonCodeUsages.add((NonCodeUsageInfo)usageInfo);
      }
    }

    for (PsiFile movedFile : myFoundUsages.keySet()) {
      MoveFileHandler.forElement(movedFile).retargetUsages(myFoundUsages.get(movedFile), oldToNewMap);
    }

    myNonCodeUsages = nonCodeUsages.toArray(new NonCodeUsageInfo[0]);
  }

  @NotNull
  @Override
  protected String getCommandName() {
    return RefactoringBundle.message("move.title");
  }

  @Override
  protected boolean shouldDisableAccessChecks() {
    // No need to check access for files before move
    return true;
  }

  static class MyUsageInfo extends UsageInfo {
    int myIndex;
    PsiReference myReference;

    MyUsageInfo(PsiElement element, final int index, PsiReference reference) {
      super(element);
      myIndex = index;
      myReference = reference;
    }
  }
}
