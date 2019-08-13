// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.favoritesTreeView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author cdr
 */
public class FavoritesProjectViewPane extends AbstractProjectViewPane {
  @NonNls public static final String ID = "Favorites";
  private FavoritesTreeViewPanel myViewPanel;
  private final FavoritesManager myFavoritesManager;
  private static final Logger LOG = Logger.getInstance(FavoritesProjectViewPane.class);

  protected FavoritesProjectViewPane(@NotNull Project project, @NotNull FavoritesManager favoritesManager) {
    super(project);

    myFavoritesManager = favoritesManager;
    FavoritesListener favoritesListener = new FavoritesListener() {
      private boolean enabled = true;

      @Override
      public void rootsChanged() {
      }

      @Override
      public void listAdded(@NotNull String listName) {
        refreshMySubIdsAndSelect(listName);
      }

      @Override
      public void listRemoved(@NotNull String listName) {
        String selectedSubId = getSubId();
        refreshMySubIdsAndSelect(selectedSubId);
      }

      private void refreshMySubIdsAndSelect(String listName) {
        if (!enabled) {
          return;
        }

        try {
          enabled = false;
          ProjectView projectView = ProjectView.getInstance(myProject);
          projectView.removeProjectPane(FavoritesProjectViewPane.this);
          projectView.addProjectPane(FavoritesProjectViewPane.this);
          if (!myFavoritesManager.getAvailableFavoritesListNames().contains(listName)) {
            listName = null;
          }
          projectView.changeView(ID, listName);
        }
        finally {
          enabled = true;
        }
      }
    };
    myFavoritesManager.addFavoritesListener(favoritesListener, this);
  }

  @NotNull
  @Override
  public String getTitle() {
    return "Favorites";
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Toolwindows.ToolWindowFavorites;
  }

  @Override
  @NotNull
  public String getId() {
    return ID;
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    //if (myViewPanel != null) return myViewPanel;

    final String subId = getSubId();
    LOG.assertTrue(subId != null, getSubIds());
    myViewPanel = new FavoritesTreeViewPanel(myProject);
    myTree = myViewPanel.getTree();
    setTreeBuilder(myViewPanel.getBuilder());
    myTreeStructure = myViewPanel.getFavoritesTreeStructure();
    installComparator();
    enableDnD();
    return myViewPanel;
  }

  @Override
  public void dispose() {
    myViewPanel = null;
    super.dispose();
  }

  @Override
  @NotNull
  public String[] getSubIds() {
    return ArrayUtilRt.toStringArray(myFavoritesManager.getAvailableFavoritesListNames());
  }

  @Override
  @NotNull
  public String getPresentableSubIdName(@NotNull final String subId) {
    return subId;
  }

  @NotNull
  @Override
  public ActionCallback updateFromRoot(boolean restoreExpandedPaths) {
    return ((FavoritesViewTreeBuilder)getTreeBuilder()).updateFromRootCB();
  }

  @Override
  public void select(Object object, VirtualFile file, boolean requestFocus) {
    if (!(object instanceof PsiElement)) return;
    /*PsiElement element = (PsiElement)object;
    PsiFile psiFile = element.getContainingFile();
    if (psiFile != null) {
      element = psiFile;
    }

    if (element instanceof PsiJavaFile) {
      final PsiClass[] classes = ((PsiJavaFile)element).getClasses();
      if (classes.length > 0) {
        element = classes[0];
      }
    }

    final PsiElement originalElement = element.getOriginalElement();*/
    final VirtualFile virtualFile = PsiUtilBase.getVirtualFile((PsiElement)object);
    final String list = FavoritesViewSelectInTarget.findSuitableFavoritesList(virtualFile, myProject, getSubId());
    if (list == null) return;
    if (!list.equals(getSubId())) {
      ProjectView.getInstance(myProject).changeView(ID, list);
    }
    myViewPanel.selectElement(object, virtualFile, requestFocus);
  }

  @Override
  public int getWeight() {
    return 4;
  }

  @NotNull
  @Override
  public SelectInTarget createSelectInTarget() {
    return new FavoritesViewSelectInTarget(myProject);
  }
}
