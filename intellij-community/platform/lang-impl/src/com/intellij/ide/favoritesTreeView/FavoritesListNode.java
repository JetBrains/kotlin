// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.AbstractUrl;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.TreeItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesListNode extends AbstractTreeNode<String> {
  private static final Logger LOGGER = Logger.getInstance(FavoritesListNode.class);
  private final Project myProject;
  private final String myDescription;

  public FavoritesListNode(Project project, @NotNull String listName, String description) {
    super(project, listName);
    myProject = project;
    myName = listName;
    myDescription = description;
  }

  public FavoritesListNode(Project project, @NotNull String listName) {
    this(project, listName, null);
  }

  public FavoritesListProvider getProvider() {
    return null;
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    return getFavoritesRoots(myProject, myName, this);
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setIcon(AllIcons.Toolwindows.ToolWindowFavorites);
    presentation.setPresentableText(myName);
    presentation.setLocationString(myDescription);
  }

  @NotNull
  public static Collection<AbstractTreeNode<?>> getFavoritesRoots(Project project, String listName, final FavoritesListNode listNode) {
    Collection<TreeItem<Pair<AbstractUrl, String>>> pairs = FavoritesManager.getInstance(project).getFavoritesListRootUrls(listName);
    if (pairs.isEmpty()) {
      return Collections.emptyList();
    }
    return createFavoriteRoots(project, pairs, listNode);
  }

  @NotNull
  private static Collection<AbstractTreeNode<?>> createFavoriteRoots(Project project,
                                                                     @NotNull Collection<? extends TreeItem<Pair<AbstractUrl, String>>> urls,
                                                                     AbstractTreeNode<?> me) {
    Collection<AbstractTreeNode<?>> result = new ArrayList<>();
    processUrls(project, urls, result, me);
    return result;
  }

  private static void processUrls(Project project,
                                  Collection<? extends TreeItem<Pair<AbstractUrl, String>>> urls,
                                  Collection<? super AbstractTreeNode<?>> result, AbstractTreeNode<?> me) {
    for (TreeItem<Pair<AbstractUrl, String>> pair : urls) {
      AbstractUrl abstractUrl = pair.getData().getFirst();
      final Object[] path = abstractUrl.createPath(project);
      if (path == null || path.length < 1 || path[0] == null) {
        continue;
      }
      try {
        ClassLoader loader;
        if (abstractUrl instanceof AbstractUrlFavoriteAdapter) {
          FavoriteNodeProvider provider = ((AbstractUrlFavoriteAdapter)abstractUrl).getNodeProvider();
          loader = provider.getClass().getClassLoader();
        } else {
          loader = FavoritesListNode.class.getClassLoader();
        }
        String className = pair.getData().getSecond();
        @SuppressWarnings("unchecked")
        Class<? extends AbstractTreeNode<?>> nodeClass = (Class<? extends AbstractTreeNode<?>>)loader.loadClass(className);

        AbstractTreeNode<?> node = ProjectViewNode
          .createTreeNode(nodeClass, project, path[path.length - 1], FavoritesManager.getInstance(project).getViewSettings());
        node.setParent(me);
        node.setIndex(result.size());
        result.add(node);

        if (node instanceof ProjectViewNodeWithChildrenList) {
          final List<TreeItem<Pair<AbstractUrl, String>>> children = pair.getChildren();
          if (children != null && !children.isEmpty()) {
            Collection<AbstractTreeNode<?>> childList = new ArrayList<>();
            processUrls(project, children, childList, node);
            for (AbstractTreeNode<?> treeNode : childList) {
              ((ProjectViewNodeWithChildrenList<?>)node).addChild(treeNode);
            }
          }
        }
      }
      catch (Exception e) {
        LOGGER.error(e);
      }
    }
  }
}
