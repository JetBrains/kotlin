// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.impl.ProjectPaneSelectInTarget;
import com.intellij.ide.projectView.BaseProjectTreeBuilder;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewSettings;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.*;
import com.intellij.ide.scratch.ScratchProjectViewPane;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;
import com.intellij.psi.PsiDirectory;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;

public class ProjectViewPane extends AbstractProjectViewPSIPane {
  @NonNls public static final String ID = "ProjectPane";
  private static final String SHOW_EXCLUDED_FILES_OPTION = "show-excluded-files";
  private static final String USE_FILE_NESTING_RULES = "use-file-nesting-rules";

  boolean myShowExcludedFiles = true;
  private boolean myUseFileNestingRules = true;

  public ProjectViewPane(Project project) {
    super(project);
  }

  @NotNull
  @Override
  public String getTitle() {
    return IdeBundle.message("title.project");
  }

  @Override
  @NotNull
  public String getId() {
    return ID;
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.General.ProjectTab;
  }


  @NotNull
  @Override
  public SelectInTarget createSelectInTarget() {
    return new ProjectPaneSelectInTarget(myProject);
  }

  @NotNull
  @Override
  protected AbstractTreeUpdater createTreeUpdater(@NotNull AbstractTreeBuilder treeBuilder) {
    return new ProjectViewTreeUpdater(treeBuilder);
  }

  @NotNull
  @Override
  protected ProjectAbstractTreeStructureBase createStructure() {
    return new ProjectViewPaneTreeStructure();
  }

  @NotNull
  @Override
  protected ProjectViewTree createTree(@NotNull DefaultTreeModel treeModel) {
    return new ProjectViewTree(treeModel) {
      @Override
      public String toString() {
        return getTitle() + " " + super.toString();
      }

      @Override
      public void setFont(Font font) {
        if (Registry.is("bigger.font.in.project.view")) {
          font = font.deriveFont(font.getSize() + 1.0f);
        }
        super.setFont(font);
      }
    };
  }

  @NotNull
  public String getComponentName() {
    return "ProjectPane";
  }

  @Override
  public void readExternal(@NotNull Element element) {
    super.readExternal(element);

    String showExcludedOption = JDOMExternalizerUtil.readField(element, SHOW_EXCLUDED_FILES_OPTION);
    myShowExcludedFiles = showExcludedOption == null || Boolean.parseBoolean(showExcludedOption);
    String useFileNestingRules = JDOMExternalizerUtil.readField(element, USE_FILE_NESTING_RULES);
    myUseFileNestingRules = useFileNestingRules == null || Boolean.parseBoolean(useFileNestingRules);
  }

  @Override
  public void writeExternal(Element element) {
    super.writeExternal(element);
    if (!myUseFileNestingRules) {
      JDOMExternalizerUtil.writeField(element, USE_FILE_NESTING_RULES, String.valueOf(false));
    }
  }

  @Override
  public void addToolbarActions(@NotNull DefaultActionGroup actionGroup) {
    //if there is a single content root in the project containing all other content roots (it's a rather common case) there will be no
    // special module nodes so it's better to hide 'Flatten Modules' action to avoid confusion
    actionGroup.addAction(createFlattenModulesAction(this::hasSeveralTopLevelModuleNodes)).setAsSecondary(true);

    actionGroup.addAction(new ConfigureFilesNestingAction()).setAsSecondary(true);
    AnAction editScopesAction = ActionManager.getInstance().getAction("ScopeView.EditScopes");
    if (editScopesAction != null) actionGroup.addAction(editScopesAction).setAsSecondary(true);
  }

  /**
   * @return {@code true} if 'Project View' have more than one top-level module node or have top-level module group nodes
   */
  private boolean hasSeveralTopLevelModuleNodes() {
    TreeModel treeModel = myTree.getModel();
    Object root = treeModel.getRoot();
    int count = treeModel.getChildCount(root);
    if (count <= 1) return false;
    int moduleNodes = 0;
    for (int i = 0; i < count; i++) {
      Object child = treeModel.getChild(root, i);
      if (child instanceof DefaultMutableTreeNode) {
        Object node = ((DefaultMutableTreeNode)child).getUserObject();
        if (node instanceof ProjectViewModuleNode || node instanceof PsiDirectoryNode) {
          moduleNodes++;
          if (moduleNodes > 1) {
            return true;
          }
        }
        else if (node instanceof ModuleGroupNode) {
          return true;
        }
      }
    }
    return false;
  }

