// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.move.moveFilesOrDirectories;

import com.intellij.ide.util.EditorHelper;
import com.intellij.lang.FileASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.paths.PsiDynaReference;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
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
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class MoveFilesOrDirectoriesProcessor extends BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance(MoveFilesOrDirectoriesProcessor.class);

  protected final PsiElement[] myElementsToMove;
  private final boolean mySearchForReferences;
  protected final boolean mySearchInComments;
  protected final boolean mySearchInNonJavaFiles;
  private final PsiDirectory myNewParent;
  private final MoveCallback myMoveCallback;
  private NonCodeUsageInfo[] myNonCodeUsages;
  private final Map<PsiFile, List<UsageInfo>> myFoundUsages = new HashMap<>();

  public MoveFilesOrDirectoriesProcessor(@NotNull Project project,
                                         PsiElement @NotNull [] elements,
                                         @NotNull PsiDirectory newParent,
                                         boolean searchInComments,
                                         boolean searchInNonJavaFiles,
                                         MoveCallback moveCallback,
                                         Runnable prepareSuccessfulCallback) {
    this(project, elements, newParent, true, searchInComments, searchInNonJavaFiles, moveCallback, prepareSuccessfulCallback);
  }

  public MoveFilesOrDirectoriesProcessor(@NotNull Project project,
                                         PsiElement @NotNull [] elements,
                                         @NotNull PsiDirectory newParent,
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
  protected UsageViewDescriptor createUsageViewDescriptor(UsageInfo @NotNull [] usages) {
    return new MoveFilesOrDirectoriesViewDescriptor(myElementsToMove, myNewParent);
  }

  @Override
  protected UsageInfo @NotNull [] findUsages() {
    List<UsageInfo> result = new ArrayList<>();
    for (int i = 0; i < myElementsToMove.length; i++) {
      PsiElement element = myElementsToMove[i];
      if (mySearchForReferences) {
        for (PsiReference reference : ReferencesSearch.search(element, GlobalSearchScope.projectScope(myProject))) {
          result.add(new MyUsageInfo(reference, i));
        }
      }
      findElementUsages(result, element);
    }

    return result.toArray(UsageInfo.EMPTY_ARRAY);
  }

  private void findElementUsages(@NotNull List<? super UsageInfo> result, @NotNull PsiElement element) {
    if (!mySearchForReferences) {
      return;
    }
    if (element instanceof PsiFile) {
      final List<UsageInfo> usages = MoveFileHandler.forElement((PsiFile)element)
        .findUsages((PsiFile)element, myNewParent, mySearchInComments, mySearchInNonJavaFiles);
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
  protected void refreshElements(PsiElement @NotNull [] elements) {
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
  protected void performRefactoring(UsageInfo @NotNull [] usages) {
    // If files are being moved then I need to collect some information to delete these
    // files from CVS. I need to know all common parents of the moved files and relative
    // paths.

    // Move files with correction of references.

    try {

      Map<SmartPsiElementPointer<PsiFile>, FileASTNode> movedFiles = new LinkedHashMap<>();
      final Map<PsiElement, PsiElement> oldToNewMap = new HashMap<>();
      if (mySearchForReferences) {
        for (final PsiElement element : myElementsToMove) {
          if (element instanceof PsiDirectory) {
            encodeDirectoryFiles(element, movedFiles);
          }
          else if (element instanceof PsiFile) {
            FileReferenceContextUtil.encodeFileReferences(element);
          }
        }
      }

      List<RefactoringElementListener> listeners = ContainerUtil.map(myElementsToMove, item -> getTransaction().getElementListener(item));

      for (int i = 0; i < myElementsToMove.length; i++) {
        PsiElement element = myElementsToMove[i];
        if (element instanceof PsiDirectory) {
          MoveFilesOrDirectoriesUtil.doMoveDirectory((PsiDirectory)element, myNewParent);
          for (PsiElement psiElement : element.getChildren()) {
            processDirectoryFiles(movedFiles, oldToNewMap, psiElement);
          }
        }
        else if (element instanceof PsiFile) {
          final PsiFile movedFile = (PsiFile)element;
          FileASTNode node = movedFile.getNode();
          MoveFileHandler.forElement(movedFile).prepareMovedFile(movedFile, myNewParent, oldToNewMap);

          PsiFile moving = myNewParent.findFile(movedFile.getName());
          if (moving == null) {
            MoveFilesOrDirectoriesUtil.doMoveFile(movedFile, myNewParent);
          }
          moving = myNewParent.findFile(movedFile.getName());
          if (moving != null) {
            movedFiles.put(SmartPointerManager.createPointer(moving), moving.getNode());
            ObjectUtils.reachabilityFence(node);
          }
        }

        listeners.get(i).elementMoved(element);
      }
      // sort by offset descending to process correctly several usages in one PsiElement [IDEADEV-33013]
      CommonRefactoringUtil.sortDepthFirstRightLeftOrder(usages);

      DumbService.getInstance(myProject).completeJustSubmittedTasks();

      // fix references in moved files to outer files
      for (SmartPsiElementPointer<PsiFile> pointer : movedFiles.keySet()) {
        PsiFile movedFile = pointer.getElement();
        if (movedFile != null) {
          MoveFileHandler.forElement(movedFile).updateMovedFile(movedFile);
          if (mySearchForReferences) FileReferenceContextUtil.decodeFileReferences(movedFile);
        }
      }

      retargetUsages(usages, oldToNewMap);

      // Perform CVS "add", "remove" commands on moved files.

      if (myMoveCallback != null) {
        myMoveCallback.refactoringCompleted();
      }
      if (MoveFilesOrDirectoriesDialog.isOpenInEditorProperty()) {
        List<PsiFile> justFiles = ContainerUtil.mapNotNull(movedFiles.keySet(), SmartPsiElementPointer::getElement);
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
  protected RefactoringEventData getAfterData(UsageInfo @NotNull [] usages) {
    RefactoringEventData data = new RefactoringEventData();
    data.addElement(myNewParent);
    return data;
  }

  private static void encodeDirectoryFiles(@NotNull PsiElement psiElement, @NotNull Map<SmartPsiElementPointer<PsiFile>, FileASTNode> movedFiles) {
    if (psiElement instanceof PsiFile) {
      movedFiles.put(SmartPointerManager.createPointer((PsiFile)psiElement), ((PsiFile)psiElement).getNode());
      FileReferenceContextUtil.encodeFileReferences(psiElement);
    }
    else if (psiElement instanceof PsiDirectory) {
      for (PsiElement element : psiElement.getChildren()) {
        encodeDirectoryFiles(element, movedFiles);
      }
    }
  }

  private static void processDirectoryFiles(@NotNull Map<SmartPsiElementPointer<PsiFile>, FileASTNode> movedFiles, @NotNull Map<PsiElement, PsiElement> oldToNewMap, @NotNull PsiElement psiElement) {
    if (psiElement instanceof PsiFile) {
      final PsiFile movedFile = (PsiFile)psiElement;
      movedFiles.put(SmartPointerManager.createPointer(movedFile), movedFile.getNode());
      MoveFileHandler.forElement(movedFile).prepareMovedFile(movedFile, movedFile.getParent(), oldToNewMap);
    }
    else if (psiElement instanceof PsiDirectory) {
      for (PsiElement element : psiElement.getChildren()) {
        processDirectoryFiles(movedFiles, oldToNewMap, element);
      }
    }
  }

  protected void retargetUsages(UsageInfo @NotNull [] usages, @NotNull Map<PsiElement, PsiElement> oldToNewMap) {
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

    MyUsageInfo(@NotNull PsiReference reference, int index) {
      super(reference);
      myIndex = index;
      myReference = reference;
    }
  }
}
