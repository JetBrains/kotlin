/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.packageDependencies.ui;

import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.ide.scopeView.nodes.BasePsiNode;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleGrouper;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

public class FileTreeModelBuilder {
  private static final Logger LOG = Logger.getInstance(FileTreeModelBuilder.class);

  public static final Key<Integer> FILE_COUNT = Key.create("FILE_COUNT");
  public static final String SCANNING_PACKAGES_MESSAGE = AnalysisScopeBundle.message("package.dependencies.build.progress.text");
  private final ProjectFileIndex myFileIndex;
  private final Project myProject;

  private final boolean myShowModuleGroups;
  private final boolean myShowModules;

  private final boolean myFlattenPackages;
  private final boolean myCompactEmptyMiddlePackages;
  private boolean myShowFiles;
  private final Marker myMarker;
  private final boolean myAddUnmarkedFiles;
  private final PackageDependenciesNode myRoot;
  private final Map<VirtualFile,DirectoryNode> myModuleDirNodes = new HashMap<>();
  private final Map<Module, ModuleNode> myModuleNodes = new HashMap<>();
  private final Map<String, ModuleGroupNode> myModuleGroupNodes = new HashMap<>();
  private final ModuleGrouper myGrouper;
  private GeneralGroupNode myExternalNode;

  private int myScannedFileCount = 0;
  private int myTotalFileCount = 0;
  private int myMarkedFileCount = 0;

  private JTree myTree;
  protected final VirtualFile myBaseDir;
  protected VirtualFile[] myContentRoots;

  public FileTreeModelBuilder(@NotNull Project project, Marker marker, DependenciesPanel.DependencyPanelSettings settings) {
    myProject = project;
    myBaseDir = myProject.getBaseDir();
    myContentRoots = ProjectRootManager.getInstance(myProject).getContentRoots();
    final boolean multiModuleProject = ModuleManager.getInstance(myProject).getModules().length > 1;
    myShowModules = settings.UI_SHOW_MODULES && multiModuleProject;
    myGrouper = ModuleGrouper.instanceFor(project);
    final ProjectViewDirectoryHelper directoryHelper = ProjectViewDirectoryHelper.getInstance(myProject);
    myFlattenPackages = directoryHelper.supportsFlattenPackages() && settings.UI_FLATTEN_PACKAGES;
    myCompactEmptyMiddlePackages = directoryHelper.supportsHideEmptyMiddlePackages() && settings.UI_COMPACT_EMPTY_MIDDLE_PACKAGES;
    myShowFiles = settings.UI_SHOW_FILES;
    myShowModuleGroups = settings.UI_SHOW_MODULE_GROUPS && multiModuleProject;
    myMarker = marker;
    myAddUnmarkedFiles = !settings.UI_FILTER_LEGALS;
    myRoot = new RootNode(myProject);
    myFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
  }

  public void setTree(DnDAwareTree tree) {
    myTree = tree;
  }

  public static synchronized TreeModel createTreeModel(Project project, boolean showProgress, Set<? extends PsiFile> files, Marker marker, DependenciesPanel.DependencyPanelSettings settings) {
    return new FileTreeModelBuilder(project, marker, settings).build(files, showProgress);
  }

  public static synchronized TreeModel createTreeModel(Project project, Marker marker, DependenciesPanel.DependencyPanelSettings settings) {
    return new FileTreeModelBuilder(project, marker, settings).build(project, false);
  }

  public static synchronized TreeModel createTreeModel(Project project, boolean showProgress, Marker marker) {
    return new FileTreeModelBuilder(project, marker, new DependenciesPanel.DependencyPanelSettings()).build(project, showProgress);
  }

  private void countFiles(Project project) {
    final Integer fileCount = project.getUserData(FILE_COUNT);
    if (fileCount == null) {
      myFileIndex.iterateContent(fileOrDir -> {
        if (!fileOrDir.isDirectory()) {
          counting();
        }
        return true;
      });
      project.putUserData(FILE_COUNT, myTotalFileCount);
    } else {
      myTotalFileCount = fileCount.intValue();
    }
  }

  public static void clearCaches(Project project) {
    project.putUserData(FILE_COUNT, null);
  }

  public TreeModel build(final Project project, boolean showProgress) {
    return build(project, showProgress, null);
  }

