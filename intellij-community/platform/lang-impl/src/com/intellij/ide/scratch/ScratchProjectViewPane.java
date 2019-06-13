// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.icons.AllIcons;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.impl.ProjectViewSelectInTarget;
import com.intellij.ide.projectView.*;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import com.intellij.ide.projectView.impl.ProjectTreeStructure;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.*;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeUi;
import com.intellij.lang.Language;
import com.intellij.notebook.editor.BackedVirtualFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.*;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES;

/**
 * @author gregsh
 */
public class ScratchProjectViewPane extends ProjectViewPane {

  public static final String ID = "Scratches";

  public static boolean isScratchesMergedIntoProjectTab() {
    return Registry.is("ide.scratch.in.project.view") &&
           !ApplicationManager.getApplication().isUnitTestMode();
  }

  public ScratchProjectViewPane(Project project) {
    super(project);
    registerUpdaters(project, this, new Runnable() {
      AbstractProjectViewPane updateTarget;
      @Override
      public void run() {
        if (updateTarget == null) {
          updateTarget = !isScratchesMergedIntoProjectTab() ? ScratchProjectViewPane.this :
                         ProjectView.getInstance(project).getProjectViewPaneById(ProjectViewPane.ID);
        }
        if (updateTarget != null) updateTarget.updateFromRoot(true);
      }
    });
  }

