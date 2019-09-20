/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ide.favoritesTreeView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.dnd.*;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.util.IconUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesPanel {
  private final Project myProject;
  private final FavoritesTreeViewPanel myViewPanel;
  private final DnDAwareTree myTree;
  private final AbstractTreeBuilder myTreeBuilder;
  private final FavoritesTreeStructure myTreeStructure;
  private final Image myFavoritesImage = IconUtil.toImage(AllIcons.Toolwindows.ToolWindowFavorites);

  public FavoritesPanel(Project project) {
    myProject = project;
    myViewPanel = new FavoritesTreeViewPanel(myProject);
    myTree = myViewPanel.getTree();
    myTreeBuilder = myViewPanel.getBuilder();
    if (myTreeBuilder != null) {
      Disposer.register(myProject, myTreeBuilder);
    }
    myTreeStructure = myViewPanel.getFavoritesTreeStructure();
    setupDnD();
  }

  public FavoritesTreeViewPanel getPanel() {
    return myViewPanel;
  }

  private void setupDnD() {
    DnDSupport.createBuilder(myTree)
      .setBeanProvider(info -> {
        final TreePath path = myTree.getPathForLocation(info.getPoint().x, info.getPoint().y);
        if (path != null && path.getPathCount() == 3) {
          Object o = path.getLastPathComponent();
          if (o instanceof DefaultMutableTreeNode) {
            o = ((DefaultMutableTreeNode)o).getUserObject();
            if (o instanceof FavoritesTreeNodeDescriptor) {
              FavoritesTreeNodeDescriptor root = ((FavoritesTreeNodeDescriptor)o).getFavoritesRoot();
              if (root != null && root != o) {
                o = root.getElement();
                if (o instanceof FavoritesListNode && ((FavoritesListNode)o).getProvider() == null) {
                  return new DnDDragStartBean(path);
                }
              }
            }
          }
        }
        if (path != null && path.getPathCount() == 2) {
          Object o = path.getLastPathComponent();
          if (o instanceof DefaultMutableTreeNode) {
            o = ((DefaultMutableTreeNode)o).getUserObject();
            if (o instanceof FavoritesTreeNodeDescriptor) {
              FavoritesTreeNodeDescriptor root = ((FavoritesTreeNodeDescriptor)o).getFavoritesRoot();
              if (root == o) {
                return new DnDDragStartBean(path);
              }
            }
          }
        }
        return new DnDDragStartBean("") {
          @Override
          public boolean isEmpty() {
            return true;
          }
        };
      })
        // todo process drag-and-drop here for tasks
      .setTargetChecker(new DnDTargetChecker() {
        @Override
        public boolean update(DnDEvent event) {
          final Object obj = event.getAttachedObject();
          if ("".equals(obj)) {
            event.setDropPossible(false);
            return false;
          }
          int pathCount = 0;
          if (obj instanceof TreePath) {
            pathCount = ((TreePath)obj).getPathCount();
            if (pathCount < 2) {
              event.setDropPossible(false);
              return true;
            }
          }
          FavoritesListNode node = myViewPanel.findFavoritesListNode(event.getPoint());
          if ((obj instanceof TreePath && pathCount == 3 && myViewPanel.myTree.getPath(node).isDescendant((TreePath)obj)) ||
              (node != null && node.getProvider() != null)) {
            event.setDropPossible(false);
            return false;
          }
          highlight(node, event);
          if (obj instanceof TreePath && pathCount == 2 && node != null && node.getProvider() == null) {
            event.setDropPossible(true);
            return true;
          }
          if (node != null) {
            event.setDropPossible(true);
            return true;
          }
          event.setDropPossible(false);
          return false;
        }
      })
      .setDropHandler(new DnDDropHandler() {
        @Override
        public void drop(DnDEvent event) {
          final FavoritesListNode node = myViewPanel.findFavoritesListNode(event.getPoint());
          final FavoritesManager mgr = FavoritesManager.getInstance(myProject);

          if (node == null) return;

          final String listTo = node.getValue();
          final Object obj = event.getAttachedObject();

          if (obj instanceof TreePath) {
            final TreePath path = (TreePath)obj;
            final String listFrom = FavoritesTreeViewPanel.getListNodeFromPath(path).getValue();
            if (listTo.equals(listFrom)) return;
            if (path.getPathCount() == 3) {
              final AbstractTreeNode abstractTreeNode =
                ((FavoritesTreeNodeDescriptor)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject()).getElement();
              Object element = abstractTreeNode.getValue();
              mgr.removeRoot(listFrom, Collections.singletonList(abstractTreeNode));
              if (element instanceof SmartPsiElementPointer) {
                element = ((SmartPsiElementPointer)element).getElement();
              }
              mgr.addRoots(listTo, null, element);
            }
            if (path.getPathCount() == 2) {//favorites lists manual sorting
              Rectangle bounds = myTree.getPathBounds(myTree.getPath(node));
              if (bounds != null) {
                mgr.setOrder(listFrom, listTo, event.getPoint().y < bounds.y + bounds.height / 2);
              }
            }
          }
          else if (obj instanceof Transferable && ((Transferable)obj).isDataFlavorSupported(DnDEventImpl.ourDataFlavor)) {
            List<PsiElement> elements = Collections.emptyList();
            try {
              Object data = ((Transferable)obj).getTransferData(DnDEventImpl.ourDataFlavor);
              if (data instanceof Object[]) elements = JBIterable.of((Object[])data).filter(PsiElement.class).toList();
            }
            catch (IOException | UnsupportedFlavorException ignored) {}
            if (!elements.isEmpty()) {
              myViewPanel.dropPsiElements(mgr, node, elements.toArray(PsiElement.EMPTY_ARRAY));
            }
          }
          else if (obj instanceof TransferableWrapper) {
            myViewPanel.dropPsiElements(mgr, node, ((TransferableWrapper)obj).getPsiElements());
          }
          else if (obj instanceof DnDNativeTarget.EventInfo) {
            myViewPanel.dropPsiElements(mgr, node,
                                        getPsiFiles(FileCopyPasteUtil.getFileList(((DnDNativeTarget.EventInfo)obj).getTransferable())));
          }
        }
      })
      .setImageProvider(
        info -> new DnDImage(myFavoritesImage, new Point(-myFavoritesImage.getWidth(null) / 2, -myFavoritesImage.getHeight(null) / 2)))
      .enableAsNativeTarget()
      .setDisposableParent(myProject)
      .install();
  }

  private void highlight(FavoritesListNode node, DnDEvent event) {
    int pathCount = 0;
    Object object = event.getAttachedObject();
    if (object instanceof TreePath) {
      pathCount = ((TreePath)object).getPathCount();
    }
    if (node != null) {
      TreePath pathToList = myTree.getPath(node);
      while (pathToList != null) {
        final Object pathObj = pathToList.getLastPathComponent();
        if (pathObj instanceof DefaultMutableTreeNode) {
          final Object userObject = ((DefaultMutableTreeNode)pathObj).getUserObject();
          if (userObject instanceof FavoritesTreeNodeDescriptor) {
            if (((FavoritesTreeNodeDescriptor)userObject).getElement() == node) {
              break;
            }
          }
        }
        pathToList = pathToList.getParentPath();
      }
      if (pathToList != null) {
        Rectangle bounds = myTree.getPathBounds(pathToList);
        if (bounds != null) {
          if (pathCount == 2) {
            FavoritesListNode pathToReorder = FavoritesTreeViewPanel.getListNodeFromPath((TreePath)object);
            FavoritesListNode anchorPath = FavoritesTreeViewPanel.getListNodeFromPath(pathToList);
            boolean below = event.getPoint().y >= bounds.y + bounds.height / 2;
            if (pathToReorder == null || anchorPath == null || !FavoritesManager.getInstance(myProject).canReorder(pathToReorder.getValue(), anchorPath.getValue(), !below)) {
              event.hideHighlighter();
              return;
            }
            if (below) {
              bounds.y+=bounds.height - 2;
            }
            bounds.height = 2;
          }
          event.setHighlighting(new RelativeRectangle(myTree, bounds), DnDEvent.DropTargetHighlightingType.RECTANGLE);
        }
      }
    }
    else {
      event.hideHighlighter();
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
}
