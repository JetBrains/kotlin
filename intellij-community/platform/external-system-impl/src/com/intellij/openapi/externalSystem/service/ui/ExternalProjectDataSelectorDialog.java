// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.importing.ExternalProjectStructureCustomizer;
import com.intellij.openapi.externalSystem.importing.ExternalProjectStructureCustomizerImpl;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.Identifiable;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ModuleDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.util.BooleanValueHolder;
import com.intellij.util.CachedValueImpl;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class ExternalProjectDataSelectorDialog extends DialogWrapper {

  private static final int MAX_PATH_LENGTH = 50;
  private static final Set<? extends Key<?>> DATA_KEYS = ContainerUtil.set(ProjectKeys.PROJECT, ProjectKeys.MODULE);
  private static final com.intellij.openapi.util.Key<DataNode> MODIFIED_NODE_KEY = com.intellij.openapi.util.Key.create("modifiedData");
  private static final com.intellij.openapi.util.Key<DataNodeCheckedTreeNode> CONNECTED_UI_NODE_KEY =
    com.intellij.openapi.util.Key.create("connectedUiNode");
  @NotNull
  private final Project myProject;
  private JBLoadingPanel loadingPanel;
  private JPanel mainPanel;
  private JPanel contentPanel;
  @SuppressWarnings("unused")
  private JBLabel myDescriptionLbl;
  private JBLabel mySelectionStatusLbl;
  private ExternalSystemUiAware myExternalSystemUiAware;
  private ExternalProjectInfo myProjectInfo;
  private final Set<Key<?>> myIgnorableKeys;
  private final Set<Key<?>> myPublicKeys;
  private final Set<Key<? extends Identifiable>> myDependencyAwareDataKeys;
  @Nullable
  private final Object myPreselectedNodeObject;
  private CheckboxTree myTree;
  @SuppressWarnings("unchecked")
  private final MultiMap<DataNode<Identifiable>, DataNode<Identifiable>> dependentNodeMap =
    MultiMap.create(TObjectHashingStrategy.IDENTITY);

  private final SimpleModificationTracker myModificationTracker = new SimpleModificationTracker();
  private final CachedValue<SelectionState> selectionState = new CachedValueImpl<>(
    () -> CachedValueProvider.Result.createSingleDependency(getSelectionStatus(), myModificationTracker));

  private boolean myShowSelectedRowsOnly;
  private int myModulesCount;

  public ExternalProjectDataSelectorDialog(@NotNull Project project,
                                           @NotNull ExternalProjectInfo projectInfo) {
    this(project, projectInfo, null);
  }

  public ExternalProjectDataSelectorDialog(@NotNull Project project,
                                           @NotNull ExternalProjectInfo projectInfo,
                                           @Nullable Object preselectedNodeDataObject) {
    super(project, true);
    myProject = project;
    myIgnorableKeys = getIgnorableKeys();
    myPublicKeys = getPublicKeys();
    myDependencyAwareDataKeys = getDependencyAwareDataKeys();
    myPreselectedNodeObject = preselectedNodeDataObject;
    init(projectInfo);
  }

  private void init(@NotNull ExternalProjectInfo projectInfo) {
    myProjectInfo = projectInfo;
    myExternalSystemUiAware = ExternalSystemUiUtil.getUiAware(myProjectInfo.getProjectSystemId());
    myTree = createTree();
    updateSelectionState();

    myTree.addCheckboxTreeListener(new CheckboxTreeListener() {
      @Override
      public void nodeStateChanged(@NotNull CheckedTreeNode node) {
        updateSelectionState();
      }
    });

    String externalSystemName = myProjectInfo.getProjectSystemId().getReadableName();
    setTitle(String.format("%s Project Data To Import", externalSystemName));
    init();
  }

  public boolean hasMultipleDataToSelect() {
    return myModulesCount > 1;
  }

  private void updateSelectionState() {
    myModificationTracker.incModificationCount();
    mySelectionStatusLbl.setText(selectionState.getValue().message);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTree).
      addExtraAction(new SelectAllButton()).
      addExtraAction(new UnselectAllButton()).
      addExtraAction(new ShowSelectedOnlyButton()).
      addExtraAction(new SelectRequiredButton()).
      setToolbarPosition(ActionToolbarPosition.BOTTOM).
      setToolbarBorder(JBUI.Borders.empty());

    contentPanel.add(decorator.createPanel());
    loadingPanel = new JBLoadingPanel(new BorderLayout(), getDisposable());
    loadingPanel.add(mainPanel, BorderLayout.CENTER);
    return loadingPanel;
  }

  private void reloadTree() {
    final DefaultTreeModel treeModel = (DefaultTreeModel)myTree.getModel();
    final Object root = treeModel.getRoot();
    if (!(root instanceof CheckedTreeNode)) return;

    final CheckedTreeNode rootNode = (CheckedTreeNode)root;

    final Couple<CheckedTreeNode> rootAndPreselectedNode = createRoot();
    final CheckedTreeNode rootCopy = rootAndPreselectedNode.first;

    List<TreeNode> nodes = TreeUtil.listChildren(rootCopy);
    rootNode.removeAllChildren();
    TreeUtil.addChildrenTo(rootNode, nodes);
    treeModel.reload();
  }

  @Override
  protected void doOKAction() {
    loadingPanel.setLoadingText("Please wait...");
    loadingPanel.startLoading();

    final DataNode<ProjectData> projectStructure = myProjectInfo.getExternalProjectStructure();
    if (projectStructure != null) {
      final boolean[] isModified = {false};
      projectStructure.visit(node -> {
        final DataNode modifiedDataNode = node.getUserData(MODIFIED_NODE_KEY);
        if (modifiedDataNode != null) {
          if (node.isIgnored() != modifiedDataNode.isIgnored()) {
            node.setIgnored(modifiedDataNode.isIgnored());
            isModified[0] = true;
          }
          node.removeUserData(MODIFIED_NODE_KEY);
          node.removeUserData(CONNECTED_UI_NODE_KEY);
        }
      });
      if (isModified[0]) {
        DataNode<?> notIgnoredNode = ContainerUtil.find(projectStructure.getChildren(), node -> !node.isIgnored());
        projectStructure.setIgnored(notIgnoredNode == null);

        // execute when current dialog is closed
        ExternalSystemUtil.invokeLater(myProject, ModalityState.NON_MODAL, () -> {
          final ProjectData projectData = projectStructure.getData();
          String title = ExternalSystemBundle.message(
            "progress.refresh.text", projectData.getExternalName(), projectData.getOwner().getReadableName());
          new Task.Backgroundable(myProject, title, true, PerformInBackgroundOption.DEAF) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
              ServiceManager.getService(ProjectDataManager.class).importData(projectStructure, myProject, false);
            }
          }.queue();
        });
      }
    }

    super.doOKAction();
  }

  @Override
  public void doCancelAction() {
    ExternalSystemApiUtil.visit(myProjectInfo.getExternalProjectStructure(), node -> {
      node.removeUserData(MODIFIED_NODE_KEY);
      node.removeUserData(CONNECTED_UI_NODE_KEY);
    });

    super.doCancelAction();
  }

  private CheckboxTree createTree() {
    final Couple<CheckedTreeNode> rootAndPreselectedNode = createRoot();
    final CheckedTreeNode root = rootAndPreselectedNode.first;

    final CheckboxTree tree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer(true, false) {

      @Override
      public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (!(value instanceof DataNodeCheckedTreeNode)) {
          return;
        }
        final DataNodeCheckedTreeNode node = (DataNodeCheckedTreeNode)value;

        String tooltip = null;
        boolean hasErrors = false;
        if (node.isChecked()) {
          final Enumeration children = node.children();
          while (children.hasMoreElements()) {
            final Object o = children.nextElement();
            if (o instanceof DataNodeCheckedTreeNode && !((DataNodeCheckedTreeNode)o).isChecked()) {
              myCheckbox.setEnabled(false);
              break;
            }
          }

          if (myDependencyAwareDataKeys.contains(node.myDataNode.getKey())) {
            //noinspection unchecked
            final String listOfUncheckedDependencies =
              StringUtil
                .join(dependentNodeMap.get((DataNode<Identifiable>)node.myDataNode), depNode -> {
                  final DataNodeCheckedTreeNode uiNode = depNode.getUserData(CONNECTED_UI_NODE_KEY);
                  return uiNode != null && !uiNode.isChecked() ? depNode.getData().getId() : null;
                }, "<br>");
            if (StringUtil.isNotEmpty(listOfUncheckedDependencies)) {
              hasErrors = true;
              tooltip = "There are not selected module dependencies of the module: <br><b>" + listOfUncheckedDependencies + "</b>";
            }
          }
        }

        ColoredTreeCellRenderer renderer = getTextRenderer();

        renderer.setIcon(node.icon);
        renderer.append(node.text, hasErrors ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);

        if (!StringUtil.isEmptyOrSpaces(node.comment)) {
          String description = node.comment;
          if (node.comment.length() > MAX_PATH_LENGTH) {
            description = node.comment.substring(0, MAX_PATH_LENGTH) + "...";
          }

          renderer.append(" (" + description + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
          setToolTipText(StringUtil.isEmpty(tooltip) ? node.comment : tooltip);
        }
        else {
          setToolTipText(StringUtil.isNotEmpty(tooltip) ? tooltip : null);
        }
      }
    }, root, new CheckboxTreeBase.CheckPolicy(true, true, false, false));

    TreeUtil.expand(tree, 1);
    if (rootAndPreselectedNode.second != null) {
      TreeUtil.selectNode(tree, rootAndPreselectedNode.second);
    }
    else {
      tree.setSelectionRow(0);
    }
    return tree;
  }

  private Couple<CheckedTreeNode> createRoot() {
    final Map<DataNode, DataNodeCheckedTreeNode> treeNodeMap = ContainerUtil.newIdentityTroveMap();
    final Map<String, DataNode> ideGroupingMap = new TreeMap<>(); // need order for assigning parents

    final DataNodeCheckedTreeNode[] preselectedNode = {null};
    final DataNodeCheckedTreeNode[] rootModuleNode = {null};

    final MultiMap<String, String> moduleDependenciesMap = MultiMap.create();
    final Map<String, DataNode<Identifiable>> modulesNodeMap = new HashMap<>();

    for (DataNode<ModuleDependencyData> moduleDependencyDataNode : ExternalSystemApiUtil.findAllRecursively(
      myProjectInfo.getExternalProjectStructure(), ProjectKeys.MODULE_DEPENDENCY)) {
      final ModuleDependencyData moduleDependencyData = moduleDependencyDataNode.getData();
      moduleDependenciesMap.putValue(
        moduleDependencyData.getOwnerModule().getId(),
        moduleDependencyData.getTarget().getId());
    }

    final int[] modulesCount = {0};

    ExternalSystemApiUtil.visit(myProjectInfo.getExternalProjectStructure(), node -> {
      final Key key = node.getKey();
      if (!myPublicKeys.contains(key)) return;

      DataNode modifiableDataNode = getModifiableDataNode(node);

      if (myDependencyAwareDataKeys.contains(key)) {
        modulesCount[0]++;
      }

      if (modifiableDataNode.isIgnored() && myShowSelectedRowsOnly) return;

      DataNodeCheckedTreeNode treeNode = treeNodeMap.get(node);
      if (treeNode == null) {
        treeNode = new DataNodeCheckedTreeNode(node);

        if (myDependencyAwareDataKeys.contains(key)) {
          final Identifiable moduleData = (Identifiable)node.getData();
          //noinspection unchecked
          modulesNodeMap.put(moduleData.getId(), (DataNode<Identifiable>)node);
        }

        if (myPreselectedNodeObject != null && myPreselectedNodeObject.equals(node.getData())) {
          preselectedNode[0] = treeNode;
        }
        if (node.getData() instanceof ModuleData) {
          ModuleData moduleData = (ModuleData)node.getData();
          if (key.equals(ProjectKeys.MODULE) && myProjectInfo.getExternalProjectPath().equals(moduleData.getLinkedExternalProjectPath())) {
            rootModuleNode[0] = treeNode;
          }
          String ideGrouping = moduleData.getIdeGrouping();
          if (ideGrouping != null) {
            ideGroupingMap.put(ideGrouping, node);
          }
        } else {
          // add elements under module node like web/enterprise artifacts
          DataNode<ModuleData> parentModule = node.getParent(ModuleData.class);
          if(parentModule != null) {
            DataNodeCheckedTreeNode moduleTreeNode = treeNodeMap.get(parentModule);
            if(moduleTreeNode != null) {
              moduleTreeNode.add(treeNode);
              treeNode.setParent(moduleTreeNode);
            }
          }
        }
        treeNode.setEnabled(myIgnorableKeys.contains(key));
        treeNodeMap.put(node, treeNode);
      }
    });

    for (Map.Entry<String, DataNode> groupingEntry : ideGroupingMap.entrySet()) {
      DataNode node = groupingEntry.getValue();
      if (!(node.getData() instanceof ModuleData)) continue;
      ModuleData moduleData = (ModuleData)node.getData();
      String ideParentGrouping = moduleData.getIdeParentGrouping();
      DataNode structuralParent = ideParentGrouping != null ? ideGroupingMap.get(ideParentGrouping) : null;
      DataNodeCheckedTreeNode treeParentNode = structuralParent != null ? treeNodeMap.get(structuralParent) : null;

      DataNodeCheckedTreeNode treeNode = treeNodeMap.get(node);

      if (treeParentNode == null) {
        treeParentNode = treeNodeMap.get(node.getParent());
      }

      if (treeNode == null || treeParentNode == null) continue;

      treeParentNode.add(treeNode);
      treeNode.setParent(treeParentNode);
    }

    myModulesCount = modulesCount[0];

    dependentNodeMap.clear();
    for (String moduleId : moduleDependenciesMap.keySet()) {
      final Collection<String> moduleDependencies = moduleDependenciesMap.get(moduleId);
      final DataNode<Identifiable> moduleNode = modulesNodeMap.get(moduleId);
      if (moduleNode != null) {
        dependentNodeMap.putValues(moduleNode, ContainerUtil.mapNotNull(moduleDependencies, modulesNodeMap::get));
      }
    }

    final CheckedTreeNode root = new CheckedTreeNode(null);
    final DataNodeCheckedTreeNode projectNode = treeNodeMap.get(myProjectInfo.getExternalProjectStructure());

    String rootModuleComment = "root module";
    if (rootModuleNode[0] != null && projectNode != null) {
      rootModuleNode[0].comment = rootModuleComment;
      if (!projectNode.isNodeChild(rootModuleNode[0])) {
        projectNode.add(rootModuleNode[0]);
      }
    }

    List<TreeNode> nodes = projectNode != null ? TreeUtil.listChildren(projectNode) : ContainerUtil.emptyList();
    Collections.sort(nodes, (o1, o2) -> {
      if(o1 instanceof DataNodeCheckedTreeNode && o2 instanceof DataNodeCheckedTreeNode) {
        if (rootModuleComment.equals(((DataNodeCheckedTreeNode)o1).comment)) return -1;
        if (rootModuleComment.equals(((DataNodeCheckedTreeNode)o2).comment)) return 1;
        return StringUtil.naturalCompare(((DataNodeCheckedTreeNode)o1).text, ((DataNodeCheckedTreeNode)o2).text);
      }
      return 0;
    });
    TreeUtil.addChildrenTo(root, nodes);
    return Couple.of(root, preselectedNode[0]);
  }

  @NotNull
  private static Set<Key<?>> getPublicKeys() {
    Set<Key<?>> result = new HashSet<>(DATA_KEYS);
    for (ExternalProjectStructureCustomizer customizer : ExternalProjectStructureCustomizer.EP_NAME.getExtensions()) {
      result.addAll(customizer.getPublicDataKeys());
    }
    return result;
  }

  @NotNull
  private static Set<Key<?>> getIgnorableKeys() {
    Set<Key<?>> result = new HashSet<>(DATA_KEYS);
    for (ExternalProjectStructureCustomizer customizer : ExternalProjectStructureCustomizer.EP_NAME.getExtensions()) {
      result.addAll(customizer.getIgnorableDataKeys());
    }
    return result;
  }

  @NotNull
  private static Set<Key<? extends Identifiable>> getDependencyAwareDataKeys() {
    Set<Key<? extends Identifiable>> result = new HashSet<>();
    result.add(ProjectKeys.MODULE);
    for (ExternalProjectStructureCustomizer customizer : ExternalProjectStructureCustomizer.EP_NAME.getExtensions()) {
      result.addAll(customizer.getDependencyAwareDataKeys());
    }
    return result;
  }

  private class DataNodeCheckedTreeNode extends CheckedTreeNode {
    private static final int MAX_DEPENDENCIES_TO_DESCRIBE = 5;

    private final DataNode myDataNode;
    @Nullable
    private final Icon icon;
    private String text;
    @Nullable
    private String comment;

    private DataNodeCheckedTreeNode(DataNode node) {
      super(node);
      myDataNode = node;
      node.putUserData(CONNECTED_UI_NODE_KEY, this);
      DataNode modifiableDataNode = (DataNode)node.getUserData(MODIFIED_NODE_KEY);
      assert modifiableDataNode != null;
      isChecked = !modifiableDataNode.isIgnored();

      Icon anIconCandidate = null;
      boolean multipleIconCandidatesFound = false;
      ExternalProjectStructureCustomizer projectStructureCustomizer = new ExternalProjectStructureCustomizerImpl();
      for (ExternalProjectStructureCustomizer customizer : ExternalProjectStructureCustomizer.EP_NAME.getExtensions()) {
        Icon icon = customizer.suggestIcon(node, myExternalSystemUiAware);
        if (!multipleIconCandidatesFound && icon != null) {
          if (anIconCandidate != null) {
            multipleIconCandidatesFound = true;
            anIconCandidate = null;
          }
          else {
            anIconCandidate = icon;
          }
        }

        if (customizer.getPublicDataKeys().contains(node.getKey())) {
          projectStructureCustomizer = customizer;
          break;
        }
      }

      icon = anIconCandidate != null ? anIconCandidate : projectStructureCustomizer.suggestIcon(node, myExternalSystemUiAware);
      final Couple<String> representationName = projectStructureCustomizer.getRepresentationName(node);
      text = representationName.first;
      comment = representationName.second;

      if (text == null) {
        text = node.getKey().toString();
      }
    }

    @Override
    public void setChecked(final boolean checked) {
      super.setChecked(checked);
      if (checked) {
        DataNodeCheckedTreeNode parent = this;
        DataNodeCheckedTreeNode moduleNode = null;
        while (parent.parent instanceof DataNodeCheckedTreeNode) {
          if (moduleNode == null && (myDependencyAwareDataKeys.contains(parent.myDataNode.getKey()))) {
            moduleNode = parent;
          }
          parent = (DataNodeCheckedTreeNode)parent.parent;
        }
        parent.isChecked = true;

        final DataNode modifiedParentDataNode = getModifiableDataNode(parent.myDataNode);
        modifiedParentDataNode.setIgnored(false);

        if (moduleNode != null) {
          moduleNode.isChecked = true;
        }
        ExternalSystemApiUtil.visit(moduleNode == null ? myDataNode : moduleNode.myDataNode, node -> getModifiableDataNode(node).setIgnored(false));
      }
      else {
        ExternalSystemApiUtil.visit(myDataNode, node -> getModifiableDataNode(node).setIgnored(true));
        if (myShowSelectedRowsOnly) {
          final DefaultTreeModel treeModel = (DefaultTreeModel)myTree.getModel();
          TreePath[] before = myTree.getSelectionPaths();
          treeModel.removeNodeFromParent(this);
          myTree.addSelectionPaths(before);
        }
      }
      if (!checked && parent instanceof DataNodeCheckedTreeNode) {
        if (myDataNode.getKey().equals(ProjectKeys.MODULE) &&
            ((DataNodeCheckedTreeNode)parent).myDataNode.getKey().equals(ProjectKeys.PROJECT)) {
          final DataNode projectDataNode = ((DataNodeCheckedTreeNode)parent).myDataNode;
          final ProjectData projectData = (ProjectData)projectDataNode.getData();
          final ModuleData moduleData = (ModuleData)myDataNode.getData();
          if (moduleData.getLinkedExternalProjectPath().equals(projectData.getLinkedExternalProjectPath())) {
            if (ExternalSystemApiUtil.findAll(projectDataNode, ProjectKeys.MODULE).size() == 1) {
              ((DataNodeCheckedTreeNode)parent).setChecked(false);
            }
          }
        }
      }

      DataNodeCheckedTreeNode[] unprocessedNodes = myTree.getSelectedNodes(
        DataNodeCheckedTreeNode.class, node -> myDependencyAwareDataKeys.contains(node.myDataNode.getKey()) && checked != node.isChecked());

      boolean isCheckCompleted = unprocessedNodes.length == 0 && myDependencyAwareDataKeys.contains(myDataNode.getKey());

      updateSelectionState();

      if (selectionState.getValue().isRequiredSelectionEnabled && isCheckCompleted) {
        warnAboutMissedDependencies(checked);
      }
    }

    private void warnAboutMissedDependencies(boolean checked) {
      List<DataNode<Identifiable>> selectedModules = ContainerUtil.newSmartList();
      for (DataNode node : TreeUtil.collectSelectedObjectsOfType(myTree, DataNode.class)) {
        if (myDependencyAwareDataKeys.contains(node.getKey())) {
          //noinspection unchecked
          selectedModules.add(node);
        }
      }

      final Set<DataNode<Identifiable>> deps = new HashSet<>();
      for (DataNode<Identifiable> selectedModule : selectedModules) {
        if (checked) {
          deps.addAll(ContainerUtil.filter(dependentNodeMap.get(selectedModule), node -> {
            final DataNodeCheckedTreeNode uiNode = node.getUserData(CONNECTED_UI_NODE_KEY);
            return uiNode != null && !uiNode.isChecked();
          }));
        }
        else {
          for (DataNode<Identifiable> node : dependentNodeMap.keySet()) {
            final DataNodeCheckedTreeNode uiNode = node.getUserData(CONNECTED_UI_NODE_KEY);
            if (uiNode != null && !uiNode.isChecked()) continue;

            Collection<DataNode<Identifiable>> dependencies = dependentNodeMap.get(node);
            if (dependencies.contains(selectedModule)) {
              deps.add(node);
            }
          }
        }
      }

      if (!deps.isEmpty() && !selectedModules.isEmpty()) {
        final String message = checked ? getEnableMessage(selectedModules, deps) : getDisableMessage(deps);
        if (Messages.showOkCancelDialog(message, checked ? "Enable Dependant Modules" : "Disable Modules with Dependency on this",
                                        Messages.getQuestionIcon()) == Messages.OK) {
          List<DataNodeCheckedTreeNode> nodes =
            ContainerUtil.mapNotNull(deps, node -> node.getUserData(CONNECTED_UI_NODE_KEY));

          for (DataNodeCheckedTreeNode node : nodes) {
            DefaultTreeModel treeModel = (DefaultTreeModel)myTree.getModel();
            myTree.addSelectionPath(new TreePath(treeModel.getPathToRoot(node)));
          }
          for (DataNodeCheckedTreeNode node : nodes) {
            node.setChecked(checked);
          }
        }
      }
    }

    private String getEnableMessage(List<? extends DataNode<Identifiable>> selectedModules, Set<? extends DataNode<Identifiable>> deps) {
      if (deps.size() > MAX_DEPENDENCIES_TO_DESCRIBE || selectedModules.size() > MAX_DEPENDENCIES_TO_DESCRIBE) {
        return String.format(
          "%d disabled %s depend on %d selected %s. Would you like to enable %s too?",
          deps.size(), StringUtil.pluralize("module", deps.size()),
          selectedModules.size(), StringUtil.pluralize("module", selectedModules.size()),
          deps.size() == 1 ? "it" : "them");
      }

      final String listOfSelectedModules = StringUtil.join(selectedModules, node -> node.getData().getId(), ", ");

      final String listOfDependencies = StringUtil.join(deps, node -> node.getData().getId(), "<br>");
      return String.format(
        "<html>The following %s on which <b>%s</b> %s %s disabled:<br><b>%s</b><br>Would you like to enable %s?</html>",
        StringUtil.pluralize("module", deps.size()), listOfSelectedModules,
        StringUtil.pluralize("depend", selectedModules.size()), deps.size() == 1 ? "is" : "are",
        listOfDependencies, deps.size() == 1 ? "it" : "them");
    }

    private String getDisableMessage(Set<? extends DataNode<Identifiable>> deps) {
      if (deps.size() > MAX_DEPENDENCIES_TO_DESCRIBE) {
        return String.format("%d enabled modules depend on selected modules. Would you like to disable them too?", deps.size());
      }

      final String listOfDependencies = StringUtil.join(deps, node -> node.getData().getId(), "<br>");
      return String.format(
        "<html>The following %s <br><b>%s</b><br>%s enabled and %s on selected modules. <br>Would you like to disable %s too?</html>",
        StringUtil.pluralize("module", deps.size()), listOfDependencies, deps.size() == 1 ? "is" : "are",
        StringUtil.pluralize("depend", deps.size()),
        deps.size() == 1 ? "it" : "them");
    }
  }

  @NotNull
  private static DataNode getModifiableDataNode(@NotNull DataNode node) {
    DataNode modifiedDataNode = (DataNode)node.getUserData(MODIFIED_NODE_KEY);
    if (modifiedDataNode == null) {
      modifiedDataNode = node.nodeCopy();
      node.putUserData(MODIFIED_NODE_KEY, modifiedDataNode);
    }
    return modifiedDataNode;
  }

  private SelectionState getSelectionStatus() {
    boolean isRequiredSelectionEnabled = computeRequiredSelectionStatus();

    String stateMessage = "";
    final Object root = myTree.getModel().getRoot();
    if (root instanceof CheckedTreeNode) {

      final int[] selectedModulesCount = {0};

      TreeUtil.traverse((CheckedTreeNode)root, node -> {
        if (node instanceof DataNodeCheckedTreeNode &&
            ((DataNodeCheckedTreeNode)node).isChecked() &&
            myDependencyAwareDataKeys.contains((((DataNodeCheckedTreeNode)node).myDataNode.getKey()))) {
          selectedModulesCount[0]++;
        }
        return true;
      });
      stateMessage = String.format("%1$d Modules. %2$d selected", myModulesCount, selectedModulesCount[0]);
    }

    return new SelectionState(isRequiredSelectionEnabled, stateMessage);
  }

  private boolean computeRequiredSelectionStatus() {
    for (DataNode<Identifiable> node : dependentNodeMap.keySet()) {
      final DataNodeCheckedTreeNode uiNode = node.getUserData(CONNECTED_UI_NODE_KEY);
      assert uiNode != null;
      if (!uiNode.isChecked()) continue;

      for (DataNode<Identifiable> depNode : dependentNodeMap.get(node)) {
        final DataNodeCheckedTreeNode uiDependentNode = depNode.getUserData(CONNECTED_UI_NODE_KEY);
        assert uiDependentNode != null;
        if (!uiDependentNode.isChecked()) return true;
      }
    }
    return false;
  }

  private static class SelectionState {
    boolean isRequiredSelectionEnabled;
    @Nullable String message;

    SelectionState(boolean isRequiredSelectionEnabled, @Nullable String message) {
      this.isRequiredSelectionEnabled = isRequiredSelectionEnabled;
      this.message = message;
    }
  }

  private class SelectAllButton extends AnActionButton {
    SelectAllButton() {
      super("Select All", AllIcons.Actions.Selectall);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      final DefaultTreeModel treeModel = (DefaultTreeModel)myTree.getModel();
      final Object root = treeModel.getRoot();
      if (!(root instanceof CheckedTreeNode)) return;

      if (!myShowSelectedRowsOnly) {
        myTree.setNodeState((CheckedTreeNode)root, true);
      }
      else {
        myShowSelectedRowsOnly = false;
        reloadTree();
        myTree.setNodeState((CheckedTreeNode)root, true);
        myShowSelectedRowsOnly = true;
      }
    }
  }

  private class UnselectAllButton extends AnActionButton {
    UnselectAllButton() {
      super("Unselect All", AllIcons.Actions.Unselectall);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      final DefaultTreeModel treeModel = (DefaultTreeModel)myTree.getModel();
      final Object root = treeModel.getRoot();
      if (!(root instanceof CheckedTreeNode)) return;

      if (!myShowSelectedRowsOnly) {
        myTree.setNodeState((CheckedTreeNode)root, false);
      }
      else {
        myShowSelectedRowsOnly = false;
        reloadTree();
        myTree.setNodeState((CheckedTreeNode)root, false);
        myShowSelectedRowsOnly = true;
        reloadTree();
      }
    }
  }

  private class ShowSelectedOnlyButton extends ToggleActionButton {

    ShowSelectedOnlyButton() {
      super("Show Selected Only", AllIcons.Actions.ShowHiddens);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myShowSelectedRowsOnly;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myShowSelectedRowsOnly = state;
      reloadTree();
    }
  }

  private class SelectRequiredButton extends AnActionButton {
    SelectRequiredButton() {
      super("Select Required", "select modules depended on currently selected modules", AllIcons.Actions.IntentionBulb);

      addCustomUpdater(e -> selectionState.getValue().isRequiredSelectionEnabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      boolean showSelectedRowsOnly = myShowSelectedRowsOnly;
      if (showSelectedRowsOnly) {
        myShowSelectedRowsOnly = false;
        reloadTree();
      }

      myTree.clearSelection();
      for (DataNode<Identifiable> node : dependentNodeMap.keySet()) {
        final DataNodeCheckedTreeNode uiNode = node.getUserData(CONNECTED_UI_NODE_KEY);
        assert uiNode != null;
        if (!uiNode.isChecked()) continue;

        for (DataNode<Identifiable> treeNode : dependentNodeMap.get(node)) {
          final DataNodeCheckedTreeNode uiDependentNode = treeNode.getUserData(CONNECTED_UI_NODE_KEY);
          assert uiDependentNode != null;
          myTree.setNodeState(uiDependentNode, true);
        }
      }

      if (showSelectedRowsOnly) {
        myShowSelectedRowsOnly = true;
        reloadTree();
      }
      updateSelectionState();
    }

    @Override
    public boolean displayTextInToolbar() {
      return true;
    }
  }

  @Override
  public boolean showAndGet() {
    final BooleanValueHolder result = new BooleanValueHolder(false);
    DumbService.getInstance(myProject).suspendIndexingAndRun(
      "Select External Data",
      () -> result.setValue(super.showAndGet())
    );
    return result.getValue();
  }
}