  boolean isUseFileNestingRules() {
    return myUseFileNestingRules;
  }

  // should be first
  @Override
  public int getWeight() {
    return 0;
  }

  private final class ProjectViewTreeUpdater extends AbstractTreeUpdater {
    private ProjectViewTreeUpdater(final AbstractTreeBuilder treeBuilder) {
      super(treeBuilder);
    }

    @Override
    public boolean addSubtreeToUpdateByElement(@NotNull Object element) {
      if (element instanceof PsiDirectory && !myProject.isDisposed()) {
        final PsiDirectory dir = (PsiDirectory)element;
        final ProjectTreeStructure treeStructure = (ProjectTreeStructure)myTreeStructure;
        PsiDirectory dirToUpdateFrom = dir;

        // optimization
        // isEmptyMiddleDirectory can be slow when project VFS is not fully loaded (initial dumb mode).
        // It's easiest to disable the optimization in any dumb mode
        if (!treeStructure.isFlattenPackages() && treeStructure.isHideEmptyMiddlePackages() && !DumbService.isDumb(myProject)) {
          while (dirToUpdateFrom != null && ProjectViewDirectoryHelper.getInstance(myProject).isEmptyMiddleDirectory(dirToUpdateFrom, true)) {
            dirToUpdateFrom = dirToUpdateFrom.getParentDirectory();
          }
        }
        boolean addedOk;
        while (!(addedOk = super.addSubtreeToUpdateByElement(dirToUpdateFrom == null? myTreeStructure.getRootElement() : dirToUpdateFrom))) {
          if (dirToUpdateFrom == null) {
            break;
          }
          dirToUpdateFrom = dirToUpdateFrom.getParentDirectory();
        }
        return addedOk;
      }

      return super.addSubtreeToUpdateByElement(element);
    }
  }

  private class ProjectViewPaneTreeStructure extends ProjectTreeStructure implements ProjectViewSettings {
    ProjectViewPaneTreeStructure() {
      super(ProjectViewPane.this.myProject, ID);
    }

    @Override
    protected AbstractTreeNode createRoot(@NotNull final Project project, @NotNull ViewSettings settings) {
      return new ProjectViewProjectNode(project, settings);
    }

    @Override
    public boolean isShowExcludedFiles() {
      return ProjectView.getInstance(myProject).isShowExcludedFiles(ID);
    }

    @Override
    public boolean isUseFileNestingRules() {
      return myUseFileNestingRules;
    }

    @Override
    public boolean isToBuildChildrenInBackground(@NotNull Object element) {
      return Registry.is("ide.projectView.ProjectViewPaneTreeStructure.BuildChildrenInBackground");
    }
  }

  private class ConfigureFilesNestingAction extends DumbAwareAction {
    private ConfigureFilesNestingAction() {
      super(IdeBundle.message("action.file.nesting.in.project.view"));
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      final ProjectView projectView = ProjectView.getInstance(myProject);
      e.getPresentation().setEnabledAndVisible(projectView.getCurrentProjectViewPane() == ProjectViewPane.this);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      final FileNestingInProjectViewDialog dialog = new FileNestingInProjectViewDialog(myProject);
      dialog.reset(myUseFileNestingRules);
      dialog.show();
      if (dialog.isOK()) {
        dialog.apply(useFileNestingRules -> myUseFileNestingRules = useFileNestingRules);
        updateFromRoot(true);
      }
    }
  }

  @Override
  protected BaseProjectTreeBuilder createBuilder(@NotNull DefaultTreeModel model) {
    return null;
  }

  public static boolean canBeSelectedInProjectView(@NotNull Project project, @NotNull VirtualFile file) {
    final VirtualFile archiveFile;

    if(file.getFileSystem() instanceof ArchiveFileSystem)
      archiveFile = ((ArchiveFileSystem)file.getFileSystem()).getLocalByEntry(file);
    else
      archiveFile = null;

    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    return (archiveFile != null && index.getContentRootForFile(archiveFile, false) != null) ||
           index.getContentRootForFile(file, false) != null ||
           index.isInLibrary(file) ||
           Comparing.equal(file.getParent(), project.getBaseDir()) ||
           ScratchProjectViewPane.isScratchesMergedIntoProjectTab() && ScratchUtil.isScratch(file);
  }
}