  @NotNull
  @Override
  public String getTitle() {
    return "Scratches";
  }

  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Scope.Scratches;
  }

  @NotNull
  @Override
  protected ProjectAbstractTreeStructureBase createStructure() {
    return new MyTreeStructure(myProject);
  }

  @Override
  public int getWeight() {
    return 11;
  }

  private static void registerUpdaters(@NotNull Project project, @NotNull Disposable disposable, @NotNull Runnable onUpdate) {
    String scratchPath = FileUtil.toSystemIndependentName(FileUtil.toCanonicalPath(PathManager.getScratchPath()));
    project.getMessageBus().connect(disposable).subscribe(VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        boolean update = JBIterable.from(events).find(e -> {
          VirtualFile file = e.getFile();
          VirtualFile parent = file == null ? null : file.getParent();
          if (parent == null) return false;
          return ScratchUtil.isScratch(parent) ||
                 file.isDirectory() && parent.getPath().startsWith(scratchPath);
        }) != null;
        if (update) {
          onUpdate.run();
        }
      }
    });
    for (RootType rootType : RootType.getAllRootTypes()) {
      if (rootType.isHidden()) continue;
      rootType.registerTreeUpdater(project, disposable, onUpdate);
    }
  }

  @NotNull
  @Override
  public SelectInTarget createSelectInTarget() {
    return new ProjectViewSelectInTarget(myProject) {

      @Override
      protected boolean canSelect(PsiFileSystemItem file) {
        VirtualFile vFile = PsiUtilCore.getVirtualFile(file);
        vFile = BackedVirtualFile.getOriginFileIfBacked(vFile);
        if (vFile == null || !vFile.isValid()) return false;
        if (!vFile.isInLocalFileSystem()) return false;

        return ScratchFileService.getInstance().getRootType(vFile) != null;
      }

      @Override
      public String toString() {
        return getTitle();
      }

      @Override
      public String getMinorViewId() {
        return getId();
      }

      @Override
      public float getWeight() {
        return ScratchProjectViewPane.this.getWeight();
      }
    };
  }

  @Nullable
  private static PsiDirectory getDirectory(@NotNull Project project, @NotNull RootType rootType) {
    VirtualFile virtualFile = getVirtualFile(rootType);
    return virtualFile == null ? null : PsiManager.getInstance(project).findDirectory(virtualFile);
  }

  @Nullable
  private static VirtualFile getVirtualFile(@NotNull RootType rootType) {
    String path = ScratchFileService.getInstance().getRootPath(rootType);
    return LocalFileSystem.getInstance().findFileByPath(path);
  }

  @Override
  public boolean isInitiallyVisible() {
    return !isScratchesMergedIntoProjectTab();
  }

  @NotNull
  public static AbstractTreeNode createRootNode(@NotNull Project project, @NotNull ViewSettings settings) {
    return new MyProjectNode(project, settings);
  }

  @Nullable
  private static AbstractTreeNode createRootNode(@NotNull Project project, @NotNull RootType rootType, @NotNull ViewSettings settings) {
    if (rootType.isHidden()) return null;
    MyRootNode node = new MyRootNode(project, rootType, settings);
    return node.isEmpty() ? null : node;
  }

  public static class MyStructureProvider implements TreeStructureProvider, DumbAware {
    @NotNull
    @Override
    public Collection<AbstractTreeNode> modify(@NotNull AbstractTreeNode parent,
                                               @NotNull Collection<AbstractTreeNode> children,
                                               ViewSettings settings) {
      Project project = parent instanceof ProjectViewProjectNode? parent.getProject() : null;
      if (project == null || !isScratchesMergedIntoProjectTab()) return children;
      if (children.isEmpty() && JBIterable.from(RootType.getAllRootTypes()).filterMap(
        o -> createRootNode(project, o, settings)).isEmpty()) return children;
      ArrayList<AbstractTreeNode> list = new ArrayList<>(children.size() + 1);
      list.addAll(children);
      list.add(createRootNode(project, settings));
      return list;
    }

    @Nullable
    @Override
    public Object getData(@NotNull Collection<AbstractTreeNode> selected, @NotNull String dataId) {
      if (LangDataKeys.PASTE_TARGET_PSI_ELEMENT.is(dataId)) {
        AbstractTreeNode single = JBIterable.from(selected).single();
        if (single instanceof MyRootNode) {
          VirtualFile file = ((MyRootNode)single).getVirtualFile();
          Project project = single.getProject();
          return file == null || project == null ? null : PsiManager.getInstance(project).findDirectory(file);
        }
      }
      return null;
    }
  }

  private static class MyTreeStructure extends ProjectTreeStructure {

    MyTreeStructure(@NotNull Project project) {
      super(project, ID);
    }

    @Override
    protected AbstractTreeNode createRoot(@NotNull Project project, @NotNull ViewSettings settings) {
      return createRootNode(project, settings);
    }

    @Nullable
    @Override
    public List<TreeStructureProvider> getProviders() {
      return null;
    }
  }

  private static class MyProjectNode extends ProjectViewNode<String> {
    MyProjectNode(Project project, ViewSettings settings) {
      super(project, ScratchesNamedScope.NAME, settings);
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
      return FileTypeRegistry.getInstance().isFileOfType(file, ScratchFileType.INSTANCE);
    }

    @NotNull
    @Override
    public Collection<? extends AbstractTreeNode> getChildren() {
      List<AbstractTreeNode> list = new ArrayList<>();
      for (RootType rootType : RootType.getAllRootTypes()) {
        ContainerUtil.addIfNotNull(list, createRootNode(getProject(), rootType, getSettings()));
      }
      return list;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      presentation.setPresentableText(getValue());
      presentation.setIcon(AllIcons.Scope.Scratches);
    }

    @Override
    public boolean canRepresent(Object element) {
      PsiElement item = element instanceof PsiElement ? (PsiElement)element : null;
      VirtualFile virtualFile = item == null ? null : PsiUtilCore.getVirtualFile(item);
      if (virtualFile == null) return false;
      return Comparing.equal(virtualFile.getPath(), FileUtil.toSystemIndependentName(PathManager.getScratchPath()));
    }
  }

  private static class MyRootNode extends ProjectViewNode<RootType> implements PsiFileSystemItemFilter {

    MyRootNode(Project project, @NotNull RootType type, ViewSettings settings) {
      super(project, type, settings);
    }

    @NotNull
    public RootType getRootType() {
      return ObjectUtils.notNull(getValue());
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
      return ScratchFileService.getInstance().getRootType(file) == getValue();
    }

    @Nullable
    @Override
    public VirtualFile getVirtualFile() {
      return ScratchProjectViewPane.getVirtualFile(getRootType());
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getRoots() {
      return getDefaultRootsFor(getVirtualFile());
    }

    @NotNull
    @Override
    public Collection<? extends AbstractTreeNode> getChildren() {
      //noinspection ConstantConditions
      return getDirectoryChildrenImpl(getProject(), getDirectory(), getSettings(), this);
    }

    PsiDirectory getDirectory() {
      //noinspection ConstantConditions
      return ScratchProjectViewPane.getDirectory(getProject(), getValue());
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      presentation.setIcon(AllIcons.Nodes.Folder);
      presentation.setPresentableText(getRootType().getDisplayName());
    }

    @Override
    public boolean canRepresent(Object element) {
      return Comparing.equal(getDirectory(), element);
    }

    public boolean isEmpty() {
      VirtualFile root = getVirtualFile();
      if (root == null) return true;
      RootType rootType = getRootType();
      Project project = ObjectUtils.notNull(getProject());
      for (VirtualFile f : root.getChildren()) {
        if (!rootType.isIgnored(project, f)) return false;
      }
      return true;
    }

    @Override
    public boolean shouldShow(@NotNull PsiFileSystemItem item) {
      //noinspection ConstantConditions
      return !getRootType().isIgnored(getProject(), item.getVirtualFile());
    }

    @NotNull
    static Collection<AbstractTreeNode> getDirectoryChildrenImpl(@NotNull Project project,
                                                                 @Nullable PsiDirectory directory,
                                                                 @NotNull ViewSettings settings,
                                                                 @NotNull PsiFileSystemItemFilter filter) {
      final List<AbstractTreeNode> result = new ArrayList<>();
      PsiElementProcessor<PsiFileSystemItem> processor = new PsiElementProcessor<PsiFileSystemItem>() {
        @Override
        public boolean execute(@NotNull PsiFileSystemItem element) {
          if (!filter.shouldShow(element)) {
            // skip
          }
          else if (element instanceof PsiDirectory) {
            result.add(new PsiDirectoryNode(project, (PsiDirectory)element, settings, filter) {
              @Override
              public Collection<AbstractTreeNode> getChildrenImpl() {
                //noinspection ConstantConditions
                return getDirectoryChildrenImpl(getProject(), getValue(), getSettings(), getFilter());
              }

              @Override
              protected void updateImpl(@NotNull PresentationData data) {
                super.updateImpl(data);
                customizePresentation(this, data);
              }
            });
          }
          else if (element instanceof PsiFile) {
            result.add(new PsiFileNode(project, (PsiFile)element, settings) {
              @Override
              public Comparable getTypeSortKey() {
                PsiFile value = getValue();
                Language language = value == null ? null : value.getLanguage();
                LanguageFileType fileType = language == null ? null : language.getAssociatedFileType();
                return fileType == null ? null : new ExtensionSortKey(fileType.getDefaultExtension());
              }

              @Override
              protected void updateImpl(@NotNull PresentationData data) {
                super.updateImpl(data);
                customizePresentation(this, data);
              }
            });
          }
          return true;
        }
      };

      return AbstractTreeUi.calculateYieldingToWriteAction(() -> {
        if (directory == null || !directory.isValid()) return Collections.emptyList();
        directory.processChildren(processor);
        return result;
      });
    }
  }

  private static void customizePresentation(@NotNull BasePsiNode node, @NotNull PresentationData data) {
    VirtualFile file = ObjectUtils.notNull(node.getVirtualFile());
    Project project = ObjectUtils.notNull(node.getProject());
    AbstractTreeNode parent = node.getParent();
    MyRootNode rootNode = parent instanceof MyRootNode ? (MyRootNode)parent :
                          parent instanceof PsiDirectoryNode ? (MyRootNode)((PsiDirectoryNode)parent).getFilter() : null;
    if (rootNode == null) return;
    RootType rootType = rootNode.getRootType();
    String name = rootType.substituteName(project, file);
    if (name != null) data.setPresentableText(name);

    Icon icon = rootType.substituteIcon(project, file);
    if (icon != null) data.setIcon(icon);
  }
}
