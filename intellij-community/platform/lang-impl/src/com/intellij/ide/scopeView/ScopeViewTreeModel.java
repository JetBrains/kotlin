// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scopeView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.CopyPasteUtil;
import com.intellij.ide.projectView.*;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.impl.OpenFilesScope;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.NavigatableWithText;
import com.intellij.problems.ProblemListener;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.scope.ProblemsScope;
import com.intellij.psi.search.scope.ProjectFilesScope;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.icons.RowIcon;
import com.intellij.ui.stripe.ErrorStripe;
import com.intellij.ui.tree.AbstractTreeWalker;
import com.intellij.ui.tree.BaseTreeModel;
import com.intellij.ui.tree.TreePathUtil;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.tree.project.ProjectFileNode;
import com.intellij.ui.tree.project.ProjectFileTreeModel;
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.tree.TreeModelAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static com.intellij.ide.projectView.impl.CompoundIconProvider.findIcon;
import static com.intellij.ide.projectView.impl.ShowModulesAction.hasModules;
import static com.intellij.openapi.util.io.FileUtil.getLocationRelativeToUserHome;
import static com.intellij.openapi.vfs.VfsUtilCore.*;
import static java.util.Collections.emptyList;

public final class ScopeViewTreeModel extends BaseTreeModel<AbstractTreeNode> implements InvokerSupplier {
  private static final Logger LOG = Logger.getInstance(ScopeViewTreeModel.class);
  private volatile Comparator<? super NodeDescriptor> comparator;
  private final ProjectFileTreeModel model;
  private final ProjectNode root;

