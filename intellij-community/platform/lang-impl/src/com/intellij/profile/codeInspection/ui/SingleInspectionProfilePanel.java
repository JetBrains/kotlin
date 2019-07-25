// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.profile.codeInspection.ui;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.BaseInspectionProfileManager;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.profile.codeInspection.ui.filter.InspectionFilterAction;
import com.intellij.profile.codeInspection.ui.filter.InspectionsFilter;
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionConfigTreeNode;
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeComparator;
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeRenderer;
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeTable;
import com.intellij.profile.codeInspection.ui.table.ScopesAndSeveritiesTable;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.ui.*;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.Alarm;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.*;

public class SingleInspectionProfilePanel extends JPanel {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.ex.InspectionToolsPanel");
  @NonNls private static final String INSPECTION_FILTER_HISTORY = "INSPECTION_FILTER_HISTORY";
  @NonNls private static final String EMPTY_HTML = "<html><body></body></html>";

  private static final float DIVIDER_PROPORTION_DEFAULT = 0.5f;
  public static final String SETTINGS = "settings://";

  private final Map<HighlightDisplayKey, ToolDescriptors> myInitialToolDescriptors = new THashMap<>();
  private final InspectionConfigTreeNode myRoot = new InspectionConfigTreeNode.Group(InspectionsBundle.message("inspection.root.node.title"));
  private final Alarm myAlarm = new Alarm();
  private final ProjectInspectionProfileManager myProjectProfileManager;
  @NotNull
  private final InspectionProfileModifiableModel myProfile;
  private JEditorPane myBrowser;
  private JPanel myOptionsPanel;
  private JPanel myInspectionProfilePanel;
  private FilterComponent myProfileFilter;
  private final InspectionsFilter myInspectionsFilter = new InspectionsFilter() {
    @Override
    protected void filterChanged() {
      filterTree();
    }
  };
  private boolean myModified;
  private InspectionsConfigTreeTable myTreeTable;
  private TreeExpander myTreeExpander;
  private boolean myIsInRestore;

  private String[] myInitialScopesOrder;
  private Disposable myDisposable = Disposer.newDisposable();

  public SingleInspectionProfilePanel(@NotNull ProjectInspectionProfileManager projectProfileManager,
                                      @NotNull InspectionProfileModifiableModel profile) {
    super(new BorderLayout());
    myProjectProfileManager = projectProfileManager;
    myProfile = profile;
    // to ensure that profile initialized with proper project
    myProfile.initInspectionTools(projectProfileManager.getProject());
  }

  public boolean differsFromDefault() {
    return myRoot.isProperSetting();
  }

  public void performProfileReset() {
    //forcibly initialize configs to be able compare xmls after reset
    TreeUtil.treeNodeTraverser(myRoot).traverse().processEach(n -> {
      InspectionConfigTreeNode node = (InspectionConfigTreeNode)n;
      if (node instanceof InspectionConfigTreeNode.Tool && node.isProperSetting()) {
        ((InspectionConfigTreeNode.Tool)node).getDefaultDescriptor().loadConfig();
      }
      return true;
    });
    getProfile().resetToBase(myProjectProfileManager.getProject());
    loadDescriptorsConfigs(true);
    postProcessModification();
    updateModificationMarker();
    myRoot.dropCache();
  }

  private static VisibleTreeState getExpandedNodes(InspectionProfileImpl profile) {
    if (profile.isProjectLevel()) {
      return ProjectInspectionProfilesVisibleTreeState.getInstance(((ProjectInspectionProfileManager)profile.getProfileManager()).getProject()).getVisibleTreeState(profile);
    }
    return AppInspectionProfilesVisibleTreeState.getInstance().getVisibleTreeState(profile);
  }

  private static InspectionConfigTreeNode findGroupNodeByPath(@NotNull String[] path, int idx, @NotNull InspectionConfigTreeNode node) {
    if (path.length == idx) {
      return node;
    }

    final String currentKey = path[idx];
    for (int i = 0; i < node.getChildCount(); i++) {
      final InspectionConfigTreeNode currentNode = (InspectionConfigTreeNode)node.getChildAt(i);
      if (currentNode instanceof InspectionConfigTreeNode.Group && ((InspectionConfigTreeNode.Group)currentNode).getGroupName().equals(currentKey)) {
        return findGroupNodeByPath(path, ++idx, currentNode);
      }
    }

    return null;
  }

  @Nullable
  private static InspectionConfigTreeNode findNodeByKey(String name, InspectionConfigTreeNode root) {
    for (int i = 0; i < root.getChildCount(); i++) {
      final InspectionConfigTreeNode child = (InspectionConfigTreeNode)root.getChildAt(i);
      if (child instanceof InspectionConfigTreeNode.Tool) {
        if (((InspectionConfigTreeNode.Tool)child).getKey().toString().equals(name)) {
          return child;
        }
      }
      else {
        final InspectionConfigTreeNode node = findNodeByKey(name, child);
        if (node != null) return node;
      }
    }
    return null;
  }

  public static String renderSeverity(HighlightSeverity severity) {
    if (HighlightSeverity.INFORMATION.equals(severity)) return "No highlighting, only fix"; //todo severity presentation
    return StringUtil.capitalizeWords(StringUtil.toLowerCase(severity.getName()), true);
  }

