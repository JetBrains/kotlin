// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.DataManager;
import com.intellij.ide.dnd.*;
import com.intellij.ide.projectView.impl.nodes.DropTargetNode;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringActionHandlerFactory;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.refactoring.copy.CopyHandler;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.intellij.util.ui.tree.TreeUtil.getLastUserObject;

/**
 * @author Anna
 * @author Konstantin Bulenkov
 */
abstract class ProjectViewDropTarget implements DnDNativeTarget {
  private final JTree myTree;
  private final Project myProject;

  ProjectViewDropTarget(JTree tree, Project project) {
    myTree = tree;
    myProject = project;
  }

  @Override
  public boolean update(DnDEvent event) {
    event.setDropPossible(false, "");

    Point point = event.getPoint();
    if (point == null) return false;

    TreePath target = myTree.getClosestPathForLocation(point.x, point.y);
    if (target == null) return false;

    Rectangle bounds = myTree.getPathBounds(target);
    if (bounds == null || bounds.y > point.y || point.y >= bounds.y + bounds.height) return false;

    DropHandler handler = getDropHandler(event);
    if (handler == null) return false;

    TreePath[] sources = getSourcePaths(event.getAttachedObject());
    if (sources != null) {
      if (ArrayUtilRt.find(sources, target) != -1) return false;//TODO???? nodes
      if (!handler.isValidSource(sources, target)) return false;
      if (Stream.of(sources).allMatch(source -> handler.isDropRedundant(source, target))) return false;
    }
    else if (!FileCopyPasteUtil.isFileListFlavorAvailable(event)) {
      return false;
    }
    else {
      // it seems like it's not possible to obtain dragged items _before_ accepting _drop_ on Macs, so just skip this check
      if (!SystemInfo.isMac) {
        PsiFileSystemItem[] psiFiles = getPsiFiles(FileCopyPasteUtil.getFileListFromAttachedObject(event.getAttachedObject()));
        if (psiFiles == null || psiFiles.length == 0) return false;
        if (!MoveHandler.isValidTarget(getPsiElement(target), psiFiles)) return false;
      }
    }
    event.setHighlighting(new RelativeRectangle(myTree, bounds), DnDEvent.DropTargetHighlightingType.RECTANGLE);
    event.setDropPossible(true);
    return false;
  }

  @Override
  public void drop(DnDEvent event) {
    Point point = event.getPoint();
    if (point == null) return;

    TreePath target = myTree.getClosestPathForLocation(point.x, point.y);
    if (target == null) return;

    Rectangle bounds = myTree.getPathBounds(target);
    if (bounds == null || bounds.y > point.y || point.y >= bounds.y + bounds.height) return;

    DropHandler handler = getDropHandler(event);
    if (handler == null) return;

    final Object attached = event.getAttachedObject();
    TreePath[] sources = getSourcePaths(event.getAttachedObject());

    if (sources == null) {
      if (FileCopyPasteUtil.isFileListFlavorAvailable(event)) {
        List<File> fileList = FileCopyPasteUtil.getFileListFromAttachedObject(attached);
        if (!fileList.isEmpty()) {
          handler.doDropFiles(fileList, target);
        }
      }
    }
    else {
      doValidDrop(sources, target, handler);
    }
  }

  @Override
  public void cleanUpOnLeave() {
  }

  @Override
  public void updateDraggedImage(Image image, Point dropPoint, Point imageOffset) {
  }

  @Nullable
  private static TreePath[] getSourcePaths(Object transferData) {
    TransferableWrapper wrapper = transferData instanceof TransferableWrapper ? (TransferableWrapper)transferData : null;
    return wrapper == null ? null : wrapper.getTreePaths();
  }

  private static void doValidDrop(@NotNull TreePath[] sources, @NotNull TreePath target, @NotNull DropHandler handler) {
    target = getValidTarget(sources, target, handler);
    if (target != null) {
      sources = removeRedundant(sources, target, handler);
      if (sources.length != 0) handler.doDrop(sources, target);
    }
  }

  @Nullable
  private static TreePath getValidTarget(@NotNull TreePath[] sources, @NotNull TreePath target, @NotNull DropHandler handler) {
    while (target != null) {
      if (handler.isValidTarget(sources, target)) return target;
      if (!handler.shouldDelegateToParent(sources, target)) break;
      target = target.getParentPath();
    }
    return null;
  }