  ScopeViewTreeModel(@NotNull Project project, @NotNull ViewSettings settings) {
    model = new ProjectFileTreeModel(project);
    model.addTreeModelListener(new TreeModelAdapter() {
      @Override
      protected void process(@NotNull TreeModelEvent event, @NotNull EventType type) {
        if (type == EventType.StructureChanged) {
          TreePath path = event.getTreePath();
          if (path == null || null == path.getParentPath()) {
            invalidate(null);
          }
          else {
            Object component = path.getLastPathComponent();
            if (component instanceof ProjectFileNode) {
              ProjectFileNode node = (ProjectFileNode)component;
              notifyStructureChanged(node.getVirtualFile());
            }
          }
        }
      }
    });
    Disposer.register(this, model);
    root = new ProjectNode(project, settings);
    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(ProblemListener.TOPIC, new ProblemListener() {
      @Override
      public void problemsAppeared(@NotNull VirtualFile file) {
        problemsDisappeared(file);
      }

      @Override
      public void problemsDisappeared(@NotNull VirtualFile file) {
        if (!updateScopeIf(ProblemsScope.class)) notifyPresentationChanged(file);
      }
    });
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void fileOpened(@NotNull FileEditorManager manager, @NotNull VirtualFile file) {
        fileClosed(manager, file);
      }

      @Override
      public void fileClosed(@NotNull FileEditorManager manager, @NotNull VirtualFile file) {
        updateScopeIf(OpenFilesScope.class);
      }
    });
    FileStatusManager.getInstance(project).addFileStatusListener(new FileStatusListener() {
      @Override
      public void fileStatusChanged(@NotNull VirtualFile file) {
        notifyPresentationChanged(file);
      }

      @Override
      public void fileStatusesChanged() {
        invalidate(null); // TODO: visit all loaded nodes
      }
    }, this);
    CopyPasteUtil.addDefaultListener(this, element -> {
      VirtualFile file = PsiUtilCore.getVirtualFile(element);
      if (file != null) notifyPresentationChanged(file);
    });
  }

  void setStructureProvider(TreeStructureProvider provider) {
    model.onValidThread(() -> {
      if (root.provider == null && provider == null) return;
      root.provider = provider;
      treeStructureChanged(null, null, null);
    });
  }

  void setNodeDecorator(ProjectViewNodeDecorator decorator) {
    model.onValidThread(() -> {
      if (root.decorator == null && decorator == null) return;
      root.decorator = decorator;
      treeStructureChanged(null, null, null);
    });
  }

  public void setComparator(Comparator<? super NodeDescriptor> comparator) {
    model.onValidThread(() -> {
      if (this.comparator == null && comparator == null) return;
      this.comparator = comparator;
      treeStructureChanged(null, null, null);
    });
  }

  public void setFilter(@Nullable NamedScopeFilter filter) {
    root.filter = filter;
    LOG.debug("set filter", filter);
    model.setFilter(filter != null && filter.getScope() instanceof ProjectFilesScope ? null : filter);
  }

  public NamedScopeFilter getFilter() {
    return root.filter;
  }

  @Nullable
  public Object getContent(Object object) {
    if (object instanceof GroupNode) {
      GroupNode node = (GroupNode)object;
      object = node.getSingleRoot();
    }
    if (object instanceof FileNode) {
      FileNode node = (FileNode)object;
      PsiElement element = node.findFileSystemItem(node.getVirtualFile());
      if (element == null || node.compacted == null) return element;
      if (isPackage(node.getIcon()) && node.getSettings().isFlattenPackages()) return element;
      ArrayDeque<PsiElement> deque = new ArrayDeque<>();
      node.compacted.forEach(file -> {
        PsiFileSystemItem item = node.findFileSystemItem(file);
        if (item != null) deque.addFirst(item);
      });
      if (deque.isEmpty()) return element;
      deque.addFirst(element);
      return deque.toArray();
    }
    if (object instanceof NodeDescriptor) {
      NodeDescriptor descriptor = (NodeDescriptor)object;
      object = descriptor.getElement();
    }
    if (object instanceof AbstractTreeNode) {
      AbstractTreeNode node = (AbstractTreeNode)object;
      object = node.getValue();
    }
    return object;
  }

  @NotNull
  @Override
  public Invoker getInvoker() {
    return model.getInvoker();
  }

  public void invalidate(@Nullable Runnable onDone) {
    model.onValidThread(() -> {
      root.childrenValid = false;
      LOG.debug("whole structure changed");
      ViewSettings settings = root.getSettings();
      boolean isShowExcludedFiles = false;
      if (settings instanceof ProjectViewSettings && ((ProjectViewSettings)settings).isShowExcludedFiles()) {
        NamedScopeFilter filter = getFilter();
        Class<? extends NamedScope> type = filter == null ? null : filter.getScope().getClass();
        isShowExcludedFiles = !NamedScope.class.equals(type); // disable excluded files for custom scopes
      }
      model.setSettings(isShowExcludedFiles, hasModules() && settings.isShowModules());
      treeStructureChanged(null, null, null);
      if (onDone != null) onDone.run();
    });
  }

  private void update(@NotNull AbstractTreeNode node, boolean structure) {
    model.onValidThread(() -> {
      boolean updated = node.update();
      boolean changed = structure || !(node instanceof Node);
      if (!updated && !changed) return;
      TreePath path = TreePathUtil.pathToCustomNode(node, AbstractTreeNode::getParent);
      if (path == null || root != path.getPathComponent(0)) return;
      if (changed) {
        LOG.debug("structure changed: ", node);
        treeStructureChanged(path, null, null);
      }
      else {
        LOG.debug("node updated: ", node);
        treeNodesChanged(path, null, null);
      }
    });
  }

  private void notifyStructureChanged(@NotNull VirtualFile file) {
    boolean flattenPackages = root.getSettings().isFlattenPackages();
    if (flattenPackages) {
      ProjectFileIndex index = getProjectFileIndex(root.getProject());
      VirtualFile ancestor = index == null ? null : index.getSourceRootForFile(file);
      if (ancestor != null && isAncestor(ancestor, file, true)) {
        // TODO: check that file is located under a source root with packages
        file = ancestor;
      }
      else {
        flattenPackages = false;
      }
    }
    boolean resolveCompactedFolder = !flattenPackages && file.isDirectory() && root.getSettings().isCompactDirectories();
    find(file, null, found -> {
      if (found instanceof Node) {
        Node node = (Node)found;
        if (resolveCompactedFolder) {
          AbstractTreeNode parent = node.getParent();
          if (parent instanceof Node) node = (Node)parent;
        }
        if (node.childrenValid) {
          node.childrenValid = false;
          update(node, true);
        }
      }
      else if (found instanceof AbstractTreeNode) {
        update((AbstractTreeNode)found, true);
      }
    });
  }

  private void notifyPresentationChanged(@NotNull VirtualFile file) {
    // find first valid parent for removed file
    while (!file.isValid()) {
      file = file.getParent();
      if (file == null) return;
    }
    List<Node> list = new SmartList<>();
    find(file, list, found -> {
      list.forEach(node -> update(node, false));
      if (found instanceof AbstractTreeNode) {
        update((AbstractTreeNode)found, false);
      }
    });
  }

  private void find(@NotNull VirtualFile file, @Nullable List<? super Node> list, @NotNull Consumer<Object> consumer) {
    model.onValidThread(() -> {
      AreaInstance area = ProjectFileNode.findArea(file, root.getProject());
      if (area != null) {
        TreeVisitor visitor = new TreeVisitor.ByComponent<VirtualFile, AbstractTreeNode>(file, AbstractTreeNode.class) {
          @Override
          protected boolean matches(@NotNull AbstractTreeNode pathComponent, @NotNull VirtualFile thisComponent) {
            if (pathComponent.canRepresent(thisComponent)) return true;
            if (pathComponent instanceof Node) return false;
            ProjectViewNode node = pathComponent instanceof ProjectViewNode ? (ProjectViewNode)pathComponent : null;
            return node != null && node.contains(thisComponent);
          }

          @Override
          protected boolean contains(@NotNull AbstractTreeNode pathComponent, @NotNull VirtualFile thisComponent) {
            Node node = pathComponent instanceof Node ? (Node)pathComponent : null;
            if (node == null || !node.contains(thisComponent, area)) return false;
            if (list != null) list.add(node);
            return true;
          }
        };
        AbstractTreeWalker<AbstractTreeNode> walker = new AbstractTreeWalker<AbstractTreeNode>(visitor) {
          @Override
          protected Collection<AbstractTreeNode> getChildren(@NotNull AbstractTreeNode pathComponent) {
            Node node = pathComponent instanceof Node ? (Node)pathComponent : null;
            return node != null && node.childrenValid ? node.children : emptyList();
          }
        };
        walker.start(root);
        walker.promise().onProcessed(path -> consumer.consume(path == null ? null : path.getLastPathComponent()));
      }
    });
  }

  @Override
  public Object getRoot() {
    if (!model.isValidThread()) return null;
    root.update();
    return root;
  }

  @Override
  public boolean isLeaf(Object object) {
    return root != object && super.isLeaf(object);
  }

  @Override
  public int getChildCount(Object object) {
    if (object instanceof AbstractTreeNode && model.isValidThread()) {
      AbstractTreeNode node = (AbstractTreeNode)object;
      return node.getChildren().size();
    }
    return 0;
  }

  @NotNull
  @Override
  public List<AbstractTreeNode> getChildren(Object object) {
    if (object instanceof AbstractTreeNode && model.isValidThread()) {
      AbstractTreeNode parent = (AbstractTreeNode)object;
      Collection<?> children = parent.getChildren();
      if (!children.isEmpty()) {
        List<AbstractTreeNode> result = new SmartList<>();
        children.forEach(child -> {
          if (child instanceof AbstractTreeNode) {
            AbstractTreeNode node = (AbstractTreeNode)child;
            node.setParent(parent);
            node.update();
            result.add(node);
          }
        });
        Comparator<? super NodeDescriptor> comparator = this.comparator;
        if (comparator != null) result.sort(comparator);
        return result;
      }
    }
    return emptyList();
  }

  @Nullable
  ErrorStripe getStripe(Object object, boolean expanded) {
    if (expanded && object instanceof Node) return null;
    if (object instanceof PresentableNodeDescriptor) {
      PresentableNodeDescriptor node = (PresentableNodeDescriptor)object;
      TextAttributesKey key = node.getPresentation().getTextAttributesKey();
      TextAttributes attributes = key == null ? null : EditorColorsManager.getInstance().getSchemeForCurrentUITheme().getAttributes(key);
      Color color = attributes == null ? null : attributes.getErrorStripeColor();
      if (color != null) return ErrorStripe.create(color, 1);
    }
    return null;
  }

  boolean updateScopeIf(@NotNull Class<? extends NamedScope> type) {
    NamedScopeFilter filter = getFilter();
    if (filter == null || !type.isInstance(filter.getScope())) return false;
    LOG.debug("update filter", filter);
    model.setFilter(filter);
    return true;
  }


  private abstract static class Node extends ProjectViewNode<Object> {
    volatile NamedScopeFilter filter;
    volatile Collection<AbstractTreeNode> children = emptyList();
    volatile boolean childrenValid;

    Node(@NotNull Project project, @NotNull Object value, @NotNull ViewSettings settings) {
      super(project, value, settings);
    }

    Node(@NotNull Node parent, @NotNull Object value) {
      super(parent.getProject(), value, parent.getSettings());
      setParent(parent);
    }

    @Override
    public int getWeight() {
      return 0;
    }

    @Override
    public final boolean canRepresent(Object element) {
      // may be called from unexpected thread
      if (element instanceof PsiFileSystemItem) {
        PsiFileSystemItem item = (PsiFileSystemItem)element;
        element = item.getVirtualFile();
      }
      return element instanceof VirtualFile && canRepresent((VirtualFile)element);
    }

    boolean canRepresent(@NotNull VirtualFile file) {
      // may be called from unexpected thread
      return file.equals(getVirtualFile());
    }

    @Override
    public final boolean contains(@NotNull VirtualFile file) {
      // may be called from unexpected thread
      AreaInstance area = ProjectFileNode.findArea(file, getProject());
      return area != null && contains(file, area);
    }

    @Override
    protected boolean hasProblemFileBeneath() {
      WolfTheProblemSolver solver = getWolfTheProblemSolver(getProject());
      return solver == null || solver.hasProblemFilesBeneath(this::contains);
    }

    // may be called from unexpected thread
    abstract boolean contains(@NotNull VirtualFile file, @NotNull AreaInstance area);

    @Override
    public Color getFileStatusColor(@NotNull FileStatus status) {
      return status.getColor();
    }

    @NotNull
    abstract Collection<AbstractTreeNode> createChildren(@NotNull Collection<? extends AbstractTreeNode> old);

    @NotNull
    @Override
    public final Collection<AbstractTreeNode> getChildren() {
      if (childrenValid) return children;
      Collection<AbstractTreeNode> oldChildren = children;
      Collection<AbstractTreeNode> newChildren = createChildren(oldChildren);
      oldChildren.forEach(node -> node.setParent(null));
      newChildren.forEach(node -> node.setParent(this));
      children = newChildren;
      childrenValid = true;
      return newChildren;
    }

    @Nullable
    String getLocation() {
      return null;
    }

    final void decorate(@NotNull PresentationData presentation) {
      String location = getLocation();
      if (location != null) {
        if (getSettings().isShowURL()) {
          presentation.setLocationString(location);
        }
        else {
          presentation.setTooltip(location);
        }
      }
      ProjectNode parent = findParent(ProjectNode.class);
      ProjectViewNodeDecorator decorator = parent == null ? null : parent.decorator;
      if (decorator != null) decorator.decorate(this, presentation);
    }

    @NotNull
    static Icon getFolderIcon(@Nullable PsiElement element) {
      Icon icon = findIcon(element, 0);
      return icon != null ? icon : AllIcons.Nodes.Folder;
    }

    @Nullable
    final PsiFileSystemItem findFileSystemItem(@NotNull VirtualFile file) {
      return PsiUtilCore.findFileSystemItem(getProject(), file);
    }

    @SuppressWarnings("SameParameterValue")
    final <N> N findParent(Class<N> type) {
      for (AbstractTreeNode node = this; node != null; node = node.getParent()) {
        if (type.isInstance(node)) return type.cast(node);
      }
      return null;
    }
  }


  private final class ProjectNode extends Node {
    private volatile HashMap<Object, RootNode> roots = new HashMap<>();
    volatile TreeStructureProvider provider;
    volatile ProjectViewNodeDecorator decorator;

    ProjectNode(@NotNull Project project, @NotNull ViewSettings settings) {
      super(project, project, settings);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      presentation.setIcon(AllIcons.Nodes.Project);
      presentation.setPresentableText(toString());
      decorate(presentation);
    }

    @Nullable
    @Override
    String getLocation() {
      Project project = getProject();
      return project == null || project.isDisposed() ? null : getLocationRelativeToUserHome(project.getPresentableUrl());
    }

    @NotNull
    @Override
    Collection<AbstractTreeNode> createChildren(@NotNull Collection<? extends AbstractTreeNode> old) {
      HashMap<Object, RootNode> oldRoots = roots;
      HashMap<Object, RootNode> newRoots = new HashMap<>();
      Mapper<RootNode, ProjectFileNode> mapper = new Mapper<>(RootNode::new, oldRoots);
      model.getChildren(model.getRoot()).forEach(child -> newRoots.put(child, mapper.apply(this, child)));
      roots = newRoots;
      if (newRoots.isEmpty()) return emptyList();
      return new Group(newRoots.values(), getSettings().isFlattenModules() || !hasModuleGroups(getProject())).createChildren(this, old);
    }

    @NotNull
    Collection<AbstractTreeNode> createChildren(@NotNull Node parent, @NotNull Collection<? extends AbstractTreeNode> old) {
      boolean flattenPackages = getSettings().isFlattenPackages();
      boolean hideEmptyMiddlePackages = getSettings().isHideEmptyMiddlePackages();
      boolean compactDirectories = getSettings().isCompactDirectories();
      Mapper<FileNode, ProjectFileNode> mapper = new Mapper<>(FileNode::new, FileNode.class, old);
      List<AbstractTreeNode> children = new SmartList<>();
      List<PsiFile> files = new SmartList<>();
      TreeStructureProvider provider = this.provider;
      model.getChildren(parent.getValue()).forEach(child -> {
        PsiElement element = findFileSystemItem(child.getVirtualFile());
        if (element instanceof PsiDirectory) {
          Icon icon = getFolderIcon(element);
          if (!isPackage(icon) || !flattenPackages) {
            ProjectFileNode childNext = !compactDirectories ? null : getSingleDirectory(child);
            while (childNext != null) {
              Icon iconNext = getFolderIcon(findFileSystemItem(childNext.getVirtualFile()));
              if (icon.equals(iconNext)) {
                child = childNext;
                childNext = getSingleDirectory(child);
              }
              else if (isFolder(icon) && !isPackage(iconNext)) {
                icon = iconNext;
                child = childNext;
                childNext = null;
              }
              else {
                childNext = null;
              }
            }
            children.add(mapper.apply(parent, child, icon));
          }
          else if (!isPackage(parent.getIcon())) {
            visitPackages(child, hideEmptyMiddlePackages,
                          childNext -> children.add(mapper.apply(parent, childNext, AllIcons.Nodes.Package)));
          }
        }
        else if (element instanceof PsiFile) {
          if (provider == null) {
            children.add(mapper.apply(parent, child));
          }
          else {
            files.add((PsiFile)element);
          }
        }
      });
      if (provider == null) return children;
      List<AbstractTreeNode> nodes = ContainerUtil.map(files, file -> new PsiFileNode(getProject(), file, getSettings()));
      children.addAll(provider.modify(parent, nodes, getSettings()));
      return children;
    }

    private void visitPackages(@NotNull ProjectFileNode parent, boolean hideEmptyMiddle, @NotNull Consumer<? super ProjectFileNode> consumer) {
      AtomicBoolean empty = new AtomicBoolean(hideEmptyMiddle);
      AtomicBoolean middle = new AtomicBoolean();
      model.getChildren(parent).forEach(child -> {
        PsiElement element = findFileSystemItem(child.getVirtualFile());
        if (element instanceof PsiDirectory) {
          Icon icon = getFolderIcon(element);
          if (isPackage(icon)) {
            if (hideEmptyMiddle) middle.set(true); // contains packages
            visitPackages(child, hideEmptyMiddle, consumer);
          }
          else {
            if (hideEmptyMiddle) empty.set(false); // contains folders
          }
        }
        else if (element instanceof PsiFile) {
          if (hideEmptyMiddle) empty.set(false); // contains files
        }
      });
      if (!empty.get() || !middle.get()) consumer.consume(parent);
    }

    @Nullable
    private ProjectFileNode getSingleDirectory(ProjectFileNode parent) {
      List<ProjectFileNode> children = model.getChildren(parent);
      ProjectFileNode child = children.size() != 1 ? null : children.get(0);
      return child != null && child.getVirtualFile().isDirectory() ? child : null;
    }

    @Override
    boolean contains(@NotNull VirtualFile file, @NotNull AreaInstance area) {
      // may be called from unexpected thread
      return roots.values().stream().anyMatch(root -> root.canRepresentOrContain(file, area));
    }

    @Override
    public int getTypeSortWeight(boolean sortByType) {
      return 1;
    }

    @NotNull
    @Override
    public String toString() {
      Project project = getProject();
      return project == null || project.isDisposed() ? "DISPOSED PROJECT" : project.getName();
    }
  }


  private static class FileNode extends Node {
    final List<VirtualFile> compacted;
    final ProjectFileNode node;

    FileNode(@NotNull Node parent, @NotNull ProjectFileNode node) {
      super(parent, node);
      this.node = node;
      compacted = getCompactedFolders(parent.getVirtualFile(), node.getVirtualFile());
      super.myName = node.getVirtualFile().getName(); // to avoid possible NPE
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.myName = getNodeName(); // rebuild a node name used in #toString()
      VirtualFile file = getVirtualFile();
      String title = getTitle();
      SimpleTextAttributes attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
      if (node.getRootID() instanceof VirtualFile) {
        ProjectFileIndex index = getProjectFileIndex(getProject());
        if (index != null && file.equals(index.getContentRootForFile(file))) {
          attributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
        }
      }
      String text = title != null ? title : toString();
      presentation.setPresentableText(text);
      presentation.addText(text, attributes);
      Icon icon = getIcon();
      if (icon == null && file.isValid()) {
        icon = file.isDirectory()
               ? getFolderIcon(findFileSystemItem(file))
               : file.getFileType().getIcon();
      }
      presentation.setIcon(icon);
      decorate(presentation);
    }

    @Override
    protected boolean valueIsCut() {
      return CopyPasteManager.getInstance().isCutElement(findFileSystemItem(getVirtualFile()));
    }

    @NotNull
    @Override
    Collection<AbstractTreeNode> createChildren(@NotNull Collection<? extends AbstractTreeNode> old) {
      ProjectNode parent = findParent(ProjectNode.class);
      if (parent == null) return emptyList();
      return parent.createChildren(this, old);
    }

    @Override
    boolean canRepresent(@NotNull VirtualFile file) {
      // may be called from unexpected thread
      return super.canRepresent(file) || compacted != null && compacted.stream().anyMatch(file::equals);
    }

    @Override
    boolean contains(@NotNull VirtualFile file, @NotNull AreaInstance area) {
      // may be called from unexpected thread
      return node.contains(file, area, true);
    }

    @Override
    public FileStatus getFileStatus() {
      FileStatusManager manager = getFileStatusManager(getProject());
      return manager == null ? FileStatus.NOT_CHANGED : manager.getRecursiveStatus(getVirtualFile());
    }

    @NotNull
    @Override
    public VirtualFile getVirtualFile() {
      return node.getVirtualFile();
    }

    @Override
    public int getWeight() {
      if (getVirtualFile().isDirectory()) {
        ViewSettings settings = getSettings();
        if (settings == null || settings.isFoldersAlwaysOnTop()) return 0;
      }
      return 20;
    }

    @Override
    public int getTypeSortWeight(boolean sortByType) {
      return getVirtualFile().isDirectory() ? 3 : 5;
    }

    @NotNull
    protected String getNodeName() {
      return getNodeName(getPackageName());
    }

    @NotNull
    private String getNodeName(@Nullable String name) {
      if (name != null) {
        AbstractTreeNode parent = getParent();
        FileNode node = parent instanceof FileNode ? (FileNode)parent : null;
        String prefix = node == null ? null : node.getPackageName();
        if (prefix == null) return name;
        int length = prefix.length();
        if (length > 0 && name.startsWith(prefix)) {
          if (length < name.length() && '.' == name.charAt(length)) length++;
          if (length < name.length()) return name.substring(length);
        }
        LOG.info("unexpected prefix: " + prefix + " for package: " + name);
      }
      if (compacted != null) {
        StringBuilder sb = new StringBuilder();
        char separator = isPackage(getIcon()) ? '.' : VFS_SEPARATOR_CHAR;
        compacted.forEach(file -> sb.append(file.getName()).append(separator));
        return sb.append(getVirtualFile().getName()).toString();
      }
      return getVirtualFile().getName();
    }

    @Nullable
    private String getPackageName() {
      PsiElement element = !isPackage(getIcon()) ? null : findFileSystemItem(getVirtualFile());
      if (element instanceof PsiDirectory && element.isValid()) {
        PsiDirectoryFactory factory = PsiDirectoryFactory.getInstance(element.getProject());
        if (factory != null && factory.isPackage((PsiDirectory)element)) {
          String name = factory.getQualifiedName((PsiDirectory)element, false);
          if (factory.isValidPackageName(name)) return name;
        }
      }
      return null;
    }
  }


  private static final class RootNode extends FileNode implements NavigatableWithText {
    RootNode(@NotNull Node parent, @NotNull ProjectFileNode node) {
      super(parent, node);
    }

    boolean canRepresentOrContain(@NotNull VirtualFile file, @NotNull AreaInstance area) {
      // may be called from unexpected thread
      return node.contains(file, area, false);
    }

    @Override
    public int getWeight() {
      return node.getRootID() instanceof Project ? 0 : super.getWeight();
    }

    @Override
    public int getTypeSortWeight(boolean sortByType) {
      return node.getRootID() instanceof Project ? 1 : super.getTypeSortWeight(sortByType);
    }

    @NotNull
    @Override
    public String getTitle() {
      return getLocation(false);
    }

    @NotNull
    @Override
    protected String getNodeName() {
      return getLocation(true);
    }

    @NotNull
    private String getLocation(boolean allowEmpty) {
      VirtualFile dir = ProjectFileNode.findBaseDir(getProject());
      String location = dir == null ? null : getRelativePath(getVirtualFile(), dir);
      if (location != null && (allowEmpty || !location.isEmpty())) return location;
      return getLocationRelativeToUserHome(getVirtualFile().getPresentableUrl());
    }

    @Override
    public boolean canNavigate() {
      return null != getProjectSettingsService(getProject());
    }

    @Override
    public void navigate(boolean requestFocus) {
      ProjectSettingsService service = getProjectSettingsService(getProject());
      if (service != null) {
        Module module = getModule(getVirtualFile(), getProject());
        if (module != null && service.canOpenModuleSettings()) {
          service.openModuleSettings(module);
        }
        else {
          service.openProjectSettings();
        }
      }
    }

    @Override
    public String getNavigateActionText(boolean focusEditor) {
      return ActionsBundle.message("action.ModuleSettings.navigate");
    }
  }


  private static final class GroupNode extends Node implements NavigatableWithText {
    private final String prefix;
    private final String name;
    private Group group;

    GroupNode(@NotNull Node parent, @NotNull Object value) {
      super(parent, value);
      if (value instanceof Module) {
        List<String> list = getModuleNameAsList((Module)value, false);
        int index = list.size() - 1;
        if (index > 0) {
          StringBuilder sb = new StringBuilder();
          for (int i = 0; i < index; i++) sb.append(list.get(i)).append(VFS_SEPARATOR_CHAR);
          prefix = sb.toString();
          name = list.get(index);
        }
        else {
          prefix = null;
          name = index < 0 ? "UNEXPECTED" : list.get(index);
        }
      }
      else {
        prefix = null;
        name = value.toString();
      }
    }

    void setGroup(@NotNull Group group) {
      this.group = group;
      childrenValid = false;
      setIcon(group.getIcon());
    }

    @Nullable
    RootNode getSingleRoot() {
      Group group = this.group;
      return group == null ? null : group.getSingleRoot();
    }

    @Nullable
    @Override
    public VirtualFile getVirtualFile() {
      RootNode node = getSingleRoot();
      return node == null ? null : node.getVirtualFile();
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      presentation.setIcon(getIcon());
      if (prefix != null) presentation.addText(prefix, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      presentation.addText(name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      decorate(presentation);
    }

    @Nullable
    @Override
    String getLocation() {
      RootNode node = getSingleRoot();
      return node == null ? null : node.getTitle();
    }

    @NotNull
    @Override
    Collection<AbstractTreeNode> createChildren(@NotNull Collection<? extends AbstractTreeNode> old) {
      Group group = this.group;
      if (group == null) return emptyList();
      RootNode node = group.getSingleRoot();
      if (node == null) return group.createChildren(this, old);
      node.setParent(this);
      return node.getChildren();
    }

    @Override
    boolean contains(@NotNull VirtualFile file, @NotNull AreaInstance area) {
      // may be called from unexpected thread
      Group group = this.group;
      return group != null && group.contains(file, area);
    }

    @Override
    public boolean canNavigate() {
      Group group = this.group;
      RootNode node = group == null ? null : group.getFirstRoot();
      return node != null && node.canNavigate();
    }

    @Override
    public void navigate(boolean requestFocus) {
      Group group = this.group;
      RootNode node = group == null ? null : group.getFirstRoot();
      if (node != null) node.navigate(requestFocus);
    }

    @Override
    public String getNavigateActionText(boolean focusEditor) {
      return ActionsBundle.message("action.ModuleSettings.navigate");
    }

    @Override
    public int getTypeSortWeight(boolean sortByType) {
      return 2;
    }

    @Override
    public boolean equals(Object object) {
      return this == object;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @NotNull
    @Override
    public String toString() {
      return prefix != null ? prefix + name : name;
    }
  }


  private static final class Group {
    private final Object id;
    private final HashMap<Object, Group> groups = new HashMap<>();
    private final List<RootNode> roots = new SmartList<>();

    private Group(@NotNull String name) {
      id = name;
    }

    private Group(@NotNull Module module) {
      id = module;
    }

    Group(@NotNull Collection<RootNode> nodes, boolean flatten) {
      id = null;
      if (!nodes.isEmpty()) {
        HashMap<Module, Group> map = new HashMap<>();
        nodes.forEach(node -> {
          Object id = node.node.getRootID();
          if (id instanceof Module) {
            Module module = (Module)id;
            Group group = map.get(module);
            if (group == null) {
              group = new Group(module);
              map.put(module, group);
            }
            group.roots.add(node);
          }
          else {
            roots.add(node);
          }
        });
        if (flatten) {
          groups.putAll(map);
        }
        else {
          map.forEach((module, group) -> {
            List<String> path = getModuleNameAsList(module, Registry.is("project.qualified.module.names"));
            group.roots.forEach(node -> add(node, path, 0));
          });
        }
      }
    }

    private void add(RootNode node, List<String> path, int index) {
      if (index < path.size()) {
        String name = path.get(index);
        Group group = groups.get(name);
        if (group == null) {
          group = new Group(name);
          groups.put(name, group);
        }
        group.add(node, path, index + 1);
      }
      else {
        roots.add(node);
      }
    }

    @NotNull
    Icon getIcon() {
      if (!groups.isEmpty() || roots.isEmpty()) return AllIcons.Nodes.ModuleGroup;
      Object id = roots.get(0).node.getRootID();
      if (roots.stream().anyMatch(root -> !root.node.getRootID().equals(id))) return AllIcons.Nodes.ModuleGroup;
      if (id instanceof Module) {
        ModuleType type = ModuleType.get((Module)id);
        Icon icon = type.getIcon();
        if (icon != null) return icon;
        LOG.warn(type.getName() + " type have no icon for " + id);
      }
      return AllIcons.Nodes.Module;
    }

    @Nullable
    RootNode getFirstRoot() {
      if (!roots.isEmpty()) return roots.get(0);
      for (Group group : groups.values()) {
        RootNode root = group.getFirstRoot();
        if (root != null) return root;
      }
      return null;
    }

    @Nullable
    RootNode getSingleRoot() {
      if (!groups.isEmpty() || roots.size() != 1) return null;
      RootNode node = roots.get(0);
      ModuleRootManager manager = getModuleRootManager(getModule(node.getVirtualFile(), node.getProject()));
      if (manager == null) return null;
      // ensure that a content root is not a source root or test root
      for (VirtualFile file : manager.getSourceRoots()) {
        if (!isAncestor(node.getVirtualFile(), file, true)) return null;
      }
      return node;
    }

    @Nullable
    private Group getSingleGroup() {
      if (!roots.isEmpty() || groups.size() != 1) return null;
      return groups.values().stream().findFirst().orElse(null);
    }

    @NotNull
    Collection<AbstractTreeNode> createChildren(@NotNull Node parent, @NotNull Collection<? extends AbstractTreeNode> old) {
      Mapper<GroupNode, Object> mapper = new Mapper<>(GroupNode::new, GroupNode.class, old);
      ModuleManager manager = getModuleManager(parent.getProject());
      char separator = manager != null && manager.hasModuleGroups() ? VFS_SEPARATOR_CHAR : '.';
      boolean compactDirectories = parent.getSettings().isCompactDirectories();
      List<AbstractTreeNode> children = new SmartList<>();
      for (Group group : groups.values()) {
        Object id = group.id;
        Group single = !compactDirectories ? null : group.getSingleGroup();
        if (single != null) {
          StringBuilder sb = new StringBuilder(id.toString());
          do {
            group = single;
            sb.append(separator).append(group.id);
            single = single.getSingleGroup();
          }
          while (single != null);
          id = sb.toString();
        }
        GroupNode node = mapper.apply(parent, id);
        node.setGroup(group);
        children.add(node);
      }
      children.addAll(roots);
      return children;
    }

    boolean contains(@NotNull VirtualFile file, @NotNull AreaInstance area) {
      // may be called from unexpected thread
      return roots.stream().anyMatch(root -> root.canRepresentOrContain(file, area)) ||
             groups.values().stream().anyMatch(group -> group.contains(file, area));
    }
  }


  private static final class Mapper<N extends Node, V> {
    private final Map<Object, N> map;
    private final BiFunction<? super Node, ? super V, ? extends N> creator;

    Mapper(@NotNull BiFunction<? super Node, ? super V, ? extends N> creator, @NotNull Map<Object, N> map) {
      this.creator = creator;
      this.map = map;
    }

    Mapper(@NotNull BiFunction<? super Node, ? super V, ? extends N> creator, @NotNull Class<? extends N> type, @NotNull Collection<? extends AbstractTreeNode> list) {
      this(creator, new HashMap<>());
      list.forEach(node -> {
        Object id = node.getValue();
        if (id != null && type.isInstance(node)) map.put(id, type.cast(node));
      });
    }

    @NotNull
    N apply(@NotNull Node parent, @NotNull V value) {
      return apply(parent, value, null);
    }

    @NotNull
    N apply(@NotNull Node parent, @NotNull V value, @Nullable Icon icon) {
      N node = map.isEmpty() ? null : map.get(value);
      if (node == null) node = creator.apply(parent, value);
      node.setIcon(icon);
      node.childrenValid = false;
      return node;
    }
  }


  @Nullable
  private static WolfTheProblemSolver getWolfTheProblemSolver(@Nullable Project project) {
    return project == null || project.isDisposed() ? null : WolfTheProblemSolver.getInstance(project);
  }

  @Nullable
  private static FileStatusManager getFileStatusManager(@Nullable Project project) {
    return project == null || project.isDisposed() ? null : FileStatusManager.getInstance(project);
  }

  @Nullable
  private static ModuleManager getModuleManager(@Nullable Project project) {
    return project == null || project.isDisposed() ? null : ModuleManager.getInstance(project);
  }

  @Nullable
  private static ProjectFileIndex getProjectFileIndex(@Nullable Project project) {
    return project == null || project.isDisposed() ? null : ProjectFileIndex.getInstance(project);
  }

  @Nullable
  private static ModuleRootManager getModuleRootManager(@Nullable Module module) {
    return module == null || module.isDisposed() ? null : ModuleRootManager.getInstance(module);
  }

  @Nullable
  private static ProjectSettingsService getProjectSettingsService(@Nullable Project project) {
    return project == null || project.isDisposed() ? null : ProjectSettingsService.getInstance(project);
  }

  @Nullable
  private static Module getModule(@NotNull VirtualFile file, @Nullable Project project) {
    ProjectFileIndex index = getProjectFileIndex(project);
    return index == null ? null : index.getModuleForFile(file);
  }

  private static boolean hasModuleGroups(@Nullable Project project) {
    if (Registry.is("project.qualified.module.names")) return true;
    ModuleManager manager = getModuleManager(project);
    return manager != null && manager.hasModuleGroups();
  }

  @NotNull
  private static List<String> getModuleNameAsList(@NotNull Module module, boolean split) {
    String name = module.getName();
    Project project = module.isDisposed() ? null : module.getProject();
    ModuleManager manager = getModuleManager(project);
    if (manager != null) {
      if (manager.hasModuleGroups()) {
        String[] path = manager.getModuleGroupPath(module);
        if (path != null && path.length != 0) {
          List<String> list = new SmartList<>(path);
          list.add(name);
          return list;
        }
      }
      else if (split) {
        return StringUtil.split(name, ".");
      }
    }
    return new SmartList<>(name);
  }

  @Nullable
  private static List<VirtualFile> getCompactedFolders(@Nullable VirtualFile ancestor, @NotNull VirtualFile file) {
    if (ancestor == null || !isAncestor(ancestor, file, true)) return null;
    ArrayDeque<VirtualFile> deque = new ArrayDeque<>();
    while (true) {
      file = file.getParent();
      if (file == null || !isAncestor(ancestor, file, true)) break;
      deque.addFirst(file);
    }
    return deque.isEmpty() ? null : new SmartList<>(deque);
  }

  private static boolean isFolder(@Nullable Icon icon) {
    return is(icon, AllIcons.Nodes.Folder);
  }

  private static boolean isPackage(@Nullable Icon icon) {
    return is(icon, AllIcons.Nodes.Package);
  }

  private static boolean is(@Nullable Icon icon, @NotNull Icon expected) {
    if (expected.equals(icon)) return true;
    if (icon instanceof RowIcon) {
      RowIcon rowIcon = (RowIcon)icon;
      return expected.equals(rowIcon.getIcon(0));
    }
    return false;
  }
}
