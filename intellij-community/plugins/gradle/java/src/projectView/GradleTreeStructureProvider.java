// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.ide.projectView.impl.nodes.*;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleGrouper;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collection;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.*;

/**
 * @author Vladislav.Soroka
 */
public class GradleTreeStructureProvider implements TreeStructureProvider, DumbAware {
  @NotNull
  @Override
  public Collection<AbstractTreeNode> modify(@NotNull AbstractTreeNode parent,
                                             @NotNull Collection<AbstractTreeNode> children,
                                             ViewSettings settings) {
    Project project = parent.getProject();
    if (project == null) return children;

    if (parent instanceof ProjectViewProjectNode) {
      return getProjectNodeChildren(project, children);
    }

    if (parent instanceof ProjectViewModuleGroupNode) {
      Collection<AbstractTreeNode> modifiedChildren = new SmartList<>();
      for (AbstractTreeNode child : children) {
        if (child instanceof ProjectViewModuleNode) {
          Module module = ((ProjectViewModuleNode)child).getValue();
          if (!showUnderModuleGroup(module)) continue;

          GradleProjectViewModuleNode sourceSetNode = getGradleModuleNode(project, (ProjectViewModuleNode)child, settings);
          child = sourceSetNode != null ? sourceSetNode : child;
        }
        else if (child instanceof PsiDirectoryNode) {
          GradleModuleDirectoryNode sourceSetNode = getGradleModuleNode(project, (PsiDirectoryNode)child, settings);
          if (sourceSetNode != null && !showUnderModuleGroup(sourceSetNode.myModule)) continue;
          child = sourceSetNode != null ? sourceSetNode : child;
        }
        modifiedChildren.add(child);
      }
      return modifiedChildren;
    }

    if (parent instanceof GradleProjectViewModuleNode) {
      Module module = ((GradleProjectViewModuleNode)parent).getValue();
      String projectPath = getExternalProjectPath(module);
      Collection<AbstractTreeNode> modifiedChildren = new SmartList<>();
      for (AbstractTreeNode child : children) {
        if (child instanceof PsiDirectoryNode) {
          PsiDirectory psiDirectory = ((PsiDirectoryNode)child).getValue();
          if (psiDirectory != null) {
            final VirtualFile virtualFile = psiDirectory.getVirtualFile();
            if (projectPath != null && FileUtil.isAncestor(projectPath, virtualFile.getPath(), false)) {
              continue;
            }
          }
        }
        modifiedChildren.add(child);
      }
      return modifiedChildren;
    }

    if (parent instanceof PsiDirectoryNode) {
      Collection<AbstractTreeNode> modifiedChildren = new SmartList<>();
      for (AbstractTreeNode child : children) {
        if (child instanceof PsiDirectoryNode) {
          GradleModuleDirectoryNode sourceSetNode = getGradleModuleNode(project, (PsiDirectoryNode)child, settings);
          child = sourceSetNode != null ? sourceSetNode : child;
        }
        modifiedChildren.add(child);
      }
      return modifiedChildren;
    }
    return children;
  }