  private static boolean isDescriptorAccepted(Descriptor descriptor,
                                              @NonNls String filter,
                                              final boolean forceInclude,
                                              final List<Set<String>> keySetList, final Set<String> quoted) {
    filter = StringUtil.toLowerCase(filter);
    if (StringUtil.containsIgnoreCase(descriptor.getText(), filter)) {
      return true;
    }
    final String[] groupPath = descriptor.getGroup();
    for (String group : groupPath) {
      if (StringUtil.containsIgnoreCase(group, filter)) {
        return true;
      }
    }
    for (String stripped : quoted) {
      if (StringUtil.containsIgnoreCase(descriptor.getText(),stripped)) {
        return true;
      }
      for (String group : groupPath) {
        if (StringUtil.containsIgnoreCase(group,stripped)) {
          return true;
        }
      }
      final String description = descriptor.getToolWrapper().loadDescription();
      if (description != null && StringUtil.containsIgnoreCase(StringUtil.toLowerCase(description), stripped)) {
        if (!forceInclude) return true;
      } else if (forceInclude) return false;
    }
    for (Set<String> keySet : keySetList) {
      if (keySet.contains(descriptor.getKey().toString())) {
        if (!forceInclude) {
          return true;
        }
      }
      else {
        if (forceInclude) {
          return false;
        }
      }
    }
    return forceInclude;
  }

  private static void setConfigPanel(final JPanel configPanelAnchor, final ScopeToolState state) {
    configPanelAnchor.removeAll();
    final JComponent additionalConfigPanel = state.getAdditionalConfigPanel();
    if (additionalConfigPanel != null) {
      // assume that the panel does not need scrolling if it already contains a scrollable content
      if (UIUtil.hasScrollPane(additionalConfigPanel)) {
        configPanelAnchor.add(additionalConfigPanel);
      }
      else {
        configPanelAnchor.add(ScrollPaneFactory.createScrollPane(additionalConfigPanel, SideBorder.NONE));
      }
    }
  }

  private static InspectionConfigTreeNode getGroupNode(InspectionConfigTreeNode root, String[] groupPath) {
    InspectionConfigTreeNode currentRoot = root;
    for (final String group : groupPath) {
      currentRoot = getGroupNode(currentRoot, group);
    }
    return currentRoot;
  }

  private static InspectionConfigTreeNode getGroupNode(InspectionConfigTreeNode root, String group) {
    final int childCount = root.getChildCount();
    for (int i = 0; i < childCount; i++) {
      InspectionConfigTreeNode child = (InspectionConfigTreeNode)root.getChildAt(i);
      if (group.equals(child.getUserObject())) {
        return child;
      }
    }
    InspectionConfigTreeNode child = new InspectionConfigTreeNode.Group(group);
    root.add(child);
    return child;
  }

  private static void copyUsedSeveritiesIfUndefined(InspectionProfileImpl selectedProfile, BaseInspectionProfileManager profileManager) {
    final SeverityRegistrar registrar = profileManager.getSeverityRegistrar();
    final Set<HighlightSeverity> severities = selectedProfile.getUsedSeverities();
    severities.removeIf(severity -> registrar.isSeverityValid(severity.getName()));

    if (!severities.isEmpty()) {
      final SeverityRegistrar oppositeRegister = selectedProfile.getProfileManager().getSeverityRegistrar();
      for (HighlightSeverity severity : severities) {
        final TextAttributesKey attributesKey = TextAttributesKey.find(severity.getName());
        final TextAttributes textAttributes = oppositeRegister.getTextAttributesBySeverity(severity);
        if (textAttributes == null) {
          continue;
        }
        HighlightInfoType.HighlightInfoTypeImpl info = new HighlightInfoType.HighlightInfoTypeImpl(severity, attributesKey);
        registrar.registerSeverity(new SeverityRegistrar.SeverityBasedTextAttributes(textAttributes.clone(), info),
                                   textAttributes.getErrorStripeColor());
      }
    }
  }

  private void initUI() {
    myInspectionProfilePanel = createInspectionProfileSettingsPanel();
    add(myInspectionProfilePanel, BorderLayout.CENTER);
    UserActivityWatcher userActivityWatcher = new UserActivityWatcher();
    userActivityWatcher.addUserActivityListener(() -> {
      //invoke after all other listeners
      ApplicationManager.getApplication().invokeLater(() -> {
        if (isDisposed()) return; //panel was disposed
        updateProperSettingsForSelection();
        updateModificationMarker();
      });
    });
    userActivityWatcher.register(myOptionsPanel);
    updateSelectedProfileState();
    reset();
  }

  private void updateSelectedProfileState() {
    if (isDisposed()) return;
    restoreTreeState();
    repaintTableData();
    updateSelection();
  }

  public void updateSelection() {
    if (myTreeTable != null) {
      final TreePath selectionPath = myTreeTable.getTree().getSelectionPath();
      if (selectionPath != null) {
        TreeUtil.selectNode(myTreeTable.getTree(), (TreeNode) selectionPath.getLastPathComponent());
        final int rowForPath = myTreeTable.getTree().getRowForPath(selectionPath);
        TableUtil.selectRows(myTreeTable, new int[]{rowForPath});
        scrollToCenter();
      }
    }
  }

  private void loadDescriptorsConfigs(boolean onlyModified) {
    myInitialToolDescriptors.values().stream().flatMap(ToolDescriptors::getDescriptors).forEach(d -> {
      if (!onlyModified || myProfile.isProperSetting(d.getKey().toString())) {
        d.loadConfig();
      }
    });
  }

  private void updateModificationMarker() {
    myModified = myInitialToolDescriptors.values().stream().flatMap(ToolDescriptors::getDescriptors).anyMatch(descriptor -> {
      Element oldConfig = descriptor.getConfig();
      if (oldConfig == null) return false;
      ScopeToolState state = descriptor.getState();
      Element newConfig = Descriptor.createConfigElement(state.getTool());
      if (!JDOMUtil.areElementsEqual(oldConfig, newConfig)) {
        myAlarm.cancelAllRequests();
        myAlarm.addRequest(() -> myTreeTable.repaint(), 300);
        return true;
      }
      return false;
    });
  }