  @NotNull
  private static TreePath[] removeRedundant(@NotNull TreePath[] sources, @NotNull TreePath target, @NotNull DropHandler dropHandler) {
    return Stream.of(sources).filter(source -> !dropHandler.isDropRedundant(source, target)).toArray(TreePath[]::new);
  }

  private DropHandler getDropHandler(DnDEvent event) {
    if (event == null) return null;
    DnDAction action = event.getAction();
    if (action == null) return null;
    int id = action.getActionId();
    if (id == DnDConstants.ACTION_COPY) return new CopyDropHandler();
    if (id != DnDConstants.ACTION_COPY_OR_MOVE && id != DnDConstants.ACTION_MOVE) return null;
    return new MoveDropHandler();
  }

  private interface DropHandler {
    boolean isValidSource(@NotNull TreePath[] sources, @NotNull TreePath target);

    boolean isValidTarget(@NotNull TreePath[] sources, @NotNull TreePath target);

    boolean shouldDelegateToParent(@NotNull TreePath[] sources, @NotNull TreePath target);

    boolean isDropRedundant(@NotNull TreePath source, @NotNull TreePath target);

    void doDrop(@NotNull TreePath[] sources, @NotNull TreePath target);

    void doDropFiles(List<? extends File> files, @NotNull TreePath target);
  }

  @Nullable
  abstract PsiElement getPsiElement(@NotNull TreePath path);

  @Nullable
  abstract Module getModule(@NotNull PsiElement element);

  abstract class MoveCopyDropHandler implements DropHandler {
    @Override
    public boolean isValidSource(@NotNull TreePath[] sources, @NotNull TreePath target) {
      return canDrop(sources, target);
    }

    @Override
    public boolean isValidTarget(@NotNull TreePath[] sources, @NotNull TreePath target) {
      return canDrop(sources, target);
    }

    protected abstract boolean canDrop(@NotNull TreePath[] sources, @NotNull TreePath target);

    @NotNull
    protected PsiElement[] getPsiElements(@NotNull TreePath[] paths) {
      List<PsiElement> psiElements = new ArrayList<>(paths.length);
      for (TreePath path : paths) {
        PsiElement psiElement = getPsiElement(path);
        if (psiElement != null) {
          psiElements.add(psiElement);
        }
      }
      if (!psiElements.isEmpty()) {
        return PsiUtilCore.toPsiElementArray(psiElements);
      }
      return BaseRefactoringAction.getPsiElementArray(DataManager.getInstance().getDataContext(myTree));
    }
  }

  @Nullable
  protected PsiFileSystemItem[] getPsiFiles(@Nullable List<? extends File> fileList) {
    if (fileList == null) return null;
    List<PsiFileSystemItem> sourceFiles = new ArrayList<>();
    for (File file : fileList) {
      final VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
      PsiFileSystemItem psiFile = PsiUtilCore.findFileSystemItem(myProject, vFile);
      if (psiFile != null) {
        sourceFiles.add(psiFile);
      }
    }
    return sourceFiles.toArray(new PsiFileSystemItem[0]);
  }

  private class MoveDropHandler extends MoveCopyDropHandler {
    @Override
    protected boolean canDrop(@NotNull TreePath[] sources, @NotNull TreePath target) {
      DropTargetNode node = getLastUserObject(DropTargetNode.class, target);
      if (node != null && node.canDrop(sources)) return true;

      PsiElement[] sourceElements = getPsiElements(sources);
      PsiElement targetElement = getPsiElement(target);
      return sourceElements.length == 0 ||
             targetElement != null && MoveHandler.canMove(sourceElements, targetElement);
    }

    @Override
    public void doDrop(@NotNull TreePath[] sources, @NotNull TreePath target) {
      DropTargetNode node = getLastUserObject(DropTargetNode.class, target);
      if (node != null && node.canDrop(sources)) {
        node.drop(sources, DataManager.getInstance().getDataContext(myTree));
      }
      else {
        doDrop(getPsiElement(target), getPsiElements(sources), false);
      }
    }