  private static boolean showUnderModuleGroup(Module module) {
    if (isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) {
      String projectPath = getExternalProjectPath(module);
      for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
        if (projectPath != null && !FileUtil.isAncestor(projectPath, root.getPath(), true)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  @NotNull
  private static Collection<AbstractTreeNode> getProjectNodeChildren(@NotNull Project project,
                                                                     @NotNull Collection<AbstractTreeNode> children) {
    Collection<AbstractTreeNode> modifiedChildren = new SmartList<>();
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    for (AbstractTreeNode child : children) {
      Pair<VirtualFile, PsiDirectoryNode> parentNodePair = null;
      if (child instanceof ProjectViewModuleGroupNode) {
        final ProjectViewModuleGroupNode groupNode = (ProjectViewModuleGroupNode)child;
        final Collection<AbstractTreeNode> groupNodeChildren = groupNode.getChildren();
        for (final AbstractTreeNode node : groupNodeChildren) {
          if (node instanceof PsiDirectoryNode) {
            final PsiDirectoryNode psiDirectoryNode = (PsiDirectoryNode)node;
            final PsiDirectory psiDirectory = psiDirectoryNode.getValue();
            if (psiDirectory == null) {
              parentNodePair = null;
              break;
            }

            final VirtualFile virtualFile = psiDirectory.getVirtualFile();
            final Module module = fileIndex.getModuleForFile(virtualFile);
            if (!isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) {
              parentNodePair = null;
              break;
            }

            if (parentNodePair == null || VfsUtilCore.isAncestor(virtualFile, parentNodePair.first, false)) {
              parentNodePair = Pair.pair(virtualFile, psiDirectoryNode);
            }
            else if (!VfsUtilCore.isAncestor(parentNodePair.first, virtualFile, false)) {
              parentNodePair = null;
              break;
            }
          }
          else {
            parentNodePair = null;
            break;
          }
        }
      }
      else if (child instanceof PsiDirectoryNode && child.getParent() == null) {
        PsiDirectory psiDirectory = ((PsiDirectoryNode)child).getValue();
        if (psiDirectory != null) {
          VirtualFile directoryFile = psiDirectory.getVirtualFile();
          GradleModuleDirectoryNode gradleModuleNode = getGradleModuleNode(project, (PsiDirectoryNode)child,
                                                                           ((PsiDirectoryNode)child).getSettings());
          if (gradleModuleNode != null) {
            parentNodePair = Pair.pair(directoryFile, gradleModuleNode);
          }
        }
      }
      modifiedChildren.add(parentNodePair != null ? parentNodePair.second : child);
    }
    return modifiedChildren;
  }


  @Nullable
  private static GradleProjectViewModuleNode getGradleModuleNode(@NotNull Project project,
                                                                 @NotNull ProjectViewModuleNode moduleNode,
                                                                 ViewSettings settings) {
    Module module = moduleNode.getValue();
    final String moduleShortName = getGradleModuleShortName(module);
    if (moduleShortName == null) return null;
    return new GradleProjectViewModuleNode(project, module, settings, moduleShortName);
  }

  @Nullable
  private static GradleModuleDirectoryNode getGradleModuleNode(@NotNull Project project,
                                                               @NotNull PsiDirectoryNode directoryNode,
                                                               ViewSettings settings) {

    PsiDirectory psiDirectory = directoryNode.getValue();
    if (psiDirectory == null) return null;

    final VirtualFile virtualFile = psiDirectory.getVirtualFile();
    if (!ProjectRootsUtil.isModuleContentRoot(virtualFile, project)) return null;

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final Module module = fileIndex.getModuleForFile(virtualFile);
    final String moduleShortName = getGradleModuleShortName(module);
    if (moduleShortName == null) return null;
    return new GradleModuleDirectoryNode(project, psiDirectory, settings, module, moduleShortName, directoryNode.getFilter());
  }

  @Nullable
  private static String getGradleModuleShortName(Module module) {
    if (!isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return null;

    String moduleShortName;
    if (GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY.equals(getExternalModuleType(module))) {
      return GradleProjectResolverUtil.getSourceSetName(module);
    }
    else {
      moduleShortName = getExternalProjectId(module);
    }

    boolean isRootModule = StringUtil.equals(getExternalProjectPath(module), getExternalRootProjectPath(module));
    if(isRootModule || moduleShortName == null) return moduleShortName;

    moduleShortName = ModuleGrouper.instanceFor(module.getProject()).getShortenedNameByFullModuleName(moduleShortName);
    return StringUtil.getShortName(moduleShortName, ':');
  }


  private static class GradleModuleDirectoryNode extends PsiDirectoryNode {
    private final String myModuleShortName;
    private final Module myModule;

    GradleModuleDirectoryNode(Project project,
                              @NotNull PsiDirectory psiDirectory,
                                     ViewSettings settings,
                                     Module module,
                                     String moduleShortName,
                                     PsiFileSystemItemFilter filter) {
      super(project, psiDirectory, settings, filter);
      myModuleShortName = moduleShortName;
      myModule = module;
    }

    @Override
    protected boolean shouldShowModuleName() {
      return false;
    }

    @Override
    protected void updateImpl(@NotNull PresentationData data) {
      super.updateImpl(data);
      PsiDirectory psiDirectory = getValue();
      assert psiDirectory != null;
      VirtualFile directoryFile = psiDirectory.getVirtualFile();
      if (StringUtil.isNotEmpty(myModuleShortName) &&
          !StringUtil.equalsIgnoreCase(myModuleShortName.replace("-", ""), directoryFile.getName().replace("-", ""))) {
        data.addText("[" + myModuleShortName + "]", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      }
    }
  }

  private static class GradleProjectViewModuleNode extends ProjectViewModuleNode {
    @NotNull
    private final String myModuleShortName;

    GradleProjectViewModuleNode(Project project, Module value, ViewSettings viewSettings, @NotNull String moduleShortName) {
      super(project, value, viewSettings);
      myModuleShortName = moduleShortName;
    }

    @Override
    public void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      presentation.setPresentableText(myModuleShortName);
      presentation.addText(myModuleShortName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }

    @Override
    protected boolean showModuleNameInBold() {
      return false;
    }
  }
}