  private void updateProperSettingsForSelection() {
    final TreePath selectionPath = myTreeTable.getTree().getSelectionPath();
    if (selectionPath != null) {
      InspectionConfigTreeNode node = (InspectionConfigTreeNode)selectionPath.getLastPathComponent();
      if (node instanceof InspectionConfigTreeNode.Tool) {
        final boolean properSetting = myProfile.isProperSetting(((InspectionConfigTreeNode.Tool)node).getKey().toString());
        if (node.isProperSetting() != properSetting) {
          myAlarm.cancelAllRequests();
          myAlarm.addRequest(() -> myTreeTable.repaint(), 300);
          InspectionConfigTreeNode.updateUpHierarchy(node);
        }
      }
    }
  }

  private void initToolStates() {
    if (isDisposed()) return;

    myInitialToolDescriptors.clear();
    final Project project = myProjectProfileManager.getProject();
    for (final ScopeToolState state : myProfile.getDefaultStates(myProjectProfileManager.getProject())) {
      if (!accept(state.getTool())) {
        continue;
      }
      ToolDescriptors descriptors = ToolDescriptors.fromScopeToolState(state, myProfile, project);
      myInitialToolDescriptors.put(descriptors.getDefaultDescriptor().getKey(), descriptors);
    }
    myInitialScopesOrder = myProfile.getScopesOrder();
  }

  private boolean isDisposed() {
    return myDisposable == null;
  }

  protected boolean accept(InspectionToolWrapper entry) {
    return entry.getDefaultLevel() != HighlightDisplayLevel.NON_SWITCHABLE_ERROR;
  }

  private void postProcessModification() {
    updateModificationMarker();
    //resetup configs
    for (ScopeToolState state : myProfile.getAllTools()) {
      state.resetConfigPanel();
    }
    fillTreeData(myProfileFilter.getFilter(), true);
    repaintTableData();
    updateOptionsAndDescriptionPanel();
  }

  public void setFilter(String filter) {
    myProfileFilter.setFilter(filter);
  }

  private void filterTree() {
    String filter = myProfileFilter != null ? myProfileFilter.getFilter() : null;
    if (myTreeTable != null) {
      getExpandedNodes(myProfile).saveVisibleState(myTreeTable.getTree());
      fillTreeData(filter, true);
      reloadModel();
      restoreTreeState();
      if (myTreeTable.getTree().getSelectionPath() == null) {
        TreeUtil.promiseSelectFirst(myTreeTable.getTree());
      }
    }
  }

  private void reloadModel() {
    try {
      myIsInRestore = true;
      ((DefaultTreeModel)myTreeTable.getTree().getModel()).reload();
    }
    finally {
      myIsInRestore = false;
    }

  }

  private void restoreTreeState() {

    try {
      myIsInRestore = true;
      getExpandedNodes(myProfile).restoreVisibleState(myTreeTable.getTree());
    }
    finally {
      myIsInRestore = false;
    }
  }

