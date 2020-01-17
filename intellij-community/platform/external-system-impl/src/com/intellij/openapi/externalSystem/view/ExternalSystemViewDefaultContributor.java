// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.view;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.model.project.dependencies.*;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.project.IdeModelsProviderImpl;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import gnu.trove.TLongObjectHashMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.InputEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Supplier;

import static com.intellij.openapi.externalSystem.model.ProjectKeys.PROJECT;

/**
 * @author Vladislav.Soroka
 */
public class ExternalSystemViewDefaultContributor extends ExternalSystemViewContributor {

  private static final Key<?>[] KEYS = new Key[]{
    ProjectKeys.MODULE,
    ProjectKeys.DEPENDENCIES_GRAPH,
    ProjectKeys.MODULE_DEPENDENCY,
    ProjectKeys.LIBRARY_DEPENDENCY,
    ProjectKeys.TASK
  };

  @NotNull
  @Override
  public ProjectSystemId getSystemId() {
    return ProjectSystemId.IDE;
  }

  @NotNull
  @Override
  public List<Key<?>> getKeys() {
    return Arrays.asList(KEYS);
  }

  @Override
  @NotNull
  public List<ExternalSystemNode<?>> createNodes(final ExternalProjectsView externalProjectsView,
                                                 final MultiMap<Key<?>, DataNode<?>> dataNodes) {
    final List<ExternalSystemNode<?>> result = new SmartList<>();

    addModuleNodes(externalProjectsView, dataNodes, result);
    // add tasks
    Collection<DataNode<?>> tasksNodes = dataNodes.get(ProjectKeys.TASK);
    if (!tasksNodes.isEmpty()) {
      TasksNode tasksNode = new TasksNode(externalProjectsView, tasksNodes);
      if (externalProjectsView.useTasksNode()) {
        result.add(tasksNode);
      }
      else {
        ContainerUtil.addAll(result, tasksNode.getChildren());
      }
    }

    addDependenciesNode(externalProjectsView, dataNodes, result);

    return result;
  }

  @Nullable
  @Override
  public String getDisplayName(@NotNull DataNode node) {
    return getNodeDisplayName(node);
  }

  private static void addDependenciesNode(@NotNull ExternalProjectsView externalProjectsView,
                                          @NotNull MultiMap<Key<?>, DataNode<?>> dataNodes,
                                          @NotNull List<? super ExternalSystemNode<?>> result) {
    final Collection<DataNode<?>> depsGraph = dataNodes.get(ProjectKeys.DEPENDENCIES_GRAPH);
    if (!depsGraph.isEmpty()) {
      final ExternalSystemNode<?> depNode = new MyDependenciesNode(externalProjectsView);
      for (DataNode<?> dataNode : depsGraph) {
        if (!(dataNode.getData() instanceof ProjectDependencies)) continue;

        ProjectDependencies projectDependencies = (ProjectDependencies)dataNode.getData();
        for (ComponentDependencies componentDependencies : projectDependencies.getComponentsDependencies()) {
          depNode.add(new DependencyScopeExternalSystemNode(externalProjectsView, componentDependencies.getCompileDependenciesGraph()));
          depNode.add(new DependencyScopeExternalSystemNode(externalProjectsView, componentDependencies.getRuntimeDependenciesGraph()));
        }
      }

      if (depNode.hasChildren()) {
        result.add(depNode);
      }
      return;
    }

    final Collection<DataNode<?>> moduleDeps = dataNodes.get(ProjectKeys.MODULE_DEPENDENCY);
    final Collection<DataNode<?>> libDeps = dataNodes.get(ProjectKeys.LIBRARY_DEPENDENCY);

    if (!moduleDeps.isEmpty() || !libDeps.isEmpty()) {
      final ExternalSystemNode<?> depNode = new MyDependenciesNode(externalProjectsView);
      boolean addDepNode = false;

      for (DataNode<?> dataNode : moduleDeps) {
        if (!(dataNode.getData() instanceof ModuleDependencyData)) continue;
        //noinspection unchecked
        ModuleDependencyDataExternalSystemNode moduleDependencyDataExternalSystemNode =
          new ModuleDependencyDataExternalSystemNode(externalProjectsView, (DataNode<ModuleDependencyData>)dataNode);
        if (dataNode.getParent() != null && dataNode.getParent().getData() instanceof AbstractDependencyData) {
          result.add(moduleDependencyDataExternalSystemNode);
        }
        else {
          depNode.add(moduleDependencyDataExternalSystemNode);
          addDepNode = true;
        }
      }

      for (DataNode<?> dataNode : libDeps) {
        if (!(dataNode.getData() instanceof LibraryDependencyData)) continue;
        //noinspection unchecked
        final ExternalSystemNode<LibraryDependencyData> libraryDependencyDataExternalSystemNode =
          new LibraryDependencyDataExternalSystemNode(externalProjectsView, (DataNode<LibraryDependencyData>)dataNode);
        if (((LibraryDependencyData)dataNode.getData()).getTarget().isUnresolved()) {
          libraryDependencyDataExternalSystemNode.setErrorLevel(
            ExternalProjectsStructure.ErrorLevel.ERROR,
            "Unable to resolve " + ((LibraryDependencyData)dataNode.getData()).getTarget().getExternalName());
        }
        else {
          libraryDependencyDataExternalSystemNode.setErrorLevel(ExternalProjectsStructure.ErrorLevel.NONE);
        }
        if (dataNode.getParent() != null && dataNode.getParent().getData() instanceof ModuleData) {
          depNode.add(libraryDependencyDataExternalSystemNode);
          addDepNode = true;
        }
        else {
          result.add(libraryDependencyDataExternalSystemNode);
        }
      }

      if (addDepNode) {
        result.add(depNode);
      }
    }
  }

