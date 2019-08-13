// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ui.actions.suppress;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.SuppressIntentionAction;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.codeInspection.ui.InspectionTreeNode;
import com.intellij.codeInspection.ui.SuppressableInspectionTreeNode;
import com.intellij.codeInspection.ui.actions.KeyAwareInspectionViewAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SequentialModalProgressTask;
import com.intellij.util.containers.ContainerUtil;
import java.util.HashSet;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import static com.intellij.codeInspection.ui.actions.InspectionViewActionBase.getView;

public class SuppressActionWrapper extends ActionGroup implements CompactActionGroup {
  private final static Logger LOG = Logger.getInstance(SuppressActionWrapper.class);

  public SuppressActionWrapper() {
    super(InspectionsBundle.message("suppress.inspection.problem"), false);
  }

  @Override
  @NotNull
  public AnAction[] getChildren(@Nullable final AnActionEvent e) {
    final InspectionResultsView view = getView(e);
    if (view == null) return AnAction.EMPTY_ARRAY;
    final InspectionToolWrapper wrapper = view.getTree().getSelectedToolWrapper(true);
    if (wrapper == null) return AnAction.EMPTY_ARRAY;
    final Set<SuppressIntentionAction> suppressActions = view.getSuppressActionHolder().getSuppressActions(wrapper);

    if (suppressActions.isEmpty()) return AnAction.EMPTY_ARRAY;
    final AnAction[] actions = new AnAction[suppressActions.size() + 1];

    int i = 0;
    for (SuppressIntentionAction action : suppressActions) {
      actions[i++] = new SuppressTreeAction(action);
    }
    actions[suppressActions.size()] = Separator.getInstance();
    Arrays.sort(actions, Comparator.comparingInt(a -> a instanceof Separator ? 0 : ((SuppressTreeAction)a).isSuppressAll() ? 1 : -1));
    return actions;
  }

  public static class SuppressTreeAction extends KeyAwareInspectionViewAction {
    private final SuppressIntentionAction mySuppressAction;

    public SuppressTreeAction(final SuppressIntentionAction suppressAction) {
      super(suppressAction.getText());
      mySuppressAction = suppressAction;
    }

    @Override
    protected void actionPerformed(@NotNull InspectionResultsView view, @NotNull HighlightDisplayKey key) {
      ApplicationManager.getApplication().invokeLater(() -> {
        Project project = view.getProject();
        final String templatePresentationText = getTemplatePresentation().getText();
        LOG.assertTrue(templatePresentationText != null);
        final InspectionToolWrapper wrapper = view.getTree().getSelectedToolWrapper(true);
        LOG.assertTrue(wrapper != null);
        final Set<SuppressableInspectionTreeNode> nodesAsSet = getNodesToSuppress(view);
        final SuppressableInspectionTreeNode[] nodes = nodesAsSet.toArray(new SuppressableInspectionTreeNode[0]);
        CommandProcessor.getInstance().executeCommand(project, () -> {
          CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
          final SequentialModalProgressTask progressTask =
            new SequentialModalProgressTask(project, templatePresentationText, true);
          progressTask.setMinIterationTime(200);
          progressTask.setTask(new SuppressActionSequentialTask(nodes, mySuppressAction, wrapper));
          ProgressManager.getInstance().run(progressTask);
        }, templatePresentationText, null);

        final Set<GlobalInspectionContextImpl> globalInspectionContexts =
          ((InspectionManagerEx)InspectionManager.getInstance(project)).getRunningContexts();
        for (GlobalInspectionContextImpl context : globalInspectionContexts) {
          context.refreshViews();
        }
        view.syncRightPanel();
      });
    }

    @Override
    protected boolean isEnabled(@NotNull InspectionResultsView view, AnActionEvent e) {
      final Set<SuppressableInspectionTreeNode> nodesToSuppress = getNodesToSuppress(view);
      if (nodesToSuppress.isEmpty()) return false;
      if (nodesToSuppress.size() == 1) {
        final PsiElement element = ObjectUtils.notNull(ContainerUtil.getFirstItem(nodesToSuppress)).getSuppressContent().getFirst();
        String text = mySuppressAction.getFamilyName();
        if (element != null) {
          mySuppressAction.isAvailable(view.getProject(), null, element);
          text = mySuppressAction.getText();
        }
        e.getPresentation().setText(text);
        return true;
      } else {
        e.getPresentation().setText(mySuppressAction.getFamilyName());
        return true;
      }
    }

    public boolean isSuppressAll() {
      return mySuppressAction.isSuppressAll();
    }

    private Set<SuppressableInspectionTreeNode> getNodesToSuppress(@NotNull InspectionResultsView view) {
      final TreePath[] paths = view.getTree().getSelectionPaths();
      if (paths == null) return Collections.emptySet();
      final Set<SuppressableInspectionTreeNode> result = new HashSet<>();
      for (TreePath path : paths) {
        final Object node = path.getLastPathComponent();
        if (!(node instanceof TreeNode)) continue;
        if (!TreeUtil.treeNodeTraverser((TreeNode)node).traverse().processEach(node1 -> {    //fetch leaves
          final InspectionTreeNode n = (InspectionTreeNode)node1;
          if (n instanceof SuppressableInspectionTreeNode &&
              ((SuppressableInspectionTreeNode)n).canSuppress() &&
              n.isValid()) {
            if (((SuppressableInspectionTreeNode)n).getAvailableSuppressActions().contains(mySuppressAction)) {
              result.add((SuppressableInspectionTreeNode)n);
            } else {
              return false;
            }
          }
          return true;
        })) {
          return Collections.emptySet();
        }
      }
      return result;
    }

  }
}