  private ActionToolbar createTreeToolbarPanel() {
    final CommonActionsManager actionManager = CommonActionsManager.getInstance();

    DefaultActionGroup actions = new DefaultActionGroup();

    actions.add(new InspectionFilterAction(myProfile, myInspectionsFilter, myProjectProfileManager.getProject(), myProfileFilter));
    actions.addSeparator();

    actions.add(actionManager.createExpandAllAction(myTreeExpander, myTreeTable));
    actions.add(actionManager.createCollapseAllAction(myTreeExpander, myTreeTable));
    actions.add(new DumbAwareAction("Reset to Empty", "Reset to empty", AllIcons.Actions.Unselectall) {
      @Override
      public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!isDisposed() && myProfile.isExecutable(myProjectProfileManager.getProject()));
      }

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        myProfile.resetToEmpty(myProjectProfileManager.getProject());
        loadDescriptorsConfigs(false);
        postProcessModification();
      }
    });

    final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("SingleInspectionProfile", actions, true);
    actionToolbar.setTargetComponent(this);
    return actionToolbar;
  }

  private void repaintTableData() {
    if (myTreeTable != null) {
      getExpandedNodes(myProfile).saveVisibleState(myTreeTable.getTree());
      reloadModel();
      restoreTreeState();
    }
  }

  public void selectInspectionTool(String name) {
    selectNode(findNodeByKey(name, myRoot));
  }

  public void selectInspectionGroup(String[] path) {
    final InspectionConfigTreeNode node = findGroupNodeByPath(path, 0, myRoot);
    selectNode(node);
    if (node != null) {
      myTreeTable.getTree().expandPath(new TreePath(node.getPath()));
    }
  }

  private void selectNode(InspectionConfigTreeNode node) {
    if (node != null) {
      TreeUtil.selectNode(myTreeTable.getTree(), node);
      final int rowForPath = myTreeTable.getTree().getRowForPath(new TreePath(node.getPath()));
      TableUtil.selectRows(myTreeTable, new int[]{rowForPath});
      scrollToCenter();
    }
  }

  private void scrollToCenter() {
    ListSelectionModel selectionModel = myTreeTable.getSelectionModel();
    int maxSelectionIndex = selectionModel.getMaxSelectionIndex();
    final int maxColumnSelectionIndex = Math.max(0, myTreeTable.getColumnModel().getSelectionModel().getMinSelectionIndex());
    Rectangle maxCellRect = myTreeTable.getCellRect(maxSelectionIndex, maxColumnSelectionIndex, false);

    final Point selectPoint = maxCellRect.getLocation();
    final int allHeight = myTreeTable.getVisibleRect().height;
    myTreeTable.scrollRectToVisible(new Rectangle(new Point(0, Math.max(0, selectPoint.y - allHeight / 2)), new Dimension(0, allHeight)));
  }

  private JScrollPane initTreeScrollPane() {
    fillTreeData(null, true);

    final InspectionsConfigTreeRenderer renderer = new InspectionsConfigTreeRenderer(){
      @Override
      protected String getFilter() {
        return myProfileFilter != null ? myProfileFilter.getFilter() : null;
      }
    };
    myTreeTable = InspectionsConfigTreeTable.create(new InspectionsConfigTreeTable.InspectionsConfigTreeTableSettings(myRoot, myProjectProfileManager.getProject()) {
      @Override
      protected void onChanged(@NotNull final InspectionConfigTreeNode node) {
        InspectionConfigTreeNode.updateUpHierarchy(node);
      }

      @Override
      public void updateRightPanel() {
        updateOptionsAndDescriptionPanel();
      }

      @Override
      @NotNull
      public InspectionProfileImpl getInspectionProfile() {
        return myProfile;
      }
    }, myDisposable);
    myTreeTable.setTreeCellRenderer(renderer);
    myTreeTable.setRootVisible(false);
    TreeUtil.installActions(myTreeTable.getTree());


    myTreeTable.getTree().addTreeSelectionListener(__ -> {
      if (myTreeTable.getTree().getSelectionPaths() != null) {
        updateOptionsAndDescriptionPanel();
      }
      else {
        initOptionsAndDescriptionPanel();
      }

      if (!myIsInRestore) {
        if (!isDisposed()) {
          InspectionProfileImpl baseProfile = myProfile.getSource();
          getExpandedNodes(baseProfile).setSelectionPaths(myTreeTable.getTree().getSelectionPaths());
          getExpandedNodes(myProfile).setSelectionPaths(myTreeTable.getTree().getSelectionPaths());
        }
      }
    });


    myTreeTable.addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        final int[] selectionRows = myTreeTable.getTree().getSelectionRows();
        if (selectionRows != null &&
            myTreeTable.getTree().getPathForLocation(x, y) != null &&
            Arrays.binarySearch(selectionRows, myTreeTable.getTree().getRowForLocation(x, y)) > -1) {
          compoundPopup().show(comp, x, y);
        }
      }
    });


    new TreeSpeedSearch(myTreeTable.getTree(), o -> {
      final InspectionConfigTreeNode node = (InspectionConfigTreeNode)o.getLastPathComponent();
      return InspectionsConfigTreeComparator.getDisplayTextToSort(node.getText());
    });


    final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTreeTable);
    myTreeTable.getTree().setShowsRootHandles(true);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM + SideBorder.LEFT + SideBorder.TOP));
    TreeUtil.collapseAll(myTreeTable.getTree(), 1);

    myTreeTable.getTree().addTreeExpansionListener(new TreeExpansionListener() {


      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        InspectionProfileModifiableModel selected = myProfile;
        getExpandedNodes(selected.getSource()).saveVisibleState(myTreeTable.getTree());
        getExpandedNodes(selected).saveVisibleState(myTreeTable.getTree());
      }

      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        if (!isDisposed()) {
          final InspectionConfigTreeNode node = (InspectionConfigTreeNode)event.getPath().getLastPathComponent();
          getExpandedNodes(myProfile.getSource()).expandNode(node);
          getExpandedNodes(myProfile).expandNode(node);
        }
      }
    });

    myTreeExpander = new DefaultTreeExpander(myTreeTable.getTree()) {
      @Override
      public boolean canExpand() {
        return myTreeTable.isShowing();
      }

      @Override
      public boolean canCollapse() {
        return myTreeTable.isShowing();
      }
    };
    myProfileFilter = new MyFilterComponent();

    return scrollPane;
  }

  private JPopupMenu compoundPopup() {
    final DefaultActionGroup group = new DefaultActionGroup();
    final SeverityRegistrar severityRegistrar = myProfile.getProfileManager().getSeverityRegistrar();
    for (HighlightSeverity severity : LevelChooserAction.getSeverities(severityRegistrar, includeDoNotShow())) {
      final HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
      group.add(new AnAction(renderSeverity(severity), renderSeverity(severity), level.getIcon()) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          setNewHighlightingLevel(level);
        }

        @Override
        public boolean isDumbAware() {
          return true;
        }
      });
    }
    group.add(Separator.getInstance());
    ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
    return menu.getComponent();
  }

  private boolean includeDoNotShow() {
    final TreePath[] paths = myTreeTable.getTree().getSelectionPaths();
    if (paths == null) return true;
    return includeDoNotShow(myTreeTable.getSelectedToolNodes());
  }

  private boolean includeDoNotShow(Collection<InspectionConfigTreeNode.Tool> nodes) {
    final Project project = myProjectProfileManager.getProject();
    return nodes
      .stream()
      .noneMatch(node -> {
        final InspectionToolWrapper tool = myProfile.getToolDefaultState(node.getKey().toString(), project).getTool();
        return tool instanceof GlobalInspectionToolWrapper && ((GlobalInspectionToolWrapper)tool).getSharedLocalInspectionToolWrapper() == null;
      });
  }

  private void fillTreeData(@Nullable String filter, boolean forceInclude) {
    if (isDisposed()) return;
    myRoot.removeAllChildren();
    myRoot.dropCache();
    List<Set<String>> keySetList = new ArrayList<>();
    final Set<String> quoted = new HashSet<>();
    if (filter != null && !filter.isEmpty()) {
      keySetList.addAll(SearchUtil.findKeys(filter, quoted));
    }
    Project project = myProjectProfileManager.getProject();
    final boolean emptyFilter = myInspectionsFilter.isEmptyFilter();
    for (ToolDescriptors toolDescriptors : myInitialToolDescriptors.values()) {
      final Descriptor descriptor = toolDescriptors.getDefaultDescriptor();
      if (filter != null && !filter.isEmpty() && !isDescriptorAccepted(descriptor, filter, forceInclude, keySetList, quoted)) {
        continue;
      }
      final InspectionConfigTreeNode node = new InspectionConfigTreeNode.Tool(() -> myInitialToolDescriptors.get(toolDescriptors.getDefaultDescriptor().getKey()));
      if (!emptyFilter && !myInspectionsFilter.matches(
        myProfile.getTools(toolDescriptors.getDefaultDescriptor().getKey().toString(), project), node)) {
        continue;
      }
      getGroupNode(myRoot, toolDescriptors.getDefaultDescriptor().getGroup()).add(node);
    }
    if (filter != null && forceInclude && myRoot.getChildCount() == 0) {
      final Set<String> filters = SearchableOptionsRegistrar.getInstance().getProcessedWords(filter);
      if (filters.size() > 1 || !quoted.isEmpty()) {
        fillTreeData(filter, false);
      }
    }
    TreeUtil.sortRecursively(myRoot, InspectionsConfigTreeComparator.INSTANCE);
  }

  // TODO 134099: see IntentionDescriptionPanel#readHTML
  public static void readHTML(JEditorPane browser, String text) {
    try {
      browser.read(new StringReader(text), null);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO 134099: see IntentionDescriptionPanel#setHTML
  public static String toHTML(JEditorPane browser, String text, boolean miniFontSize) {
    final HintHint hintHint = new HintHint(browser, new Point(0, 0));
    hintHint.setFont(miniFontSize ? UIUtil.getLabelFont(UIUtil.FontSize.SMALL) : UIUtil.getLabelFont());
    return HintUtil.prepareHintText(text, hintHint);
  }

  private void updateOptionsAndDescriptionPanel() {
    if (isDisposed()) {
      return;
    }
    Collection<InspectionConfigTreeNode.Tool> nodes = myTreeTable.getSelectedToolNodes();
    if (!nodes.isEmpty()) {
      final InspectionConfigTreeNode.Tool singleNode = myTreeTable.getStrictlySelectedToolNode();
      if (singleNode != null) {
        final Descriptor descriptor = singleNode.getDefaultDescriptor();
        if (descriptor.loadDescription() != null) {
          // need this in order to correctly load plugin-supplied descriptions
          final Descriptor defaultDescriptor = singleNode.getDefaultDescriptor();
          final String description = defaultDescriptor.loadDescription();
          try {
            readHTML(myBrowser, SearchUtil.markup(toHTML(myBrowser, description, false), myProfileFilter.getFilter()));
          }
          catch (Throwable t) {
            LOG.error("Failed to load description for: " +
                      defaultDescriptor.getToolWrapper().getTool().getClass() +
                      "; description: " +
                      description, t);
          }

        }
        else {
          readHTML(myBrowser, toHTML(myBrowser, "Can't find inspection description.", false));
        }
      }
      else {
        readHTML(myBrowser, toHTML(myBrowser, "Multiple inspections are selected. You can edit them as a single inspection.", false));
      }

      myOptionsPanel.removeAll();
      final Project project = myProjectProfileManager.getProject();
      final JPanel severityPanel = new JPanel(new GridBagLayout());
      final JPanel configPanelAnchor = new JPanel(new GridLayout());

      final Set<String> scopesNames = new THashSet<>();
      for (final InspectionConfigTreeNode.Tool node : nodes) {
        final List<ScopeToolState> nonDefaultTools = myProfile.getNonDefaultTools(node.getDefaultDescriptor().getKey().toString(), project);
        for (final ScopeToolState tool : nonDefaultTools) {
          scopesNames.add(tool.getScopeName());
        }
      }

      final double severityPanelWeightY;
      ScopesAndSeveritiesTable scopesAndScopesAndSeveritiesTable;
      if (scopesNames.isEmpty()) {

        final LevelChooserAction severityLevelChooser =
          new LevelChooserAction(myProfile.getProfileManager().getSeverityRegistrar(),
                                 includeDoNotShow(nodes)) {
            @Override
            protected void onChosen(final HighlightSeverity severity) {
              final HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
              final List<InspectionConfigTreeNode.Tool> toUpdate = new SmartList<>();
              for (final InspectionConfigTreeNode.Tool node : nodes) {
                final HighlightDisplayKey key = node.getDefaultDescriptor().getKey();
                final NamedScope scope = node.getDefaultDescriptor().getScope();
                final boolean doUpdate = myProfile.getErrorLevel(key, scope, project) != level;
                if (doUpdate) {
                  myProfile.setErrorLevel(key, level, null, project);
                  toUpdate.add(node);
                }
              }
              updateRecursively(toUpdate, false);
              myTreeTable.updateUI();
            }
          };
        final HighlightSeverity severity =
          ScopesAndSeveritiesTable.getSeverity(ContainerUtil.map(nodes, node -> node.getDefaultDescriptor().getState()));
        severityLevelChooser.setChosen(severity);

        final ScopesChooser scopesChooser = new ScopesChooser(ContainerUtil.map(nodes, node -> node.getDefaultDescriptor()), myProfile, project, null) {
          @Override
          protected void onScopesOrderChanged() {
            updateRecursively(nodes, true);
          }

          @Override
          protected void onScopeAdded(@NotNull String scopeName) {
            updateRecursively(nodes, true);
          }
        };

        severityPanel.add(new JLabel(InspectionsBundle.message("inspection.severity")),
                          new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                                 JBInsets.create(10, 0), 0, 0));
        final JComponent severityLevelChooserComponent = severityLevelChooser.createCustomComponent(
          severityLevelChooser.getTemplatePresentation(), ActionPlaces.UNKNOWN);
        severityPanel.add(severityLevelChooserComponent,
                          new GridBagConstraints(1, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                                                 JBInsets.create(10, 0), 0, 0));
        final JComponent scopesChooserComponent = scopesChooser.createCustomComponent(
          scopesChooser.getTemplatePresentation(), ActionPlaces.UNKNOWN);
        severityPanel.add(scopesChooserComponent,
                          new GridBagConstraints(2, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                                                 JBInsets.create(10, 0), 0, 0));
        final JLabel label = new JLabel("", SwingConstants.RIGHT);
        severityPanel.add(label,
                          new GridBagConstraints(3, 0, 1, 1, 1, 0,
                                                 GridBagConstraints.EAST,
                                                 GridBagConstraints.BOTH,
                                                 JBInsets.create(2, 0), 0, 0));
        severityPanelWeightY = 0.0;
        if (singleNode != null) {
          setConfigPanel(configPanelAnchor, myProfile.getToolDefaultState(singleNode.getDefaultDescriptor().getKey().toString(),
                                                                                  project));
        }
        scopesAndScopesAndSeveritiesTable = null;
      }
      else {
        if (singleNode != null) {
          for (final Descriptor descriptor : singleNode.getDescriptors().getNonDefaultDescriptors()) {
            descriptor.loadConfig();
          }
        }
        scopesAndScopesAndSeveritiesTable =
          new ScopesAndSeveritiesTable(new ScopesAndSeveritiesTable.TableSettings(nodes, myProfile, project) {
            @Override
            protected void onScopeChosen(@NotNull final ScopeToolState state) {
              setConfigPanel(configPanelAnchor, state);
              configPanelAnchor.revalidate();
              configPanelAnchor.repaint();
            }

            @Override
            protected void onSettingsChanged() {
              updateRecursively(nodes, false);
            }

            @Override
            protected void onScopeAdded() {
              updateRecursively(nodes, false);
            }

            @Override
            protected void onScopesOrderChanged() {
              updateRecursively(nodes, true);
            }

            @Override
            protected void onScopeRemoved(final int scopesCount) {
              updateRecursively(nodes, scopesCount == 1);
            }
          });

        final ToolbarDecorator wrappedTable = ToolbarDecorator.createDecorator(scopesAndScopesAndSeveritiesTable).disableUpDownActions().setRemoveActionUpdater(
          __ -> {
            final int selectedRow = scopesAndScopesAndSeveritiesTable.getSelectedRow();
            final int rowCount = scopesAndScopesAndSeveritiesTable.getRowCount();
            return rowCount - 1 != selectedRow;
          });
        final JPanel panel = wrappedTable.createPanel();
        panel.setMinimumSize(new Dimension(getMinimumSize().width, 3 * scopesAndScopesAndSeveritiesTable.getRowHeight()));
        severityPanel.add(new JBLabel(InspectionsBundle.message("inspection.scopes.and.severities")),
                          new GridBagConstraints(0, 0, 1, 1, 1.0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                 JBUI.insets(5, 0, 2, 10), 0, 0));
        severityPanel.add(panel, new GridBagConstraints(0, 1, 1, 1, 0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                                                        JBUI.insets(0, 0, 0, 0), 0, 0));
        severityPanelWeightY = 0.3;
      }
      myOptionsPanel.add(severityPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, severityPanelWeightY, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                                                               JBUI.insets(0, 2, 0, 0), 0, 0));
      GuiUtils.enableChildren(myOptionsPanel, isThoughOneNodeEnabled(nodes));
      if (configPanelAnchor.getComponentCount() != 0) {
        myOptionsPanel.add(new ToolOptionsSeparator(configPanelAnchor, scopesAndScopesAndSeveritiesTable), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                                                                                                        JBUI.emptyInsets(), 0, 0));
        myOptionsPanel.add(configPanelAnchor, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                                                                              JBUI.insets(0, 2, 0, 0), 0, 0));
      }
      else if (scopesNames.isEmpty()) {
        myOptionsPanel.add(configPanelAnchor, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                                                                     JBUI.insets(0, 2, 0, 0), 0, 0));
      }
      myOptionsPanel.revalidate();
    }
    else {
      initOptionsAndDescriptionPanel();
    }
    myOptionsPanel.repaint();
  }

  private void updateRecursively(Collection<? extends InspectionConfigTreeNode> nodes, boolean updateOptionsAndDescriptionPanel) {
    InspectionConfigTreeNode.updateUpHierarchy(nodes);
    myTreeTable.repaint();
    if (updateOptionsAndDescriptionPanel) {
      updateOptionsAndDescriptionPanel();
    }
  }

  private boolean isThoughOneNodeEnabled(Collection<InspectionConfigTreeNode.Tool> nodes) {
    final Project project = myProjectProfileManager.getProject();
    for (final InspectionConfigTreeNode.Tool node : nodes) {
      final String toolId = node.getKey().toString();
      if (myProfile.getTools(toolId, project).isEnabled()) {
        return true;
      }
    }
    return false;
  }

  private void initOptionsAndDescriptionPanel() {
    myOptionsPanel.removeAll();
    readHTML(myBrowser, EMPTY_HTML);
    myOptionsPanel.validate();
    myOptionsPanel.repaint();
  }

  @NotNull
  public InspectionProfileModifiableModel getProfile() {
    return myProfile;
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(700, 500);
  }

  public void disposeUI() {
    if (myInspectionProfilePanel == null) {
      return;
    }
    myAlarm.cancelAllRequests();
    myProfileFilter.dispose();
    for (ScopeToolState state : myProfile.getAllTools()) {
      state.resetConfigPanel();
    }
    Disposer.dispose(myDisposable);
    myDisposable = null;
  }

  public static HyperlinkAdapter createSettingsHyperlinkListener(Project project){
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        String description = e.getDescription();
        if (description.startsWith(SETTINGS)) {
          DataContext context = DataManager.getInstance().getDataContextFromFocus().getResult();
          if (context != null) {
            Settings settings = Settings.KEY.getData(context);
            String configId = description.substring(SETTINGS.length());
            if (settings != null) {
              settings.select(settings.find(configId));
            } else {
              ShowSettingsUtilImpl.showSettingsDialog(project, configId, "");
            }
          }
        }
        else {
          BrowserUtil.browse(description);
        }
      }
    };
  }

  private JPanel createInspectionProfileSettingsPanel() {

    myBrowser = new JEditorPane(UIUtil.HTML_MIME, EMPTY_HTML);
    myBrowser.setEditable(false);
    myBrowser.setBorder(JBUI.Borders.empty(5));
    myBrowser.addHyperlinkListener(createSettingsHyperlinkListener(myProjectProfileManager.getProject()));

    initToolStates();
    fillTreeData(myProfileFilter != null ? myProfileFilter.getFilter() : null, true);

    JPanel descriptionPanel = new JPanel(new BorderLayout());
    descriptionPanel.setBorder(IdeBorderFactory.createTitledBorder(InspectionsBundle.message("inspection.description.title"), false,
                                                                   new JBInsets(2, 2, 0, 0)));
    descriptionPanel.add(ScrollPaneFactory.createScrollPane(myBrowser), BorderLayout.CENTER);

    JBSplitter rightSplitter =
      new JBSplitter(true, "SingleInspectionProfilePanel.HORIZONTAL_DIVIDER_PROPORTION", DIVIDER_PROPORTION_DEFAULT);
    rightSplitter.setFirstComponent(descriptionPanel);

    myOptionsPanel = new JPanel(new GridBagLayout());
    initOptionsAndDescriptionPanel();
    rightSplitter.setSecondComponent(myOptionsPanel);
    rightSplitter.setHonorComponentsMinimumSize(true);

    final JScrollPane tree = initTreeScrollPane();

    final JPanel northPanel = new JPanel(new GridBagLayout());
    northPanel.setBorder(JBUI.Borders.empty(2, 0));
    myProfileFilter.setPreferredSize(new Dimension(20, myProfileFilter.getPreferredSize().height));
    northPanel.add(myProfileFilter, new GridBagConstraints(0, 0, 1, 1, 0.5, 1, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.HORIZONTAL,
                                                           JBUI.emptyInsets(), 0, 0));
    northPanel.add(createTreeToolbarPanel().getComponent(), new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL,
                                                                                   JBUI.emptyInsets(), 0, 0));

    JBSplitter mainSplitter = new OnePixelSplitter(false, DIVIDER_PROPORTION_DEFAULT, 0.01f, 0.99f);
    mainSplitter.setSplitterProportionKey("SingleInspectionProfilePanel.VERTICAL_DIVIDER_PROPORTION");
    mainSplitter.setFirstComponent(tree);
    mainSplitter.setSecondComponent(rightSplitter);
    mainSplitter.setHonorComponentsMinimumSize(false);

    final JPanel inspectionTreePanel = new JPanel(new BorderLayout());
    inspectionTreePanel.add(northPanel, BorderLayout.NORTH);
    inspectionTreePanel.add(mainSplitter, BorderLayout.CENTER);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(inspectionTreePanel, BorderLayout.CENTER);
    final JBCheckBox disableNewInspectionsCheckBox = new JBCheckBox("Disable new inspections by default",
                                                                    getProfile().isProfileLocked());
    panel.add(disableNewInspectionsCheckBox, BorderLayout.SOUTH);
    disableNewInspectionsCheckBox.addItemListener(__ -> {
      final boolean enabled = disableNewInspectionsCheckBox.isSelected();
      if (!isDisposed()) {
        final InspectionProfileImpl profile = getProfile();
        profile.lockProfile(enabled);
      }
    });
    return panel;
  }

  public boolean isModified() {
    if (myTreeTable == null) return false;
    if (myModified) return true;
    if (myProfile.isChanged()) return true;
    if (myProfile.getSource().isProjectLevel() != myProfile.isProjectLevel()) return true;
    if (!Comparing.strEqual(myProfile.getSource().getName(), myProfile.getName())) return true;
    if (!Arrays.equals(myInitialScopesOrder, myProfile.getScopesOrder())) return true;
    return descriptorsAreChanged();
  }

  public void reset() {
    myModified = false;
    filterTree();
    final String filter = myProfileFilter.getFilter();
    myProfileFilter.reset();
    myProfileFilter.setSelectedItem(filter);
    myProfile.setName(myProfile.getSource().getName());
    myProfile.setProjectLevel(myProfile.getSource().isProjectLevel());
  }

  public void apply() {
    final boolean modified = isModified();
    if (!modified) {
      return;
    }
    InspectionProfileModifiableModel selectedProfile = myProfile;

    BaseInspectionProfileManager profileManager = selectedProfile.isProjectLevel() ? myProjectProfileManager : (BaseInspectionProfileManager)InspectionProfileManager.getInstance();
    InspectionProfileImpl source = selectedProfile.getSource();

    if (source.getProfileManager() != profileManager) {
      source.getProfileManager().deleteProfile(source);
    }

    if (selectedProfile.getProfileManager() != profileManager) {
      copyUsedSeveritiesIfUndefined(selectedProfile, profileManager);
      selectedProfile.setProfileManager(profileManager);
    }

    selectedProfile.commit();
    profileManager.addProfile(source);
    profileManager.fireProfileChanged(source);

    myModified = false;
    myRoot.dropCache();
    initToolStates();
    updateOptionsAndDescriptionPanel();
  }

  private boolean descriptorsAreChanged() {
    return ContainerUtil.exists(myInitialToolDescriptors.values(),
                  toolDescriptors -> areToolDescriptorsChanged(myProjectProfileManager.getProject(), myProfile, toolDescriptors));
  }

  public static boolean areToolDescriptorsChanged(@NotNull Project project,
                                                  @NotNull InspectionProfileModifiableModel profile,
                                                  @NotNull ToolDescriptors toolDescriptors) {
    Descriptor desc = toolDescriptors.getDefaultDescriptor();
    if (profile.isToolEnabled(desc.getKey(), null, project) != desc.isEnabled()) {
      return true;
    }
    if (profile.getErrorLevel(desc.getKey(), desc.getScope(), project) != desc.getLevel()) {
      return true;
    }
    final List<Descriptor> descriptors = toolDescriptors.getNonDefaultDescriptors();
    for (Descriptor descriptor : descriptors) {
      if (profile.isToolEnabled(descriptor.getKey(), descriptor.getScope(), project) != descriptor.isEnabled()) {
        return true;
      }
      if (profile.getErrorLevel(descriptor.getKey(), descriptor.getScope(), project) != descriptor.getLevel()) {
        return true;
      }
    }

    final List<ScopeToolState> tools = profile.getNonDefaultTools(desc.getKey().toString(), project);
    if (tools.size() != descriptors.size()) {
      return true;
    }
    for (int i = 0; i < tools.size(); i++) {
      final ScopeToolState pair = tools.get(i);
      if (!Comparing.equal(pair.getScope(project), descriptors.get(i).getScope())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void setVisible(boolean aFlag) {
    if (aFlag && myInspectionProfilePanel == null) {
      initUI();
    }
    super.setVisible(aFlag);
  }

  private void setNewHighlightingLevel(@NotNull HighlightDisplayLevel level) {
    Collection<InspectionConfigTreeNode.Tool> tools = myTreeTable.getSelectedToolNodes();
    if (!tools.isEmpty()) {
      for (InspectionConfigTreeNode.Tool tool : tools) {
        updateErrorLevel(tool, level);
      }
      updateOptionsAndDescriptionPanel();
    } else {
      initOptionsAndDescriptionPanel();
    }
    repaintTableData();
  }

  private void updateErrorLevel(final InspectionConfigTreeNode.Tool child,
                                @NotNull HighlightDisplayLevel level) {
    final HighlightDisplayKey key = child.getKey();
    myProfile.setErrorLevel(key, level, null, myProjectProfileManager.getProject());
    child.dropCache();
  }

  public JComponent getPreferredFocusedComponent() {
    return myTreeTable;
  }

  private class MyFilterComponent extends FilterComponent {
    private MyFilterComponent() {
      super(INSPECTION_FILTER_HISTORY, 10);
    }

    @Override
    public void filter() {
      filterTree();
    }

    @Override
    protected void onlineFilter() {
      if (isDisposed()) return;
      final String filter = getFilter();
      getExpandedNodes(myProfile).saveVisibleState(myTreeTable.getTree());
      fillTreeData(filter, true);
      reloadModel();
      if (filter == null || filter.isEmpty()) {
        restoreTreeState();
      } else {
        TreeUtil.expandAll(myTreeTable.getTree());
      }
    }
  }

  private class ToolOptionsSeparator extends JPanel {
    private final LinkLabel<?> myResetLink;
    @Nullable
    private final ScopesAndSeveritiesTable myScopesAndSeveritiesTable;

    ToolOptionsSeparator(JComponent options, @Nullable ScopesAndSeveritiesTable scopesAndSeveritiesTable) {
      myScopesAndSeveritiesTable = scopesAndSeveritiesTable;
      setLayout(new GridBagLayout());
      GridBagConstraints optionsLabelConstraints = new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insets(0, 2, 0, 0), 0, 0);
      add(new JBLabel("Options"), optionsLabelConstraints);
      GridBagConstraints separatorConstraints =
        new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, JBUI.insets(2,
                                                                                                                       TitledSeparator.SEPARATOR_LEFT_INSET,
                                                                                                                       0,
                                                                                                                       TitledSeparator.SEPARATOR_RIGHT_INSET),
                               0, 0);
      add(new JSeparator(SwingConstants.HORIZONTAL), separatorConstraints);
      GridBagConstraints resetLabelConstraints = new GridBagConstraints(2, 0, 0, 1, 0, 1, GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0);

      UserActivityWatcher userActivityWatcher = new UserActivityWatcher();
      userActivityWatcher.addUserActivityListener(() -> setupResetLinkVisibility());
      userActivityWatcher.register(options);
      myResetLink = LinkLabel.create("Reset", () -> {
        ScopeToolState state = getSelectedState();
        if (state != null) {
          state.resetConfigPanel();
          Project project = myProjectProfileManager.getProject();
          myProfile.resetToBase(state.getTool().getTool().getShortName(), state.getScope(project), project);
          updateOptionsAndDescriptionPanel();
        }
      });
      add(myResetLink, resetLabelConstraints);
      setupResetLinkVisibility();
    }

    private void setupResetLinkVisibility() {
      if (myTreeTable == null || isDisposed()) return;
      InspectionConfigTreeNode.Tool node = myTreeTable.getStrictlySelectedToolNode();
      if (node != null) {
        ScopeToolState state = getSelectedState();
        if (state == null) return;
        Project project = myProjectProfileManager.getProject();
        boolean canReset = !myProfile.isProperSetting(state.getTool().getTool().getShortName(), state.getScope(project), project);

        myResetLink.setVisible(canReset);
        revalidate();
        repaint();
      }
    }

    private ScopeToolState getSelectedState() {
      InspectionConfigTreeNode.Tool node = myTreeTable.getStrictlySelectedToolNode();
      if (node == null) return null;
      if (myScopesAndSeveritiesTable != null) {
        List<ScopeToolState> selectedStates = myScopesAndSeveritiesTable.getSelectedStates();
        LOG.assertTrue(selectedStates.size() == 1);
        return selectedStates.get(0);
      } else {
        return node.getDescriptors().getDefaultDescriptor().getState();
      }
    }
  }
}