  private static void addModuleNodes(@NotNull ExternalProjectsView externalProjectsView,
                                     @NotNull MultiMap<Key<?>, DataNode<?>> dataNodes,
                                     @NotNull List<? super ExternalSystemNode<?>> result) {
    final Collection<DataNode<?>> moduleDataNodes = dataNodes.get(ProjectKeys.MODULE);
    if (!moduleDataNodes.isEmpty()) {
      final AbstractExternalSystemSettings systemSettings =
        ExternalSystemApiUtil.getSettings(externalProjectsView.getProject(), externalProjectsView.getSystemId());

      final Map<String, ModuleNode> groupToModule = new HashMap<>(moduleDataNodes.size());

      List<ModuleNode> moduleNodes = new ArrayList<>();

      for (DataNode<?> dataNode : moduleDataNodes) {
        final ModuleData data = (ModuleData)dataNode.getData();

        final ExternalProjectSettings projectSettings = systemSettings.getLinkedProjectSettings(data.getLinkedExternalProjectPath());
        DataNode<ProjectData> projectDataNode = ExternalSystemApiUtil.findParent(dataNode, PROJECT);
        final boolean isRoot =
          projectSettings != null && data.getLinkedExternalProjectPath().equals(projectSettings.getExternalProjectPath()) &&
          projectDataNode != null && projectDataNode.getData().getInternalName().equals(data.getInternalName());
        //noinspection unchecked
        final ModuleNode moduleNode = new ModuleNode(externalProjectsView, (DataNode<ModuleData>)dataNode, null, isRoot);
        moduleNodes.add(moduleNode);

        String group = moduleNode.getIdeGrouping();
        if (group != null) {
          groupToModule.put(group, moduleNode);
        }
      }

      for (ModuleNode moduleNode : moduleNodes) {
        moduleNode.setAllModules(moduleNodes);
        String parentGroup = moduleNode.getIdeParentGrouping();
        ModuleNode parent = parentGroup != null ? groupToModule.get(parentGroup) : null;
        if (parent == null) {
          continue;
        }
        moduleNode.setParent(parent);
      }

      result.addAll(moduleNodes);
    }
  }

  @Order(ExternalSystemNode.BUILTIN_DEPENDENCIES_DATA_NODE_ORDER)
  private static class MyDependenciesNode extends ExternalSystemNode<Object> {
    MyDependenciesNode(ExternalProjectsView externalProjectsView) {
      super(externalProjectsView, null, null);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      presentation.setIcon(AllIcons.Nodes.PpLibFolder);
    }

    @Override
    public String getName() {
      return "Dependencies";
    }
  }

