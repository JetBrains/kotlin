// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.move.moveFilesOrDirectories;

import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public final class MoveFilesOrDirectoriesUtil {
  private MoveFilesOrDirectoriesUtil() { }

  /**
   * Moves the specified directory to the specified parent directory. Does not process non-code usages!
   *
   * @param aDirectory          the directory to move.
   * @param destDirectory the directory to move {@code dir} into.
   * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
   */
  public static void doMoveDirectory(final PsiDirectory aDirectory, final PsiDirectory destDirectory) throws IncorrectOperationException {
    PsiManager manager = aDirectory.getManager();
    // do actual move
    checkMove(aDirectory, destDirectory);

    try {
      aDirectory.getVirtualFile().move(manager, destDirectory.getVirtualFile());
    }
    catch (IOException e) {
      throw new IncorrectOperationException(e);
    }
    DumbService.getInstance(manager.getProject()).completeJustSubmittedTasks();
  }

  /**
   * Moves the specified file to the specified directory. Does not process non-code usages! file may be invalidated, need to be refreshed before use, like {@code newDirectory.findFile(file.getName())}
   *
   * @param file         the file to move.
   * @param newDirectory the directory to move the file into.
   * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
   */
  public static void doMoveFile(@NotNull PsiFile file, @NotNull PsiDirectory newDirectory) throws IncorrectOperationException {
    // the class is already there, this is true when multiple classes are defined in the same file
    if (!newDirectory.equals(file.getContainingDirectory())) {
      // do actual move
      checkMove(file, newDirectory);

      VirtualFile vFile = file.getVirtualFile();
      if (vFile == null) {
        throw new IncorrectOperationException("Non-physical file: " + file + " (" + file.getClass() + ")");
      }

      try {
        vFile.move(file.getManager(), newDirectory.getVirtualFile());
      }
      catch (IOException e) {
        throw new IncorrectOperationException(e);
      }
    }
  }

  /**
   * @param elements should contain PsiDirectories or PsiFiles only
   */
  public static void doMove(final Project project,
                            final PsiElement[] elements,
                            final PsiElement[] targetElement,
                            final MoveCallback moveCallback) {
    doMove(project, elements, targetElement, moveCallback, null);
  }

  /**
   * @param elements should contain PsiDirectories or PsiFiles only if adjustElements == null
   */
  public static void doMove(final Project project,
                            final PsiElement[] elements,
                            final PsiElement[] targetElement,
                            final MoveCallback moveCallback,
                            final Function<PsiElement[], PsiElement[]> adjustElements) {
    if (adjustElements == null) {
      for (PsiElement element : elements) {
        if (!(element instanceof PsiFile) && !(element instanceof PsiDirectory)) {
          throw new IllegalArgumentException("unexpected element type: " + element);
        }
      }
    }

    final PsiDirectory targetDirectory = resolveToDirectory(project, targetElement[0]);
    if (targetElement[0] != null && targetDirectory == null) return;

    final PsiElement[] adjustedElements = adjustElements != null ? adjustElements.fun(elements) : elements;

    final PsiDirectory initialTargetDirectory = getInitialTargetDirectory(targetDirectory, elements);

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      Objects.requireNonNull(initialTargetDirectory, "It is null! The target directory, it is null!");
      doMove(project, elements, adjustedElements, initialTargetDirectory, moveCallback, EmptyRunnable.INSTANCE);
    }
    else {
      new MoveFilesOrDirectoriesDialog(project, adjustedElements, initialTargetDirectory) {
        @Override
        protected void performMove(@NotNull PsiDirectory targetDirectory) {
          Runnable doneCallback = this::closeOKAction;
          doMove(project, elements, adjustedElements, targetDirectory, moveCallback, doneCallback);
        }
      }.show();
    }
  }

  private static void doMove(Project project,
                             PsiElement[] elements,
                             PsiElement[] adjustedElements,
                             PsiDirectory targetDirectory,
                             MoveCallback moveCallback,
                             Runnable doneCallback) {
    CommandProcessor.getInstance().executeCommand(project, () -> {
      Collection<PsiElement> toCheck = ContainerUtil.newArrayList(targetDirectory);
      for (PsiElement e : adjustedElements) {
        toCheck.add(e instanceof PsiFileSystemItem && e.getParent() != null ? e.getParent() : e);
      }
      if (!CommonRefactoringUtil.checkReadOnlyStatus(project, toCheck, false)) {
        return;
      }

      try {
        int[] choice = elements.length > 1 || elements[0] instanceof PsiDirectory ? new int[]{-1} : null;
        List<PsiElement> els = new ArrayList<>();
        for (PsiElement psiElement : adjustedElements) {
          if (psiElement instanceof PsiFile) {
            PsiFile file = (PsiFile)psiElement;
            if (CopyFilesOrDirectoriesHandler.checkFileExist(targetDirectory, choice, file, file.getName(), "Move")) continue;
          }
          checkMove(psiElement, targetDirectory);
          els.add(psiElement);
        }

        if (els.isEmpty()) {
          doneCallback.run();
        }
        else {
          new MoveFilesOrDirectoriesProcessor(project, els.toArray(PsiElement.EMPTY_ARRAY), targetDirectory,
                                              RefactoringSettings.getInstance().MOVE_SEARCH_FOR_REFERENCES_FOR_FILE,
                                              false, false, moveCallback, doneCallback).run();
        }
      }
      catch (IncorrectOperationException e) {
        CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.getMessage(), "refactoring.moveFile", project);
      }
    }, MoveHandler.getRefactoringName(), null);
  }

  @Nullable
  public static PsiDirectory resolveToDirectory(final Project project, final PsiElement element) {
    if (!(element instanceof PsiDirectoryContainer)) {
      return (PsiDirectory)element;
    }

    PsiDirectory[] directories = ((PsiDirectoryContainer)element).getDirectories();
    switch (directories.length) {
      case 0:
        return null;
      case 1:
        return directories[0];
      default:
        return DirectoryChooserUtil.chooseDirectory(directories, directories[0], project, new HashMap<>());
    }

  }

  @Nullable
  private static PsiDirectory getCommonDirectory(PsiElement[] movedElements) {
    PsiDirectory commonDirectory = null;

    for (PsiElement movedElement : movedElements) {
      final PsiDirectory containingDirectory;
      if (movedElement instanceof PsiDirectory) {
        containingDirectory = ((PsiDirectory)movedElement).getParentDirectory();
      }
      else {
        final PsiFile containingFile = movedElement.getContainingFile();
        containingDirectory = containingFile == null ? null : containingFile.getContainingDirectory();
      }

      if (containingDirectory != null) {
        if (commonDirectory == null) {
          commonDirectory = containingDirectory;
        }
        else {
          if (commonDirectory != containingDirectory) {
            return null;
          }
        }
      }
    }
    return commonDirectory;
  }

  @Nullable
  public static PsiDirectory getInitialTargetDirectory(PsiDirectory initialTargetElement, final PsiElement[] movedElements) {
    PsiDirectory initialTargetDirectory = initialTargetElement;
    if (initialTargetDirectory == null) {
      if (movedElements != null) {
        final PsiDirectory commonDirectory = getCommonDirectory(movedElements);
        if (commonDirectory != null) {
          initialTargetDirectory = commonDirectory;
        }
        else {
          initialTargetDirectory = getContainerDirectory(movedElements[0]);
        }
      }
    }
    return initialTargetDirectory;
  }

  @Nullable
  private static PsiDirectory getContainerDirectory(final PsiElement psiElement) {
    if (psiElement instanceof PsiDirectory) {
      return (PsiDirectory)psiElement;
    }
    else if (psiElement != null) {
      PsiFile containingFile = psiElement.getContainingFile();
      if (containingFile != null) {
        return containingFile.getContainingDirectory();
      }
    }

    return null;
  }

  /**
   * Checks if it is possible to move the specified PSI element under the specified container,
   * and throws an exception if the move is not possible. Does not actually modify anything.
   *
   * @param element      the element to check the move possibility.
   * @param newContainer the target container element to move into.
   * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
   */
  public static void checkMove(@NotNull PsiElement element, @NotNull PsiElement newContainer) throws IncorrectOperationException {
    if (element instanceof PsiDirectoryContainer) {
      PsiDirectory[] dirs = ((PsiDirectoryContainer)element).getDirectories();
      if (dirs.length == 0) {
        throw new IncorrectOperationException();
      }
      else if (dirs.length > 1) {
        throw new IncorrectOperationException(
          "Moving of packages represented by more than one physical directory is not supported.");
      }
      checkMove(dirs[0], newContainer);
      return;
    }

    //element.checkDelete(); //move != delete + add
    newContainer.checkAdd(element);
    checkIfMoveIntoSelf(element, newContainer);
  }

  public static void checkIfMoveIntoSelf(PsiElement element, PsiElement newContainer) throws IncorrectOperationException {
    PsiElement container = newContainer;
    while (container != null) {
      if (container == element) {
        if (element instanceof PsiDirectory) {
          if (element == newContainer) {
            throw new IncorrectOperationException("Cannot place directory into itself.");
          }
          else {
            throw new IncorrectOperationException("Cannot place directory into its subdirectory.");
          }
        }
        else {
          throw new IncorrectOperationException();
        }
      }
      container = container.getParent();
    }
  }
}