    private void doDrop(PsiElement target, PsiElement[] sources, boolean externalDrop) {
      if (target == null) return;

      if (DumbService.isDumb(myProject)) {
        Messages.showMessageDialog(myProject, "Move refactoring is not available while indexing is in progress", "Indexing", null);
        return;
      }

      if (!myProject.isInitialized()) {
        Messages.showMessageDialog(myProject, "Move refactoring is not available while project initialization is in progress", "Project Initialization", null);
        return;
      }

      Module module = getModule(target);
      final DataContext dataContext = DataManager.getInstance().getDataContext(myTree);
      PsiDocumentManager.getInstance(myProject).commitAllDocuments();

      if (!target.isValid()) return;
      for (PsiElement element : sources) {
        if (!element.isValid()) return;
      }

      DataContext context = dataId -> {
        if (LangDataKeys.TARGET_MODULE.is(dataId)) {
          if (module != null) return module;
        }
        if (LangDataKeys.TARGET_PSI_ELEMENT.is(dataId)) {
          return target;
        }
        else {
          return externalDrop ? null : dataContext.getData(dataId);
        }
      };
      TransactionGuard.getInstance().submitTransactionAndWait(
        () -> getActionHandler().invoke(myProject, sources, context));
    }

    private RefactoringActionHandler getActionHandler() {
      return RefactoringActionHandlerFactory.getInstance().createMoveHandler();
    }

    @Override
    public boolean isDropRedundant(@NotNull TreePath source, @NotNull TreePath target) {
      return target.equals(source.getParentPath()) || MoveHandler.isMoveRedundant(getPsiElement(source), getPsiElement(target));
    }

    @Override
    public boolean shouldDelegateToParent(@NotNull TreePath[] sources, @NotNull TreePath target) {
      PsiElement psiElement = getPsiElement(target);
      return !MoveHandler.isValidTarget(psiElement, getPsiElements(sources));
    }

    @Override
    public void doDropFiles(List<? extends File> files, @NotNull TreePath target) {
      PsiFileSystemItem[] sourceFileArray = getPsiFiles(files);

      DropTargetNode node = getLastUserObject(DropTargetNode.class, target);
      if (node != null) {
        node.dropExternalFiles(sourceFileArray, DataManager.getInstance().getDataContext(myTree));
      }
      else {
        doDrop(getPsiElement(target), sourceFileArray, true);
      }
    }
  }

  private class CopyDropHandler extends MoveCopyDropHandler {
    @Override
    protected boolean canDrop(@NotNull TreePath[] sources, @NotNull TreePath target) {
      PsiElement[] sourceElements = getPsiElements(sources);
      PsiElement targetElement = getPsiElement(target);
      if (targetElement == null) return false;
      PsiFile containingFile = targetElement.getContainingFile();
      boolean isTargetAcceptable = targetElement instanceof PsiDirectoryContainer ||
                                   targetElement instanceof PsiDirectory ||
                                   containingFile != null && containingFile.getContainingDirectory() != null;
      return isTargetAcceptable && CopyHandler.canCopy(sourceElements);
    }

    @Override
    public void doDrop(@NotNull TreePath[] sources, @NotNull TreePath target) {
      PsiElement[] sourceElements = getPsiElements(sources);
      doDrop(target, sourceElements);
    }

    private void doDrop(@NotNull TreePath target, PsiElement[] sources) {
      final PsiElement targetElement = getPsiElement(target);
      if (targetElement == null) return;

      if (DumbService.isDumb(myProject)) {
        Messages.showMessageDialog(myProject, "Copy refactoring is not available while indexing is in progress", "Indexing", null);
        return;
      }

      final PsiDirectory psiDirectory;
      if (targetElement instanceof PsiDirectoryContainer) {
        final PsiDirectoryContainer directoryContainer = (PsiDirectoryContainer)targetElement;
        final PsiDirectory[] psiDirectories = directoryContainer.getDirectories();
        psiDirectory = psiDirectories.length != 0 ? psiDirectories[0] : null;
      }
      else if (targetElement instanceof PsiDirectory) {
        psiDirectory = (PsiDirectory)targetElement;
      }
      else {
        final PsiFile containingFile = targetElement.getContainingFile();
        LOG.assertTrue(containingFile != null, targetElement);
        psiDirectory = containingFile.getContainingDirectory();
      }
      TransactionGuard.getInstance().submitTransactionAndWait(() -> CopyHandler.doCopy(sources, psiDirectory));
    }

    @Override
    public boolean isDropRedundant(@NotNull TreePath source, @NotNull TreePath target) {
      return false;
    }

    @Override
    public boolean shouldDelegateToParent(@NotNull TreePath[] sources, @NotNull TreePath target) {
      PsiElement psiElement = getPsiElement(target);
      return !(psiElement instanceof PsiDirectoryContainer) && !(psiElement instanceof PsiDirectory);
    }

    @Override
    public void doDropFiles(List<? extends File> files, @NotNull TreePath target) {
      PsiFileSystemItem[] sourceFileArray = getPsiFiles(files);
      doDrop(target, sourceFileArray);
    }
  }
}
