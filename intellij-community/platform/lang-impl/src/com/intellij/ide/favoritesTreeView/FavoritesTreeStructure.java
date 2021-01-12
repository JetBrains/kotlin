// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.projectView.SettingsProvider;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectTreeStructure;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.ide.favoritesTreeView.FavoritesViewTreeBuilder.ID;

/**
 * @author Konstantin Bulenkov
 */
public final class FavoritesTreeStructure extends ProjectTreeStructure {
  private static final Logger LOGGER = Logger.getInstance(FavoritesTreeStructure.class);
  private final TreeStructureProvider myNonProjectProvider;
  public FavoritesTreeStructure(@NotNull Project project) {
    super(project, ID);
    myNonProjectProvider = new MyProvider(project);
  }

  @Override
  protected AbstractTreeNode<?> createRoot(@NotNull final Project project, @NotNull ViewSettings settings) {
    return new FavoritesRootNode(project);
  }

  public void rootsChanged() {
    ((FavoritesRootNode)getRootElement()).rootsChanged();
  }


  @Override
  public Object @NotNull [] getChildElements(@NotNull Object element) {
    if (!(element instanceof AbstractTreeNode)) {
      return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
    }

    AbstractTreeNode<?> favTreeElement = (AbstractTreeNode<?>)element;
    try {
      if (!(element instanceof FavoritesListNode)) {
        Object[] elements = super.getChildElements(favTreeElement);
        if (elements.length > 0) return elements;

        ViewSettings settings = favTreeElement instanceof SettingsProvider ? ((SettingsProvider)favTreeElement).getSettings() : ViewSettings.DEFAULT;
        return ArrayUtil.toObjectArray(myNonProjectProvider.modify(favTreeElement, new ArrayList<>(), settings));
      }

      List<AbstractTreeNode<?>> result = new ArrayList<>();
      FavoritesListNode listNode = (FavoritesListNode)element;
      if (listNode.getProvider() != null) {
        return ArrayUtil.toObjectArray(listNode.getChildren());
      }
      Collection<AbstractTreeNode<?>> roots = FavoritesListNode.getFavoritesRoots(myProject, listNode.getName(), listNode);
      for (AbstractTreeNode<?> abstractTreeNode : roots) {
        final Object value = abstractTreeNode.getValue();

        if (value == null) continue;
        if (value instanceof PsiElement && !((PsiElement)value).isValid()) continue;
        if (value instanceof SmartPsiElementPointer && ((SmartPsiElementPointer<?>)value).getElement() == null) continue;

        boolean invalid = false;
        for (FavoriteNodeProvider nodeProvider : FavoriteNodeProvider.EP_NAME.getExtensions(myProject)) {
          if (nodeProvider.isInvalidElement(value)) {
            invalid = true;
            break;
          }
        }
        if (invalid) continue;

        result.add(abstractTreeNode);
      }
      return ArrayUtil.toObjectArray(result);
    }
    catch (ProcessCanceledException ignored) {
    }
    catch (Exception e) {
      LOGGER.error(e);
    }

    return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public Object getParentElement(@NotNull Object element) {
    AbstractTreeNode<?> parent = null;
    if (element == getRootElement()) {
      return null;
    }
    if (element instanceof AbstractTreeNode) {
      parent = ((AbstractTreeNode<?>)element).getParent();
    }
    if (parent == null) {
      return getRootElement();
    }
    return parent;
  }

  @Override
  @NotNull
  public NodeDescriptor<?> createDescriptor(@NotNull Object element, NodeDescriptor parentDescriptor) {
    return new FavoriteTreeNodeDescriptor(myProject, parentDescriptor, (AbstractTreeNode<?>)element);
  }

  private static class MyProvider implements TreeStructureProvider {
    private final Project myProject;

    MyProvider(Project project) {
      myProject = project;
    }

    @NotNull
    @Override
    public Collection<AbstractTreeNode<?>> modify(@NotNull AbstractTreeNode<?> parent,
                                               @NotNull Collection<AbstractTreeNode<?>> children,
                                               ViewSettings settings) {
      if (parent instanceof PsiDirectoryNode && children.isEmpty()) {
        VirtualFile virtualFile = ((PsiDirectoryNode)parent).getVirtualFile();
        if (virtualFile == null) return children;
        VirtualFile[] virtualFiles = virtualFile.getChildren();
        List<AbstractTreeNode<?>> result = new ArrayList<>();
        PsiManagerImpl psiManager = (PsiManagerImpl)PsiManager.getInstance(myProject);
        for (VirtualFile file : virtualFiles) {
          AbstractTreeNode<?> child;
          if (file.isDirectory()) {
            PsiDirectory directory = psiManager.findDirectory(file);
            if (directory == null) continue;
            child = new PsiDirectoryNode(myProject, directory, settings);
          }
          else {
            PsiFile psiFile = psiManager.findFile(file);
            if (psiFile == null) continue;
            child = new PsiFileNode(myProject, psiFile, settings);
          }
          child.setParent(parent);
          result.add(child);
        }
        return result;
      }
      return children;
    }
  }
}