  private static class DependencyScopeExternalSystemNode extends ExternalSystemNode<Object> {
    private final DependencyScopeNode myDependenciesGraph;
    private final TLongObjectHashMap<DependencyNode> myDependencyNodeMap = new TLongObjectHashMap<>();

    DependencyScopeExternalSystemNode(@NotNull ExternalProjectsView externalProjectsView,
                                      @NotNull DependencyScopeNode dependenciesGraph) {
      super(externalProjectsView, null);
      myDependenciesGraph = dependenciesGraph;
    }

    @Override
    public String getName() {
      return myDependenciesGraph.getScope();
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      presentation.setIcon(AllIcons.Nodes.PpLibFolder);
      String description = myDependenciesGraph.getDescription();
      if (description != null) {
        setNameAndTooltip(getName(), description, (String)null);
      }
    }

    @NotNull
    @Override
    protected List<ExternalSystemNode<?>> doBuildChildren() {
      buildNodesMap(myDependencyNodeMap, myDependenciesGraph);
      List<ExternalSystemNode<?>> myChildNodes = new ArrayList<>();
      for (DependencyNode dependency : myDependenciesGraph.getDependencies()) {
        myChildNodes.add(new DependencyExternalSystemNode(getExternalProjectsView(), dependency, myDependencyNodeMap));
      }
      return myChildNodes;
    }

    @Override
    public ExternalProjectsStructure.ErrorLevel getChildrenErrorLevel() {
      return getErrorLevelRecursively(myDependenciesGraph);
    }

    @NotNull
    private static ExternalProjectsStructure.ErrorLevel getErrorLevelRecursively(@NotNull DependencyNode node) {
      if (node instanceof UnknownDependencyNode) {
        return ExternalProjectsStructure.ErrorLevel.ERROR;
      }
      ExternalProjectsStructure.ErrorLevel result = ExternalProjectsStructure.ErrorLevel.NONE;
      for (DependencyNode dependencyNode : node.getDependencies()) {
        ExternalProjectsStructure.ErrorLevel eachLevel = getErrorLevelRecursively(dependencyNode);
        if (result.compareTo(eachLevel) > 0) result = eachLevel;
        if (result == ExternalProjectsStructure.ErrorLevel.ERROR) break;
      }
      return result;
    }

    private static void buildNodesMap(@NotNull TLongObjectHashMap<DependencyNode> dependencyNodeMap, @NotNull DependencyNode node) {
      for (DependencyNode child : node.getDependencies()) {
        if (child instanceof ReferenceNode) continue;
        dependencyNodeMap.put(child.getId(), child);
        buildNodesMap(dependencyNodeMap, child);
      }
    }
  }

  private static class DependencyExternalSystemNode extends ExternalSystemNode<Object> {
    @NotNull
    private final DependencyNode myDependencyNode;
    @NotNull
    private final TLongObjectHashMap<DependencyNode> myDependencyNodeMap;
    @Nullable
    private DependencyNode myReferencedNode;
    private final String myName;

    DependencyExternalSystemNode(@NotNull ExternalProjectsView externalProjectsView,
                                 @NotNull DependencyNode dependencyNode,
                                 @NotNull TLongObjectHashMap<DependencyNode> dependencyNodeMap) {
      super(externalProjectsView, null);
      myDependencyNode = dependencyNode;
      myDependencyNodeMap = dependencyNodeMap;
      if (myDependencyNode instanceof ReferenceNode) {
        myReferencedNode = myDependencyNodeMap.get(myDependencyNode.getId());
      }
      if (myReferencedNode != null) {
        myName = MessageFormat.format("{0}{1}",
                                      myReferencedNode.getDisplayName(),
                                      myReferencedNode.getDependencies().isEmpty() ? "" : " (*)");
      }
      else {
        myName = myDependencyNode.getDisplayName();
      }
    }

    @Override
    public String getName() {
      return myName;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      boolean isProjectDependency = myDependencyNode instanceof ProjectDependencyNode || myReferencedNode instanceof ProjectDependencyNode;
      presentation.setIcon(isProjectDependency ? getUiAware().getProjectIcon() : AllIcons.Nodes.PpLib);
      String tooltip;
      if (myReferencedNode != null) {
        tooltip = ExternalSystemBundle.message("external.system.view.nodes.dependency_reference_node_tooltip");
      }
      else {
        if (myDependencyNode instanceof FileCollectionDependencyNode) {
          String path = ((FileCollectionDependencyNode)myDependencyNode).getPath();
          tooltip = StringUtil.join(path.split(File.pathSeparator), s -> StringEscapeUtils.escapeHtml(s) + "<br/>" , "");
        }
        else {
          tooltip = null;
        }
      }
      setNameAndTooltip(getName(), tooltip, (String)null);
    }

