// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.*;
import com.intellij.ide.projectView.impl.ProjectViewFileNestingService.NestingRule;
import com.intellij.ide.projectView.impl.nodes.NestingTreeNode;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * {@code NestingTreeStructureProvider} moves some files in the Project View to be shown as children of another peer file. Standard use
 * case is to improve folder contents presentation when it contains both source file and its compiled output. For example generated
 * {@code foo.min.js} file will be shown as a child of {@code foo.js} file.<br/>
 * Nesting logic is based on file names only. Rules about files that should be nested are provided by
 * {@code com.intellij.projectViewNestingRulesProvider} extensions.
 *
 * @see ProjectViewNestingRulesProvider
 * @see ProjectViewFileNestingService
 * @see FileNestingInProjectViewDialog
 */
public class NestingTreeStructureProvider implements TreeStructureProvider, DumbAware {
  private static final Logger LOG = Logger.getInstance(NestingTreeStructureProvider.class);

  @NotNull
  @Override
  public Collection<AbstractTreeNode> modify(@NotNull final AbstractTreeNode parent,
                                             @NotNull final Collection<AbstractTreeNode> children,
                                             final ViewSettings settings) {
    if (!(settings instanceof ProjectViewSettings) || !((ProjectViewSettings)settings).isUseFileNestingRules()) return children;
    if (!(parent instanceof PsiDirectoryNode)) return children;

    final ArrayList<PsiFileNode> childNodes = new ArrayList<>();
    for (AbstractTreeNode node : children) {
      if (!(node instanceof PsiFileNode)) continue;
      childNodes.add((PsiFileNode)node);
    }

    Function<PsiFileNode, String> fileNameFunc = psiFileNode -> {
      final PsiFile file = psiFileNode.getValue();
      if (file == null) return null;
      return file.getName();
    };
    FileNestingBuilder fileNestingBuilder = FileNestingBuilder.getInstance();
    final MultiMap<PsiFileNode, PsiFileNode> parentToChildren = fileNestingBuilder.mapParentToChildren(childNodes, fileNameFunc);
    if (parentToChildren.isEmpty()) return children;

    // initial ArrayList size may be not exact, not a big problem
    final Collection<AbstractTreeNode> newChildren = new ArrayList<>(children.size() - parentToChildren.size());

    final Set<PsiFileNode> childrenToMoveDown = new THashSet<>(parentToChildren.values());

    for (AbstractTreeNode node : children) {
      if (!(node instanceof PsiFileNode)) {
        newChildren.add(node);
        continue;
      }

      if (childrenToMoveDown.contains(node)) {
        continue;
      }

      final Collection<PsiFileNode> childrenOfThisFile = parentToChildren.get((PsiFileNode)node);
      if (childrenOfThisFile.isEmpty()) {
        newChildren.add(node);
        continue;
      }

      newChildren.add(new NestingTreeNode((PsiFileNode)node, childrenOfThisFile));
    }

    return newChildren;
  }

  // Algorithm is similar to calcParentToChildren(), but a bit simpler, because we have one specific parentFile.
  public static Collection<ChildFileInfo> getFilesShownAsChildrenInProjectView(@NotNull final Project project,
                                                                               @NotNull final VirtualFile parentFile) {
    LOG.assertTrue(!parentFile.isDirectory());

    final AbstractProjectViewPane pane = ProjectView.getInstance(project).getProjectViewPaneById(ProjectViewPane.ID);
    if (pane instanceof ProjectViewPane) {
      if (!((ProjectViewPane)pane).isUseFileNestingRules()) return Collections.emptyList();
    }

    final VirtualFile dir = parentFile.getParent();
    if (dir == null) return Collections.emptyList();

    final Collection<NestingRule> rules = FileNestingBuilder.getInstance().getNestingRules();
    if (rules.isEmpty()) return Collections.emptyList();

    final VirtualFile[] children = dir.getChildren();
    if (children.length <= 1) return Collections.emptyList();

    final Collection<NestingRule> rulesWhereItCanBeParent = filterRules(rules, parentFile.getName(), true);
    if (rulesWhereItCanBeParent.isEmpty()) return Collections.emptyList();

    final Collection<NestingRule> rulesWhereItCanBeChild = filterRules(rules, parentFile.getName(), false);

    final SmartList<ChildFileInfo> result = new SmartList<>();

    for (VirtualFile child : children) {
      if (child.isDirectory()) continue;
      if (child.equals(parentFile)) continue;

      // if given parentFile itself appears to be a child of some other file, it means that it is not shown as parent node in Project View
      for (NestingRule rule : rulesWhereItCanBeChild) {
        final String childName = child.getName();

        final Couple<Boolean> c = FileNestingBuilder.checkMatchingAsParentOrChild(rule, childName);
        final boolean matchesParent = c.first;

        if (matchesParent) {
          final String baseName = childName.substring(0, childName.length() - rule.getParentFileSuffix().length());
          if (parentFile.getName().equals(baseName + rule.getChildFileSuffix())) {
            return Collections.emptyList(); // parentFile itself appears to be a child of childFile
          }
        }
      }

      for (NestingRule rule : rulesWhereItCanBeParent) {
        final String childName = child.getName();

        final Couple<Boolean> c = FileNestingBuilder.checkMatchingAsParentOrChild(rule, childName);
        final boolean matchesChild = c.second;

        if (matchesChild) {
          final String baseName = childName.substring(0, childName.length() - rule.getChildFileSuffix().length());
          if (parentFile.getName().equals(baseName + rule.getParentFileSuffix())) {
            result.add(new ChildFileInfo(child, baseName));
          }
        }
      }
    }

    return result;
  }

  /**
   * @return only those rules where given {@code fileName} can potentially be a parent (if {@code parentNotChild} is {@code true})
   * or only those rules where given {@code fileName} can potentially be a child (if {@code parentNotChild} is {@code false})
   */
  @NotNull
  private static Collection<NestingRule> filterRules(@NotNull final Collection<NestingRule> rules,
                                                     @NotNull final String fileName,
                                                     final boolean parentNotChild) {
    final SmartList<NestingRule> result = new SmartList<>();
    for (NestingRule rule : rules) {
      final Couple<Boolean> c = FileNestingBuilder.checkMatchingAsParentOrChild(rule, fileName);
      final boolean matchesParent = c.first;
      final boolean matchesChild = c.second;

      if (!matchesChild && !matchesParent) continue;

      if (matchesParent && parentNotChild) {
        result.add(rule);
      }

      if (matchesChild && !parentNotChild) {
        result.add(rule);
      }
    }

    return result;
  }

  public static class ChildFileInfo {
    @NotNull public final VirtualFile file;
    @NotNull public final String namePartCommonWithParentFile;

    public ChildFileInfo(@NotNull final VirtualFile file, @NotNull final String namePartCommonWithParentFile) {
      this.file = file;
      this.namePartCommonWithParentFile = namePartCommonWithParentFile;
    }
  }
}
