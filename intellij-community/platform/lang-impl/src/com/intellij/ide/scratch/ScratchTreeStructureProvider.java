// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.*;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeUi;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.psi.*;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * @author gregsh
 */
public class ScratchTreeStructureProvider implements TreeStructureProvider, DumbAware {

  public ScratchTreeStructureProvider(Project project) {
    registerUpdaters(project, project, new Runnable() {
      AbstractProjectViewPane updateTarget;
      @Override
      public void run() {
        if (updateTarget == null) {
          updateTarget = ProjectView.getInstance(project).getProjectViewPaneById(ProjectViewPane.ID);
        }
        if (updateTarget != null) updateTarget.updateFromRoot(true);
      }
    });
  }

  private static void registerUpdaters(@NotNull Project project, @NotNull Disposable disposable, @NotNull Runnable onUpdate) {
    String scratchPath = FileUtil.toSystemIndependentName(FileUtil.toCanonicalPath(PathManager.getScratchPath()));
    VirtualFileManager.getInstance().addAsyncFileListener(events -> {
      boolean update = JBIterable.from(events).find(e -> {
        ProgressManager.checkCanceled();

        final boolean isDirectory = isDirectory(e);
        final VirtualFile parent = getNewParent(e);
        return parent != null && (ScratchUtil.isScratch(parent) ||
                                  isDirectory && parent.getPath().startsWith(scratchPath));
      }) != null;

      return !update ? null : new AsyncFileListener.ChangeApplier() {
        @Override
        public void afterVfsChange() {
          onUpdate.run();
        }
      };
    }, disposable);
    ConcurrentMap<RootType, Disposable> disposables = ConcurrentFactoryMap.createMap(o -> Disposer.newDisposable(o.getDisplayName()));
    for (RootType rootType : RootType.getAllRootTypes()) {
      registerRootTypeUpdater(project, rootType, onUpdate, disposable, disposables);
    }
    RootType.ROOT_EP.addExtensionPointListener(new ExtensionPointListener<RootType>() {
      @Override
      public void extensionAdded(@NotNull RootType rootType, @NotNull PluginDescriptor pluginDescriptor) {
        registerRootTypeUpdater(project, rootType, onUpdate, disposable, disposables);
      }

      @Override
      public void extensionRemoved(@NotNull RootType rootType, @NotNull PluginDescriptor pluginDescriptor) {
        Disposable rootDisposable = disposables.remove(rootType);
        if (rootDisposable != null) Disposer.dispose(rootDisposable);
      }
    }, project);
    RootType.ROOT_EP.addChangeListener(onUpdate, project);
  }

  private static void registerRootTypeUpdater(@NotNull Project project,
                                              @NotNull RootType rootType,
                                              @NotNull Runnable onUpdate,
                                              @NotNull Disposable parentDisposable,
                                              @NotNull Map<RootType, Disposable> disposables) {
    if (rootType.isHidden()) return;
    Disposable rootDisposable = disposables.get(rootType);
    Disposer.register(parentDisposable, rootDisposable);
    ReadAction
      .nonBlocking(() -> rootType.registerTreeUpdater(project, parentDisposable, onUpdate))
      .expireWith(parentDisposable)
      .submit(NonUrgentExecutor.getInstance());
  }

  private static VirtualFile getNewParent(@NotNull VFileEvent e) {
    if (e instanceof VFileMoveEvent) {
      return ((VFileMoveEvent)e).getNewParent();
    }
    else if (e instanceof VFileCopyEvent) {
      return ((VFileCopyEvent)e).getNewParent();
    }
    else if (e instanceof VFileCreateEvent) {
      return ((VFileCreateEvent)e).getParent();
    }
    else {
      return Objects.requireNonNull(e.getFile()).getParent();
    }
  }

