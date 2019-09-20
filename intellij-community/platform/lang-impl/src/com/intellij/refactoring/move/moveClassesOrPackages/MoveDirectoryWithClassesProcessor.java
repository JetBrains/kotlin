/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.refactoring.move.moveClassesOrPackages;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringEventData;
import com.intellij.refactoring.move.FileReferenceContextUtil;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveMultipleElementsViewDescriptor;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.refactoring.util.NonCodeUsageInfo;
import com.intellij.refactoring.util.RefactoringUIUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class MoveDirectoryWithClassesProcessor extends BaseRefactoringProcessor {
  private final PsiDirectory[] myDirectories;
  private final PsiDirectory myTargetDirectory;
  private final boolean mySearchInComments;
  private final boolean mySearchInNonJavaFiles;
  private final Map<VirtualFile, TargetDirectoryWrapper> myFilesToMove;
  private final Map<PsiDirectory, TargetDirectoryWrapper> myNestedDirsToMove;
  private NonCodeUsageInfo[] myNonCodeUsages;
  private final MoveCallback myMoveCallback;
  private final PsiManager myManager;

  public MoveDirectoryWithClassesProcessor(Project project,
                                           PsiDirectory[] directories,
                                           PsiDirectory targetDirectory,
                                           boolean searchInComments,
                                           boolean searchInNonJavaFiles,
                                           boolean includeSelf,
                                           MoveCallback moveCallback) {
    super(project);
    if (targetDirectory != null) {
      final List<PsiDirectory> dirs = new ArrayList<>(Arrays.asList(directories));
      for (Iterator<PsiDirectory> iterator = dirs.iterator(); iterator.hasNext(); ) {
        final PsiDirectory directory = iterator.next();
        if (targetDirectory.equals(directory.getParentDirectory()) || targetDirectory.equals(directory)) {
          iterator.remove();
        }
      }
      directories = dirs.toArray(PsiDirectory.EMPTY_ARRAY);
    }
    myManager = PsiManager.getInstance(project);
    myDirectories = directories;
    myTargetDirectory = targetDirectory;
    mySearchInComments = searchInComments;
    mySearchInNonJavaFiles = searchInNonJavaFiles;
    myMoveCallback = moveCallback;
    myFilesToMove = new HashMap<>();
    myNestedDirsToMove = new LinkedHashMap<>();
    for (PsiDirectory dir : directories) {
      collectFiles2Move(myFilesToMove, myNestedDirsToMove, dir, includeSelf ? dir.getParentDirectory() : dir, getTargetDirectory(dir));
    }
  }

  @NotNull
  @Override
  protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
    PsiElement[] elements = new PsiElement[myFilesToMove.size()];
    final PsiFile[] classes = PsiUtilCore.toPsiFileArray(getPsiFiles());
    System.arraycopy(classes, 0, elements, 0, classes.length);
    return new MoveMultipleElementsViewDescriptor(elements, getTargetName());
  }

  private Set<PsiFile> getPsiFiles() {
    return myFilesToMove.keySet().stream().map(myManager::findFile).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  protected String getTargetName() {
    return RefactoringUIUtil.getDescription(getTargetDirectory(null).getTargetDirectory(), false);
  }

  @NotNull
  @Override
  public UsageInfo[] findUsages() {
    final List<UsageInfo> usages = new ArrayList<>();
    for (MoveDirectoryWithClassesHelper helper : MoveDirectoryWithClassesHelper.findAll()) {
      helper.findUsages(getPsiFiles(), myDirectories, usages, mySearchInComments, mySearchInNonJavaFiles, myProject);
    }
    return UsageViewUtil.removeDuplicatedUsages(usages.toArray(UsageInfo.EMPTY_ARRAY));
  }

  private void collectConflicts(@NotNull MultiMap<PsiElement, String> conflicts,
                                @NotNull Ref<UsageInfo[]> refUsages) {
    for (VirtualFile vFile : myFilesToMove.keySet()) {
      PsiFile file = myManager.findFile(vFile);
      if (file == null) continue;
      try {
        myFilesToMove.get(vFile).checkMove(file);
      }
      catch (IncorrectOperationException e) {
        conflicts.putValue(file, e.getMessage());
      }
    }
    for (MoveDirectoryWithClassesHelper helper : MoveDirectoryWithClassesHelper.findAll()) {
      helper.preprocessUsages(myProject, getPsiFiles(), refUsages.get(), myTargetDirectory, conflicts);
    }
  }

  @Override
  protected boolean preprocessUsages(@NotNull final Ref<UsageInfo[]> refUsages) {
    final MultiMap<PsiElement, String> conflicts = new MultiMap<>();
    if (!ProgressManager.getInstance()
      .runProcessWithProgressSynchronously(() -> ReadAction.run(() -> collectConflicts(conflicts, refUsages)), RefactoringBundle.message("detecting.possible.conflicts"), true, myProject)) {
      return false;
    }
    return showConflicts(conflicts, refUsages.get());
  }

  @Override
  public void performRefactoring(@NotNull UsageInfo[] usages) {
    //try to create all directories beforehand
    try {
      //top level directories should be created even if they are empty
      for (PsiDirectory directory : myDirectories) {
        getResultDirectory(directory).findOrCreateTargetDirectory();
      }

      for (PsiDirectory directory : myNestedDirsToMove.keySet()) {
        myNestedDirsToMove.get(directory).findOrCreateTargetDirectory();
      }

      for (VirtualFile virtualFile : myFilesToMove.keySet()) {
        myFilesToMove.get(virtualFile).findOrCreateTargetDirectory();
      }

      DumbService.getInstance(myProject).completeJustSubmittedTasks();

      final List<PsiFile> movedFiles = new ArrayList<>();
      final Map<PsiElement, PsiElement> oldToNewElementsMapping = new HashMap<>();
      for (VirtualFile virtualFile : myFilesToMove.keySet()) {
        PsiFile file = myManager.findFile(virtualFile);
        if (file == null) continue;
        for (MoveDirectoryWithClassesHelper helper : MoveDirectoryWithClassesHelper.findAll()) {
          helper.beforeMove(file);
        }
        final RefactoringElementListener listener = getTransaction().getElementListener(file);
        final PsiDirectory moveDestination = myFilesToMove.get(virtualFile).getTargetDirectory();
  
        for (MoveDirectoryWithClassesHelper helper : MoveDirectoryWithClassesHelper.findAll()) {
          boolean processed = helper.move(file, moveDestination, oldToNewElementsMapping, movedFiles, listener);
          if (processed) {
            break;
          }
        }
      }
      for (PsiElement newElement : oldToNewElementsMapping.values()) {
        for (MoveDirectoryWithClassesHelper helper : MoveDirectoryWithClassesHelper.findAll()) {
          helper.afterMove(newElement);
        }
      }

      // fix references in moved files to outer files
      for (PsiFile movedFile : movedFiles) {
        MoveFileHandler.forElement(movedFile).updateMovedFile(movedFile);
        FileReferenceContextUtil.decodeFileReferences(movedFile);
      }

      myNonCodeUsages = CommonMoveUtil.retargetUsages(usages, oldToNewElementsMapping);
      for (MoveDirectoryWithClassesHelper helper : MoveDirectoryWithClassesHelper.findAll()) {
        helper.postProcessUsages(usages, dir -> getResultDirectory(dir).findOrCreateTargetDirectory());
      }
      for (PsiDirectory directory : myDirectories) {
        if (!isUsedInTarget(directory)) {
          directory.delete();
        }
      }

      for (PsiDirectory directory : myNestedDirsToMove.keySet()) {
        if (directory.isValid() && directory.getChildren().length == 0) {
          directory.delete();
        }
      }

    }
    catch (IncorrectOperationException e) {
      myNonCodeUsages = new NonCodeUsageInfo[0];
      RefactoringUIUtil.processIncorrectOperation(myProject, e);
    }
  }

  private boolean isUsedInTarget(PsiDirectory directory) {
    PsiDirectory targetDirectory = myNestedDirsToMove.get(directory).getTargetDirectory();
    //don't delete super directory if move was performed inside subpackage
    if (targetDirectory != null && PsiTreeUtil.isAncestor(directory, targetDirectory, false)) {
      return true;
    }
    //don't delete subdirectory: something could be moved in there
    if (PsiTreeUtil.isAncestor(targetDirectory, directory, false)) {
      return true;
    }

    return false;
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
    data.addElements(myDirectories);
    return data;
  }

  @Nullable
  @Override
  protected RefactoringEventData getAfterData(@NotNull UsageInfo[] usages) {
    RefactoringEventData data = new RefactoringEventData();
    data.addElement(myTargetDirectory);
    return data;
  }

  private TargetDirectoryWrapper getResultDirectory(PsiDirectory dir) {
    return myTargetDirectory != null
           ? new TargetDirectoryWrapper(myTargetDirectory, dir.getName())
           : getTargetDirectory(dir);
  }

  @Override
  protected void performPsiSpoilingRefactoring() {
    if (myNonCodeUsages == null) return; //refactoring was aborted
    RenameUtil.renameNonCodeUsages(myProject, myNonCodeUsages);
    if (myMoveCallback != null) {
      myMoveCallback.refactoringCompleted();
    }
  }

  private static void collectFiles2Move(Map<VirtualFile, TargetDirectoryWrapper> files2Move,
                                        Map<PsiDirectory, TargetDirectoryWrapper> nestedDirsToMove,
                                        PsiDirectory directory,
                                        PsiDirectory rootDirectory,
                                        @NotNull TargetDirectoryWrapper targetDirectory) {
    final PsiElement[] children = directory.getChildren();
    final String relativePath = VfsUtilCore.getRelativePath(directory.getVirtualFile(), rootDirectory.getVirtualFile(), '/');

    final TargetDirectoryWrapper newTargetDirectory = relativePath.length() == 0
                                                      ? targetDirectory
                                                      : targetDirectory.findOrCreateChild(relativePath);
    nestedDirsToMove.put(directory, newTargetDirectory);
    for (PsiElement child : children) {
      if (child instanceof PsiFile) {
        files2Move.put(PsiUtilCore.getVirtualFile(child), newTargetDirectory);
      }
      else if (child instanceof PsiDirectory){
        collectFiles2Move(files2Move, nestedDirsToMove, (PsiDirectory)child, directory, newTargetDirectory);
      }
    }
  }

  @NotNull
  @Override
  protected String getCommandName() {
    return RefactoringBundle.message("moving.directories.command");
  }

  public TargetDirectoryWrapper getTargetDirectory(PsiDirectory dir) {
    return new TargetDirectoryWrapper(myTargetDirectory);
  }

  public static class TargetDirectoryWrapper {
    private TargetDirectoryWrapper myParentDirectory;
    private PsiDirectory myTargetDirectory;
    private String myRelativePath;

    public TargetDirectoryWrapper(PsiDirectory targetDirectory) {
      myTargetDirectory = targetDirectory;
    }

    public TargetDirectoryWrapper(TargetDirectoryWrapper parentDirectory, String relativePath) {
      myParentDirectory = parentDirectory;
      myRelativePath = relativePath;
    }

    public TargetDirectoryWrapper(PsiDirectory parentDirectory, String relativePath) {
      myTargetDirectory = parentDirectory.findSubdirectory(relativePath);
      //in case it was null
      myParentDirectory = new TargetDirectoryWrapper(parentDirectory);
      myRelativePath = relativePath;
    }

    public PsiDirectory findOrCreateTargetDirectory() throws IncorrectOperationException{
      if (myTargetDirectory == null) {
        PsiDirectory root = myParentDirectory.findOrCreateTargetDirectory();

        final String[] pathComponents = myRelativePath.split("/");
        for (String component : pathComponents) {
          myTargetDirectory = root.findSubdirectory(component);
          if (myTargetDirectory == null) {
            myTargetDirectory = root.createSubdirectory(component);
          }
          root = myTargetDirectory;
        }
      }
      return myTargetDirectory;
    }

    @Nullable
    public PsiDirectory getTargetDirectory() {
      return myTargetDirectory;
    }

    public TargetDirectoryWrapper findOrCreateChild(String relativePath) {
      if (myTargetDirectory != null) {
        final PsiDirectory psiDirectory = myTargetDirectory.findSubdirectory(relativePath);
        if (psiDirectory != null) {
          return new TargetDirectoryWrapper(psiDirectory);
        }
      }
      return new TargetDirectoryWrapper(this, relativePath);
    }

    public void checkMove(PsiFile psiFile) throws IncorrectOperationException {
      if (myTargetDirectory != null) {
        MoveFilesOrDirectoriesUtil.checkMove(psiFile, myTargetDirectory);
      }
    }
  }
}
