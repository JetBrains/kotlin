// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.BuildParticipant;
import org.jetbrains.plugins.gradle.settings.CompositeDefinitionSource;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.util.io.FileUtil.pathsEqual;

/**
 * @author Vladislav.Soroka
 */
public class GradleProjectCompositeSelectorDialog extends DialogWrapper {

  private static final int MAX_PATH_LENGTH = 40;
  @NotNull
  private final Project myProject;
  @Nullable
  private final GradleProjectSettings myCompositeRootSettings;
  private JPanel mainPanel;
  private JPanel contentPanel;
  @SuppressWarnings("unused")
  private JBLabel myDescriptionLbl;
  private final ExternalSystemUiAware myExternalSystemUiAware;
  private final CheckboxTree myTree;

  public GradleProjectCompositeSelectorDialog(@NotNull Project project, String compositeRootProjectPath) {
    super(project, true);
    myProject = project;
    myCompositeRootSettings = GradleSettings.getInstance(myProject).getLinkedProjectSettings(compositeRootProjectPath);
    myExternalSystemUiAware = ExternalSystemUiUtil.getUiAware(GradleConstants.SYSTEM_ID);
    myTree = createTree();

    setTitle("Composite Build Configuration");
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTree).
      addExtraAction(new SelectAllButton()).
      addExtraAction(new UnselectAllButton()).
      setToolbarPosition(ActionToolbarPosition.BOTTOM).
      setToolbarBorder(JBUI.Borders.empty());
    contentPanel.add(decorator.createPanel());
    return mainPanel;
  }

  @Override
  protected void doOKAction() {
    if (myCompositeRootSettings != null) {
      Pair[] compositeParticipants = myTree.getCheckedNodes(Pair.class, null);
      if (compositeParticipants.length == 0) {
        myCompositeRootSettings.setCompositeBuild(null);
      }
      else {
        GradleProjectSettings.CompositeBuild compositeBuild = new GradleProjectSettings.CompositeBuild();
        compositeBuild.setCompositeDefinitionSource(CompositeDefinitionSource.IDE);
        for (Pair participant : compositeParticipants) {
          BuildParticipant buildParticipant = new BuildParticipant();
          buildParticipant.setRootProjectName(participant.first.toString());
          buildParticipant.setRootPath(participant.second.toString());
          compositeBuild.getCompositeParticipants().add(buildParticipant);
        }
        myCompositeRootSettings.setCompositeBuild(compositeBuild);
      }
    }
    super.doOKAction();
  }

  @Override
  public void doCancelAction() {
    super.doCancelAction();
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  private CheckboxTree createTree() {
    final CheckedTreeNode root = new CheckedTreeNode();
    if (myCompositeRootSettings != null) {
      List<CheckedTreeNode> nodes = new ArrayList<>();
      for (GradleProjectSettings projectSettings : GradleSettings.getInstance(myProject).getLinkedProjectsSettings()) {
        if (projectSettings == myCompositeRootSettings) continue;
        if (projectSettings.getCompositeBuild() != null &&
            projectSettings.getCompositeBuild().getCompositeDefinitionSource() == CompositeDefinitionSource.SCRIPT) {
          continue;
        }

        GradleProjectSettings.CompositeBuild compositeBuild = myCompositeRootSettings.getCompositeBuild();
        boolean added = compositeBuild != null && compositeBuild.getCompositeParticipants().stream()
          .anyMatch(participant -> pathsEqual(participant.getRootPath(), projectSettings.getExternalProjectPath()));

        String representationName = myExternalSystemUiAware.getProjectRepresentationName(
          projectSettings.getExternalProjectPath(), projectSettings.getExternalProjectPath());
        CheckedTreeNode treeNode = new CheckedTreeNode(Pair.create(representationName, projectSettings.getExternalProjectPath()));
        treeNode.setChecked(added);
        nodes.add(treeNode);
      }

      ContainerUtil.sort(nodes, (o1, o2) ->
        StringUtil.naturalCompare((String)((Pair)o1.getUserObject()).first, (String)((Pair)o2.getUserObject()).first));
      TreeUtil.addChildrenTo(root, nodes);
    }

    final CheckboxTree tree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer(true, false) {

      @Override
      public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (!(value instanceof CheckedTreeNode)) return;
        CheckedTreeNode node = (CheckedTreeNode)value;

        if (!(node.getUserObject() instanceof Pair)) return;
        Pair pair = (Pair)node.getUserObject();

        ColoredTreeCellRenderer renderer = getTextRenderer();
        renderer.setIcon(myExternalSystemUiAware.getProjectIcon());
        String projectName = (String)pair.first;
        renderer.append(projectName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        String projectPath = StringUtil.trimMiddle((String)pair.second, MAX_PATH_LENGTH);
        renderer.append(" (" + projectPath + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
        setToolTipText((String)pair.second);
      }
    }, root);

    TreeUtil.expand(tree, 1);
    return tree;
  }

  private void walkTree(Consumer<? super CheckedTreeNode> consumer) {
    final TreeModel treeModel = myTree.getModel();
    final Object root = treeModel.getRoot();
    if (!(root instanceof CheckedTreeNode)) return;

    for (TreeNode node : TreeUtil.listChildren((CheckedTreeNode)root)) {
      if (!(node instanceof CheckedTreeNode)) continue;
      consumer.consume(((CheckedTreeNode)node));
    }
  }

  private class SelectAllButton extends AnActionButton {
    SelectAllButton() {
      super("Select All", AllIcons.Actions.Selectall);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      walkTree(node -> node.setChecked(true));
      ((DefaultTreeModel)myTree.getModel()).reload();
    }
  }

  private class UnselectAllButton extends AnActionButton {
    UnselectAllButton() {
      super("Unselect All", AllIcons.Actions.Unselectall);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      walkTree(node -> node.setChecked(false));
      ((DefaultTreeModel)myTree.getModel()).reload();
    }
  }
}