  private static boolean isDirectory(@NotNull VFileEvent e) {
    if (e instanceof VFileCreateEvent) {
      return ((VFileCreateEvent)e).isDirectory();
    }
    else {
      return Objects.requireNonNull(e.getFile()).isDirectory();
    }
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

  @Nullable
  private static AbstractTreeNode<?> createRootTypeNode(@NotNull Project project, @NotNull RootType rootType, @NotNull ViewSettings settings) {
    if (rootType.isHidden()) return null;
    MyRootNode node = new MyRootNode(project, rootType, settings);
    return node.isEmpty() ? null : node;
  }

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> modify(@NotNull AbstractTreeNode<?> parent,
                                                         @NotNull Collection<AbstractTreeNode<?>> children,
                                                         ViewSettings settings) {
    Project project = parent instanceof ProjectViewProjectNode ? parent.getProject() : null;
    if (project == null) return children;
    if (ApplicationManager.getApplication().isUnitTestMode()) return children;
    if (children.isEmpty() &&
        JBIterable.from(RootType.getAllRootTypes()).filterMap(o -> createRootTypeNode(project, o, settings)).isEmpty()) {
      return children;
    }
    List<AbstractTreeNode<?>> list = new ArrayList<>(children.size() + 1);
    list.addAll(children);
    list.add(new MyProjectNode(project, settings));
    return list;
  }

  /**
   * @deprecated Use modify method instead
   */
  @Deprecated
  public static AbstractTreeNode<?> createRootNode(@NotNull Project project, ViewSettings settings) {
    return new MyProjectNode(project, settings);
  }

  @Override
  public @Nullable Object getData(@NotNull Collection<AbstractTreeNode<?>> selected, @NotNull String dataId) {
    if (LangDataKeys.PASTE_TARGET_PSI_ELEMENT.is(dataId)) {
      AbstractTreeNode<?> single = JBIterable.from(selected).single();
      if (single instanceof MyRootNode) {
        VirtualFile file = ((MyRootNode)single).getVirtualFile();
        Project project = single.getProject();
        return file == null || project == null ? null : PsiManager.getInstance(project).findDirectory(file);
      }
    }
    return null;
  }

  private static final class MyProjectNode extends ProjectViewNode<String> {
    MyProjectNode(Project project, ViewSettings settings) {
      super(project, ScratchesNamedScope.scratchesAndConsoles(), settings);
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
      return ScratchUtil.isScratch(file);
    }

    @NotNull
    @Override
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
      List<AbstractTreeNode<?>> list = new ArrayList<>();
      Project project = Objects.requireNonNull(getProject());
      for (RootType rootType : RootType.getAllRootTypes()) {
        ContainerUtil.addIfNotNull(list, createRootTypeNode(project, rootType, getSettings()));
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
      return Objects.equals(virtualFile.getPath(), FileUtil.toSystemIndependentName(PathManager.getScratchPath()));
    }
  }

  private static class MyRootNode extends ProjectViewNode<RootType> implements PsiFileSystemItemFilter {
    MyRootNode(Project project, @NotNull RootType type, ViewSettings settings) {
      super(project, type, settings);
    }

    @NotNull
    public RootType getRootType() {
      return Objects.requireNonNull(getValue());
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
      return getValue().containsFile(file);
    }

    @Nullable
    @Override
    public VirtualFile getVirtualFile() {
      return ScratchTreeStructureProvider.getVirtualFile(getRootType());
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getRoots() {
      return getDefaultRootsFor(getVirtualFile());
    }

    @NotNull
    @Override
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
      //noinspection ConstantConditions
      return getDirectoryChildrenImpl(getProject(), getDirectory(), getSettings(), this);
    }

    PsiDirectory getDirectory() {
      //noinspection ConstantConditions
      return ScratchTreeStructureProvider.getDirectory(getProject(), getValue());
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
      Project project = Objects.requireNonNull(getProject());
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
    static Collection<AbstractTreeNode<?>> getDirectoryChildrenImpl(@NotNull Project project,
                                                                 @Nullable PsiDirectory directory,
                                                                 @NotNull ViewSettings settings,
                                                                 @NotNull PsiFileSystemItemFilter filter) {
      final List<AbstractTreeNode<?>> result = new ArrayList<>();
      PsiElementProcessor<PsiFileSystemItem> processor = new PsiElementProcessor<PsiFileSystemItem>() {
        @Override
        public boolean execute(@NotNull PsiFileSystemItem element) {
          if (!filter.shouldShow(element)) {
            // skip
          }
          else if (element instanceof PsiDirectory) {
            result.add(new PsiDirectoryNode(project, (PsiDirectory)element, settings, filter) {
              @Override
              public Collection<AbstractTreeNode<?>> getChildrenImpl() {
                //noinspection ConstantConditions
                return getDirectoryChildrenImpl(getProject(), getValue(), getSettings(), getFilter());
              }
            });
          }
          else if (element instanceof PsiFile) {
            result.add(new PsiFileNode(project, (PsiFile)element, settings) {
              @Override
              public Comparable<ExtensionSortKey> getTypeSortKey() {
                PsiFile value = getValue();
                Language language = value == null ? null : value.getLanguage();
                LanguageFileType fileType = language == null ? null : language.getAssociatedFileType();
                return fileType == null ? null : new ExtensionSortKey(fileType.getDefaultExtension());
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
}