  public TreeModel build(final Project project, final boolean showProgress, @Nullable final Runnable successRunnable) {
    final Runnable buildingRunnable = () -> {
      ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.setText(SCANNING_PACKAGES_MESSAGE);
        indicator.setIndeterminate(true);
      }
      countFiles(project);
      if (indicator != null) {
        indicator.setIndeterminate(false);
      }
      myFileIndex.iterateContent(new MyContentIterator());
    };
    final TreeModel treeModel = new TreeModel(myRoot);
    if (showProgress) {
      final Task.Backgroundable backgroundable =
        new Task.Backgroundable(project, AnalysisScopeBundle.message("package.dependencies.build.process.title")) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            buildingRunnable.run();
          }

          @Override
          public void onSuccess() {
            if (project.isDisposed()) return;
            myRoot.setSorted(false);
            myRoot.sortChildren();
            treeModel.reload(myRoot);
            if (successRunnable != null) {
              successRunnable.run();
            }
          }
        };
      ProgressManager.getInstance().run(backgroundable);
    }
    else {
      buildingRunnable.run();
    }

    treeModel.setTotalFileCount(myTotalFileCount);
    treeModel.setMarkedFileCount(myMarkedFileCount);
    return treeModel;
  }

  private void counting() {
    myTotalFileCount++;
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      update(indicator, true, -1);
    }
  }

  private static void update(ProgressIndicator indicator, boolean indeterminate, double fraction) {
    if (indicator instanceof PanelProgressIndicator) {
      ((PanelProgressIndicator)indicator).update(SCANNING_PACKAGES_MESSAGE, indeterminate, fraction);
    } else {
      if (fraction != -1) {
        indicator.setFraction(fraction);
      }
    }
  }

  private TreeModel build(final Set<? extends PsiFile> files, boolean showProgress) {
    if (files.size() == 1) {
      myShowFiles = true;
    }

    Runnable buildingRunnable = () -> {
      for (final PsiFile file : files) {
        if (file != null) {
          ReadAction.run(() -> buildFileNode(file.getVirtualFile(), null));
        }
      }
    };

    if (showProgress) {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(buildingRunnable, AnalysisScopeBundle
        .message("package.dependencies.build.process.title"), false, myProject);
    }
    else {
      buildingRunnable.run();
    }

    TreeUtil.sortRecursively(myRoot, new DependencyNodeComparator());
    return new TreeModel(myRoot, myTotalFileCount, myMarkedFileCount);
  }

  private PackageDependenciesNode buildFileNode(VirtualFile file, PackageDependenciesNode lastParent) {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (file == null || !file.isValid()) return null;
    if (indicator != null) {
      update(indicator, false, ((double)myScannedFileCount++) / myTotalFileCount);
    }


    boolean isMarked = myMarker != null && myMarker.isMarked(file);
    if (isMarked) myMarkedFileCount++;
    if (isMarked || myAddUnmarkedFiles) {
      PackageDependenciesNode dirNode = !myCompactEmptyMiddlePackages && lastParent != null ? lastParent : getFileParentNode(file);

      if (myShowFiles) {
        FileNode fileNode = new FileNode(file, myProject, isMarked);
        dirNode.add(fileNode);
      }
      else {
        dirNode.addFile(file, isMarked);
      }
      return dirNode;
    }
    return null;
  }

  public @NotNull PackageDependenciesNode getFileParentNode(VirtualFile file) {
    LOG.assertTrue(file != null);
    final VirtualFile containingDirectory = file.getParent();
    return getModuleDirNode(containingDirectory, myFileIndex.getModuleForFile(file), null);
  }

  public boolean hasFileNode(@NotNull VirtualFile file) {
    return myModuleDirNodes.containsKey(file);
  }

  @Nullable
  public DefaultMutableTreeNode removeNode(final PsiElement element, PsiDirectory parent) {
    LOG.assertTrue(parent != null, element instanceof PsiFile && ((PsiFile)element).getVirtualFile() != null ? ((PsiFile)element).getVirtualFile().getPath() : element);
    final VirtualFile parentVirtualFile = parent.getVirtualFile();
    Module module = myFileIndex.getModuleForFile(parentVirtualFile);
    if (element instanceof PsiDirectory && myFlattenPackages) {
      final PackageDependenciesNode moduleNode = getModuleNode(module);
      final PsiDirectory psiDirectory = (PsiDirectory)element;
      final VirtualFile virtualFile = psiDirectory.getVirtualFile();
      final PackageDependenciesNode dirNode =
        getModuleDirNode(virtualFile, myFileIndex.getModuleForFile(virtualFile), null);
      dirNode.removeFromParent();
      return moduleNode;
    }
    DirectoryNode dirNode = myModuleDirNodes.get(parentVirtualFile);
    if (dirNode == null) return null;
    DirectoryNode wrapper = dirNode.getWrapper();
    while (wrapper != null) {
      dirNode = wrapper;
      myModuleDirNodes.put(wrapper.getDirectory(), null);
      wrapper = dirNode.getWrapper();
    }
    final PackageDependenciesNode[] classOrDirNodes = findNodeForPsiElement(dirNode, element);
    if (classOrDirNodes != null){
      for (PackageDependenciesNode classNode : classOrDirNodes) {
        classNode.removeFromParent();
      }
    }

    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)dirNode.getParent();
    DefaultMutableTreeNode node = dirNode;
    if (element == parent) {
      myModuleDirNodes.put(parentVirtualFile, null);
      dirNode.removeFromParent();
      node = parentNode;
    }
    while (parent != null && node != null && node.getChildCount() == 0) {
      PsiDirectory directory = parent.getParentDirectory();
      parentNode = (DefaultMutableTreeNode)node.getParent();
      node.removeFromParent();
      if (node instanceof DirectoryNode) {
        while (node != null) {  //clear all compacted links
          myModuleDirNodes.put(((DirectoryNode)node).getDirectory(), null);
          node = ((DirectoryNode)node).getCompactedDirNode();
        }
      } else if (node instanceof ModuleNode) {
        myModuleNodes.put(((ModuleNode)node).getModule(), null);
      } else if (node instanceof ModuleGroupNode) {
        myModuleGroupNodes.put(((ModuleGroupNode)node).getModuleGroupName(), null);
      }
      node = parentNode;
      parent = directory;
    }
    if (myCompactEmptyMiddlePackages && node instanceof DirectoryNode && node.getChildCount() == 1) { //compact
      final TreeNode treeNode = node.getChildAt(0);
      if (treeNode instanceof DirectoryNode){
        node.removeAllChildren();
        for (int i = treeNode.getChildCount() - 1; i >= 0; i--){
          node.add((MutableTreeNode)treeNode.getChildAt(i));
        }
        ((DirectoryNode)node).setCompactedDirNode((DirectoryNode)treeNode);
      }
    }
    return parentNode != null ? parentNode : myRoot;
  }

  @Nullable
  public PackageDependenciesNode addFileNode(final PsiFile file){
    final VirtualFile vFile = file.getVirtualFile();
    LOG.assertTrue(vFile != null);
    boolean isMarked = myMarker != null && myMarker.isMarked(vFile);
    if (!isMarked) return null;

    VirtualFile dirToReload = vFile.getParent();
    PackageDependenciesNode rootToReload = myModuleDirNodes.get(dirToReload);
    if (rootToReload == null && myFlattenPackages) {
      final Module module = myFileIndex.getModuleForFile(vFile);
      final boolean moduleNodeExist = myModuleNodes.get(module) != null;
      rootToReload = getModuleNode(module);
      if (!moduleNodeExist) {
        rootToReload = null; //need to reload from parent / mostly for problems view
      }
    } else {
      //reload parents of compacted nodes as getFileParentNode() may expand them
      while ((rootToReload == null || ((DirectoryNode)rootToReload).getCompactedDirNode() != null) && dirToReload != null){
        dirToReload = dirToReload.getParent();
        rootToReload = myModuleDirNodes.get(dirToReload);
      }
    }

    PackageDependenciesNode dirNode = getFileParentNode(vFile);
    if (findNodeForPsiElement(dirNode, file) == null) {  //check if dir node already contains child
      dirNode.add(new FileNode(vFile, myProject, isMarked));
    }
    return rootToReload;
  }

  @Nullable
  public PackageDependenciesNode addDirNode(PsiDirectory dir) {
    final VirtualFile vFile = dir.getVirtualFile();
    if (myMarker == null) return null;
    final boolean[] isMarked = new boolean[]{myMarker.isMarked(vFile)};
    VirtualFile dirToReload = vFile.getParent();
    PackageDependenciesNode rootToReload = myModuleDirNodes.get(dirToReload);
    if (rootToReload == null && myFlattenPackages) {
      final Module module = myFileIndex.getModuleForFile(vFile);
      final boolean moduleNodeExist = myModuleNodes.get(module) != null;
      rootToReload = getModuleNode(module);
      if (!moduleNodeExist) {
        rootToReload = null; //need to reload from parent / mostly for problems view
      }
    }
    else {
      while (rootToReload == null && dirToReload != null) {
        dirToReload = dirToReload.getParent();
        rootToReload = myModuleDirNodes.get(dirToReload);
      }
    }
    myFileIndex.iterateContentUnderDirectory(vFile, new MyContentIterator() {
      @Override
      public boolean processFile(@NotNull VirtualFile fileOrDir) {
        isMarked[0] |= myMarker.isMarked(fileOrDir);
        return super.processFile(fileOrDir);
      }
    });
    if (!isMarked[0]) return null;

    getModuleDirNode(vFile, myFileIndex.getModuleForFile(vFile), null);
    return rootToReload != null ? rootToReload : myRoot;
  }


  @Nullable
  public PackageDependenciesNode findNode(PsiFileSystemItem file, final PsiElement psiElement) {
    if (file instanceof PsiDirectory) {
      return getModuleDirNode(file.getVirtualFile(), myFileIndex.getModuleForFile(file.getVirtualFile()), null);
    }
    PackageDependenciesNode parent = getFileParentNode(file.getVirtualFile());
    PackageDependenciesNode[] nodes = findNodeForPsiElement(parent, file);
    if (nodes == null || nodes.length == 0) {
      return null;
    }
    else {
      for (PackageDependenciesNode node : nodes) {
        if (node.getPsiElement() == psiElement) return node;
      }
      return nodes[0];
    }
  }

  @Nullable
  public static PackageDependenciesNode[] findNodeForPsiElement(PackageDependenciesNode parent, PsiElement element){
    final Set<PackageDependenciesNode> result = new HashSet<>();
    for (int i = 0; i < parent.getChildCount(); i++){
      final TreeNode treeNode = parent.getChildAt(i);
      if (treeNode instanceof PackageDependenciesNode){
        final PackageDependenciesNode node = (PackageDependenciesNode)treeNode;
        if (element instanceof PsiDirectory && node.getPsiElement() == element){
          return new PackageDependenciesNode[] {node};
        }
        if (element instanceof PsiFile) {
          PsiFile psiFile = null;
          if (node instanceof BasePsiNode) {
            psiFile = ((BasePsiNode)node).getContainingFile();
          }
          else if (node instanceof FileNode) { //non java files
            psiFile = ((PsiFile)node.getPsiElement());
          }
          if (psiFile != null && Comparing.equal(psiFile.getVirtualFile(), ((PsiFile)element).getVirtualFile())) {
            result.add(node);
          }
        }
      }
    }
    return result.isEmpty() ? null : result.toArray(new PackageDependenciesNode[0]);
  }

  private PackageDependenciesNode getModuleDirNode(VirtualFile virtualFile, Module module, DirectoryNode childNode) {
    if (virtualFile == null) {
      return getModuleNode(module);
    }

    PackageDependenciesNode directoryNode = myModuleDirNodes.get(virtualFile);
    if (directoryNode != null) {
      if (myCompactEmptyMiddlePackages) {
        final DirectoryNode nestedNode = ((DirectoryNode)directoryNode).getCompactedDirNode();
        if (nestedNode != null) { //decompact
          boolean expand = false;
          if (myTree != null){
            expand = !myTree.isCollapsed(new TreePath(directoryNode.getPath()));
          }
          DirectoryNode parentWrapper = nestedNode.getWrapper();
          while (parentWrapper.getWrapper() != null) {
            parentWrapper = parentWrapper.getWrapper();
          }
          for (int i = parentWrapper.getChildCount() - 1; i >= 0; i--) {
            nestedNode.add((MutableTreeNode)parentWrapper.getChildAt(i));
          }
          ((DirectoryNode)directoryNode).setCompactedDirNode(null);
          parentWrapper.add(nestedNode);
          nestedNode.removeUpReference();
          if (myTree != null && expand) {
            final Runnable expandRunnable = () -> myTree.expandPath(new TreePath(nestedNode.getPath()));
            SwingUtilities.invokeLater(expandRunnable);
          }
          return parentWrapper;
        }
        if (directoryNode.getParent() == null) {    //find first node in tree
          DirectoryNode parentWrapper = ((DirectoryNode)directoryNode).getWrapper();
          if (parentWrapper != null) {
            while (parentWrapper.getWrapper() != null) {
              parentWrapper = parentWrapper.getWrapper();
            }
            return parentWrapper;
          }
        }
      }
      return directoryNode;
    }

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    final VirtualFile sourceRoot = fileIndex.getSourceRootForFile(virtualFile);
    final VirtualFile contentRoot = fileIndex.getContentRootForFile(virtualFile);

    directoryNode = new DirectoryNode(virtualFile, myProject, myCompactEmptyMiddlePackages, myFlattenPackages, myBaseDir,
                                      myContentRoots);
    myModuleDirNodes.put(virtualFile, (DirectoryNode)directoryNode);

    final VirtualFile directory = virtualFile.getParent();
    if (!myFlattenPackages && directory != null) {
      if (myCompactEmptyMiddlePackages && !Comparing.equal(sourceRoot, virtualFile) && !Comparing.equal(contentRoot, virtualFile)) {//compact
        ((DirectoryNode)directoryNode).setCompactedDirNode(childNode);
      }
      if (fileIndex.getModuleForFile(directory) == module) {
        DirectoryNode parentDirectoryNode = myModuleDirNodes.get(directory);
        if (parentDirectoryNode != null
            || !myCompactEmptyMiddlePackages
            || (sourceRoot != null && VfsUtilCore.isAncestor(directory, sourceRoot, false) && fileIndex.getSourceRootForFile(directory) != null)
            || Comparing.equal(directory, contentRoot)) {
          getModuleDirNode(directory, module, (DirectoryNode)directoryNode).add(directoryNode);
        }
        else {
          directoryNode = getModuleDirNode(directory, module, (DirectoryNode)directoryNode);
        }
      }
      else {
        getModuleNode(module).add(directoryNode);
      }
    }
    else {
      if (Comparing.equal(contentRoot, virtualFile)) {
        getModuleNode(module).add(directoryNode);
      }
      else {
        final VirtualFile root;
        if (!Comparing.equal(sourceRoot, virtualFile) && sourceRoot != null) {
          root = sourceRoot;
        }
        else if (contentRoot != null) {
          root = contentRoot;
        }
        else {
          root = null;
        }
        if (root != null) {
          getModuleDirNode(root, module, null).add(directoryNode);
        } else {
          if (myExternalNode == null) {
            myExternalNode = new GeneralGroupNode("External Dependencies", AllIcons.Nodes.PpLibFolder, myProject);
            myRoot.add(myExternalNode);
          }

          myExternalNode.add(directoryNode);
        }
      }
    }

    return directoryNode;
  }


  @Nullable
  private PackageDependenciesNode getModuleNode(Module module) {
    if (module == null || !myShowModules) {
      return myRoot;
    }
    ModuleNode node = myModuleNodes.get(module);
    if (node != null) return node;
    node = new ModuleNode(module, myShowModuleGroups ? myGrouper : null);
    final List<String> groupPath = myGrouper.getGroupPath(module);
    if (groupPath.isEmpty()) {
      myModuleNodes.put(module, node);
      myRoot.add(node);
      return node;
    }
    myModuleNodes.put(module, node);
    if (myShowModuleGroups) {
      getParentModuleGroup(groupPath).add(node);
    } else {
      myRoot.add(node);
    }
    return node;
  }

  private PackageDependenciesNode getParentModuleGroup(List<String> groupPath){
    final String key = StringUtil.join(groupPath, "");
    ModuleGroupNode groupNode = myModuleGroupNodes.get(key);
    if (groupNode == null) {
      groupNode = new ModuleGroupNode(new ModuleGroup(groupPath), myProject);
      myModuleGroupNodes.put(key, groupNode);
      myRoot.add(groupNode);
    }
    if (groupPath.size() > 1) {
      final PackageDependenciesNode node = getParentModuleGroup(groupPath.subList(0, groupPath.size() - 1));
      node.add(groupNode);
    }
    return groupNode;
  }


  private class MyContentIterator implements ContentIterator {
    PackageDependenciesNode lastParent = null;
    VirtualFile dir;

    @Override
    public boolean processFile(@NotNull VirtualFile fileOrDir) {
      ReadAction.run(() -> {
        if (!fileOrDir.isDirectory()) {
          if (lastParent != null && !Comparing.equal(dir, fileOrDir.getParent())) {
            lastParent = null;
          }
          lastParent = buildFileNode(fileOrDir, lastParent);
          dir = fileOrDir.getParent();
        } else {
          lastParent = null;
        }
      });
      return true;
    }
  }
}