    @NotNull
    @Override
    protected List<ExternalSystemNode<?>> doBuildChildren() {
      if (myReferencedNode != null) {
        return Collections.emptyList();
      }
      List<ExternalSystemNode<?>> myChildNodes = new ArrayList<>();
      for (DependencyNode dependency : myDependencyNode.getDependencies()) {
        myChildNodes.add(new DependencyExternalSystemNode(getExternalProjectsView(), dependency, myDependencyNodeMap));
      }
      return myChildNodes;
    }

    @Override
    public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
      if (myReferencedNode == null) {
        super.handleDoubleClickOrEnter(tree, inputEvent);
      }
      else {
        selectAndExpandReferencedNode();
      }
    }

    private void selectAndExpandReferencedNode() {
      ExternalProjectsStructure structure = getStructure();
      if (structure == null) return;
      DependencyScopeExternalSystemNode scopeNode = findParent(DependencyScopeExternalSystemNode.class);
      if (scopeNode == null) return;

      DependencyExternalSystemNode referencedNode = findReferencedNode(scopeNode);
      if (referencedNode != null) {
        structure.select(referencedNode);
        structure.expand(referencedNode);
      }
    }

    @Nullable
    private DependencyExternalSystemNode findReferencedNode(ExternalSystemNode<?> node) {
      for (ExternalSystemNode<?> child : node.getChildren()) {
        if (child instanceof DependencyExternalSystemNode &&
            ((DependencyExternalSystemNode)child).myDependencyNode == myReferencedNode) {
          return (DependencyExternalSystemNode)child;
        }
        DependencyExternalSystemNode referencedNode = findReferencedNode(child);
        if (referencedNode != null) return referencedNode;
      }
      return null;
    }
  }

  private static abstract class DependencyDataExternalSystemNode<T extends DependencyData<?>> extends ExternalSystemNode<T> {

    private final Navigatable myNavigatable;

    DependencyDataExternalSystemNode(@NotNull ExternalProjectsView externalProjectsView,
                                     @Nullable ExternalSystemNode parent,
                                     @Nullable DataNode<T> dataNode) {
      super(externalProjectsView, parent, dataNode);
      myNavigatable = new OrderEntryNavigatable(getProject(), () -> getOrderEntry());
    }

    @Nullable
    @Override
    public Navigatable getNavigatable() {
      return myNavigatable;
    }

    @Nullable
    private OrderEntry getOrderEntry() {
      final T data = getData();
      if (data == null) return null;
      final Project project = getProject();
      if (project == null) return null;
      return new IdeModelsProviderImpl(project).findIdeModuleOrderEntry(data);
    }

    @Override
    public int compareTo(@NotNull ExternalSystemNode node) {
      final T myData = getData();
      final Object thatData = node.getData();
      if (myData instanceof OrderAware && thatData instanceof OrderAware) {
        int order1 = ((OrderAware)myData).getOrder();
        int order2 = ((OrderAware)thatData).getOrder();
        if (order1 != order2) {
          return order1 < order2 ? -1 : 1;
        }
      }

      String dependencyName = getDependencySimpleName(this);
      String thatDependencyName = getDependencySimpleName(node);
      return StringUtil.compare(dependencyName, thatDependencyName, true);
    }

    @NotNull
    private static String getDependencySimpleName(@NotNull ExternalSystemNode<?> node) {
      Object thatData = node.getData();
      if (thatData instanceof LibraryDependencyData) {
        LibraryDependencyData dependencyData = (LibraryDependencyData)thatData;
        String externalName = dependencyData.getExternalName();
        if (StringUtil.isEmpty(externalName)) {
          Set<String> paths = dependencyData.getTarget().getPaths(LibraryPathType.BINARY);
          if (paths.size() == 1) {
            return new File(paths.iterator().next()).getName();
          }
        }
      }
      return node.getName();
    }

    private static class OrderEntryNavigatable implements Navigatable {
      @NotNull private final Supplier<OrderEntry> myProvider;
      @Nullable private final Project myProject;
      @Nullable private OrderEntry myOrderEntry;

      OrderEntryNavigatable(@Nullable Project project,
                            @NotNull Supplier<OrderEntry> provider) {
        myProject = project;
        myProvider = provider;
      }

      @Override
      public void navigate(boolean requestFocus) {
        if (myOrderEntry != null && myProject != null) {
          ProjectSettingsService.getInstance(myProject).openModuleDependenciesSettings(myOrderEntry.getOwnerModule(), myOrderEntry);
        }
      }

      @Override
      public boolean canNavigate() {
        myOrderEntry = myProvider.get();
        return myOrderEntry != null;
      }

      @Override
      public boolean canNavigateToSource() {
        return true;
      }
    }
  }

  private static class ModuleDependencyDataExternalSystemNode extends DependencyDataExternalSystemNode<ModuleDependencyData> {

    ModuleDependencyDataExternalSystemNode(ExternalProjectsView externalProjectsView, DataNode<ModuleDependencyData> dataNode) {
      super(externalProjectsView, null, dataNode);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      presentation.setIcon(getUiAware().getProjectIcon());

      final ModuleDependencyData data = getData();
      if (data != null) {
        setNameAndTooltip(getName(), null, data.getScope().getDisplayName());
      }
    }

    @NotNull
    @Override
    protected List<? extends ExternalSystemNode<?>> doBuildChildren() {
      return Collections.emptyList();
    }

    @Override
    public ExternalProjectsStructure.ErrorLevel getChildrenErrorLevel() {
      return ExternalProjectsStructure.ErrorLevel.NONE;
    }
  }

  private static class LibraryDependencyDataExternalSystemNode extends DependencyDataExternalSystemNode<LibraryDependencyData> {

    LibraryDependencyDataExternalSystemNode(ExternalProjectsView externalProjectsView, DataNode<LibraryDependencyData> dataNode) {
      super(externalProjectsView, null, dataNode);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      presentation.setIcon(AllIcons.Nodes.PpLib);

      final LibraryDependencyData data = getData();
      if (data != null) {
        setNameAndTooltip(getName(), null, data.getScope().getDisplayName());
      }
    }
  }

  @NotNull
  private static String getNodeDisplayName(@NotNull DataNode<?> node) {
    Object data = node.getData();
    if (data instanceof LibraryDependencyData) {
      LibraryDependencyData libraryDependencyData = (LibraryDependencyData)data;
      String externalName = libraryDependencyData.getExternalName();
      if (StringUtil.isEmpty(externalName)) {
        Set<String> paths = libraryDependencyData.getTarget().getPaths(LibraryPathType.BINARY);
        if (paths.size() == 1) {
          String relativePathToRoot = null;
          String path = ExternalSystemApiUtil.toCanonicalPath(paths.iterator().next());
          DataNode<ProjectData> projectDataDataNode = ExternalSystemApiUtil.findParent(node, PROJECT);
          if (projectDataDataNode != null) {
            relativePathToRoot = FileUtil.getRelativePath(projectDataDataNode.getData().getLinkedExternalProjectPath(), path, '/');
            relativePathToRoot = relativePathToRoot != null && StringUtil.startsWith(relativePathToRoot, "../../")
                                 ? new File(relativePathToRoot).getName()
                                 : relativePathToRoot;
          }
          return ObjectUtils.notNull(relativePathToRoot, path);
        }
        else {
          return "<file set>";
        }
      }
      return externalName;
    }
    if (data instanceof Named) {
      return ((Named)data).getExternalName();
    }
    if (data instanceof TaskData) {
      return ((TaskData)data).getName();
    }
    return StringUtil.notNullize(node.toString());
  }

  @Override
  public ExternalProjectsStructure.ErrorLevel getErrorLevel(DataNode<?> dataNode) {
    if (ProjectKeys.LIBRARY_DEPENDENCY.equals(dataNode.getKey())) {
      if (((LibraryDependencyData)dataNode.getData()).getTarget().isUnresolved()) {
        return ExternalProjectsStructure.ErrorLevel.ERROR;
      }
    }
    return super.getErrorLevel(dataNode);
  }
}
