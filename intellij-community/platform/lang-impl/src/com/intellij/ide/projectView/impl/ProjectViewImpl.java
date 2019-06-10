// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl;

import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.ide.impl.ProjectViewSelectInTarget;
import com.intellij.ide.projectView.HelpID;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.nodes.*;
import com.intellij.ide.scopeView.ScopeViewPane;
import com.intellij.ide.ui.SplitterProportionsDataImpl;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.DeleteHandler;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.module.UnloadedModuleDescription;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.SplitterProportionsData;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.AutoScrollFromSourceHandler;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.IdeUICustomization;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.switcher.QuickActionProvider;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

@State(name = "ProjectView", storages = {
  @Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE),
  @Storage(value = StoragePathMacros.WORKSPACE_FILE, deprecated = true)
})
public class ProjectViewImpl extends ProjectView implements PersistentStateComponent<Element>, Disposable, QuickActionProvider, BusyObject  {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.projectView.impl.ProjectViewImpl");
  private static final Key<String> ID_KEY = Key.create("pane-id");
  private static final Key<String> SUB_ID_KEY = Key.create("pane-sub-id");
  private final CopyPasteDelegator myCopyPasteDelegator;
  private boolean isInitialized;
  private boolean myExtensionsLoaded;
  @NotNull private final Project myProject;

  // + options
  private final Map<String, Boolean> myFlattenPackages = new THashMap<>();
  private static final boolean ourFlattenPackagesDefaults = false;
  private final Map<String, Boolean> myShowMembers = new THashMap<>();
  private static final boolean ourShowMembersDefaults = false;
  private final Map<String, Boolean> myManualOrder = new THashMap<>();
  private static final boolean ourManualOrderDefaults = false;
  private final Map<String, Boolean> mySortByType = new THashMap<>();
  private static final boolean ourSortByTypeDefaults = false;
  private final Map<String, Boolean> myShowModules = new THashMap<>();
  private static final boolean ourShowModulesDefaults = true;
  private final Map<String, Boolean> myFlattenModules = new THashMap<>();
  private static final boolean ourFlattenModulesDefaults = false;
  private final Map<String, Boolean> myShowExcludedFiles = new THashMap<>();
  private static final boolean ourShowExcludedFilesDefaults = true;
  private final Map<String, Boolean> myShowLibraryContents = new THashMap<>();
  private static final boolean ourShowLibraryContentsDefaults = true;
  private final Map<String, Boolean> myHideEmptyPackages = new THashMap<>();
  private static final boolean ourHideEmptyPackagesDefaults = true;
  private final Map<String, Boolean> myCompactDirectories = new THashMap<>();
  private static final boolean ourCompactDirectoriesDefaults = false;
  private final Map<String, Boolean> myAbbreviatePackageNames = new THashMap<>();
  private static final boolean ourAbbreviatePackagesDefaults = false;
  private final Map<String, Boolean> myAutoscrollToSource = new THashMap<>();
  private final Map<String, Boolean> myAutoscrollFromSource = new THashMap<>();
  private static final boolean ourAutoscrollFromSourceDefaults = false;

  private boolean myFoldersAlwaysOnTop = true;

  private String myCurrentViewId;
  private String myCurrentViewSubId;
  // - options

  private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;
  private final MyAutoScrollFromSourceHandler myAutoScrollFromSourceHandler;
  private volatile ThreeState myCurrentSelectionObsolete = ThreeState.NO;

  private final IdeView myIdeView = new IdeViewForProjectViewPane(this::getCurrentProjectViewPane);
  private final MyDeletePSIElementProvider myDeletePSIElementProvider = new MyDeletePSIElementProvider();
  private final ModuleDeleteProvider myDeleteModuleProvider = new ModuleDeleteProvider();

  private SimpleToolWindowPanel myPanel;
  private final Map<String, AbstractProjectViewPane> myId2Pane = new LinkedHashMap<>();
  private final Collection<AbstractProjectViewPane> myUninitializedPanes = new THashSet<>();

  private static final DataKey<ProjectViewImpl> DATA_KEY = DataKey.create("com.intellij.ide.projectView.impl.ProjectViewImpl");

  private DefaultActionGroup myActionGroup;
  private String mySavedPaneId = getDefaultViewId();
  private String mySavedPaneSubId;
  @NonNls private static final String ELEMENT_NAVIGATOR = "navigator";
  @NonNls private static final String ELEMENT_PANES = "panes";
  @NonNls private static final String ELEMENT_PANE = "pane";
  @NonNls private static final String ATTRIBUTE_CURRENT_VIEW = "currentView";
  @NonNls private static final String ATTRIBUTE_CURRENT_SUBVIEW = "currentSubView";
  @NonNls private static final String ELEMENT_FLATTEN_PACKAGES = "flattenPackages";
  @NonNls private static final String ELEMENT_SHOW_MEMBERS = "showMembers";
  @NonNls private static final String ELEMENT_SHOW_MODULES = "showModules";
  @NonNls private static final String ELEMENT_SHOW_EXCLUDED_FILES = "showExcludedFiles";
  @NonNls private static final String ELEMENT_SHOW_LIBRARY_CONTENTS = "showLibraryContents";
  @NonNls private static final String ELEMENT_HIDE_EMPTY_PACKAGES = "hideEmptyPackages";
  @NonNls private static final String ELEMENT_COMPACT_DIRECTORIES = "compactDirectories";
  @NonNls private static final String ELEMENT_ABBREVIATE_PACKAGE_NAMES = "abbreviatePackageNames";
  @NonNls private static final String ELEMENT_AUTOSCROLL_TO_SOURCE = "autoscrollToSource";
  @NonNls private static final String ELEMENT_AUTOSCROLL_FROM_SOURCE = "autoscrollFromSource";
  @NonNls private static final String ELEMENT_SORT_BY_TYPE = "sortByType";
  @NonNls private static final String ELEMENT_FOLDERS_ALWAYS_ON_TOP = "foldersAlwaysOnTop";
  @NonNls private static final String ELEMENT_MANUAL_ORDER = "manualOrder";

  private static final String ATTRIBUTE_ID = "id";
  private JPanel myViewContentPanel;
  private static final Comparator<AbstractProjectViewPane> PANE_WEIGHT_COMPARATOR = Comparator.comparingInt(AbstractProjectViewPane::getWeight);
  private final MyPanel myDataProvider;
  private final SplitterProportionsData splitterProportions = new SplitterProportionsDataImpl();
  private final MessageBusConnection myConnection;
  private final Map<String, Element> myUninitializedPaneState = new THashMap<>();
  private final Map<String, SelectInTarget> mySelectInTargets = new LinkedHashMap<>();
  private ContentManager myContentManager;

  public ProjectViewImpl(@NotNull Project project) {
    myProject = project;

    constructUi();

    myConnection = project.getMessageBus().connect();
    myAutoScrollFromSourceHandler = new MyAutoScrollFromSourceHandler();

    myDataProvider = new MyPanel();
    myDataProvider.add(myPanel, BorderLayout.CENTER);
    myCopyPasteDelegator = new CopyPasteDelegator(myProject, myPanel);
    myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
      @Override
      protected boolean isAutoScrollMode() {
        return isAutoscrollToSource(myCurrentViewId);
      }

      @Override
      protected void setAutoScrollMode(boolean state) {
        setAutoscrollToSource(state, myCurrentViewId);
      }

      @Override
      protected boolean isAutoScrollEnabledFor(@NotNull VirtualFile file) {
        if (!super.isAutoScrollEnabledFor(file)) return false;
        AbstractProjectViewPane pane = getCurrentProjectViewPane();
        return pane == null || pane.isAutoScrollEnabledFor(file);
      }
    };

    myConnection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
      private boolean toolWindowVisible;

      @Override
      public void stateChanged() {
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (window == null || toolWindowVisible == window.isVisible()) return;
        myCurrentSelectionObsolete = ThreeState.NO;
        if (window.isVisible() && !toolWindowVisible) {
          AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
          if (currentProjectViewPane != null && isAutoscrollFromSource(currentProjectViewPane.getId())) {
            SimpleSelectInContext context = myAutoScrollFromSourceHandler.findSelectInContext();
            if (context != null) {
              myCurrentSelectionObsolete = ThreeState.UNSURE;
              context.selectInCurrentTarget();
            }
          }
        }
        toolWindowVisible = window.isVisible();
      }
    });
  }

  private void constructUi() {
    myViewContentPanel = new JPanel();
    myPanel = new SimpleToolWindowPanel(true).setProvideQuickActions(false);
    myPanel.setContent(myViewContentPanel);
  }

  @NotNull
  @Override
  public String getName() {
    return IdeUICustomization.getInstance().getProjectViewTitle();
  }

  @NotNull
  @Override
  public List<AnAction> getActions(boolean originalProvider) {
    DefaultActionGroup views = new DefaultActionGroup("Change View", true);

    ChangeViewAction lastHeader = null;
    for (int i = 0; i < myContentManager.getContentCount(); i++) {
      Content each = myContentManager.getContent(i);
      if (each == null) continue;

      String id = each.getUserData(ID_KEY);
      String subId = each.getUserData(SUB_ID_KEY);
      ChangeViewAction newHeader = new ChangeViewAction(id, subId);

      if (lastHeader != null) {
        boolean lastHasKids = lastHeader.mySubId != null;
        boolean newHasKids = newHeader.mySubId != null;
        if (lastHasKids != newHasKids ||
            lastHasKids && lastHeader.myId != newHeader.myId) {
          views.add(Separator.getInstance());
        }
      }

      views.add(newHeader);
      lastHeader = newHeader;
    }
    List<AnAction> result = new ArrayList<>();
    result.add(views);
    result.add(Separator.getInstance());

    if (myActionGroup != null) {
      List<AnAction> secondary = new ArrayList<>();
      for (AnAction each : myActionGroup.getChildren(null)) {
        if (myActionGroup.isPrimary(each)) {
          result.add(each);
        }
        else {
          secondary.add(each);
        }
      }

      result.add(Separator.getInstance());
      result.addAll(secondary);
    }

    return result;
  }

  private class ChangeViewAction extends AnAction {
    @NotNull private final String myId;
    @Nullable private final String mySubId;

    private ChangeViewAction(@NotNull String id, @Nullable String subId) {
      myId = id;
      mySubId = subId;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      AbstractProjectViewPane pane = getProjectViewPaneById(myId);
      e.getPresentation().setText(mySubId != null ? pane.getPresentableSubIdName(mySubId) : pane.getTitle());
      e.getPresentation().setIcon(mySubId != null ? pane.getPresentableSubIdIcon(mySubId) : pane.getIcon());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      changeView(myId, mySubId);
    }
  }

  @Override
  public synchronized void addProjectPane(@NotNull final AbstractProjectViewPane pane) {
    myUninitializedPanes.add(pane);
    SelectInTarget selectInTarget = pane.createSelectInTarget();
    String id = selectInTarget.getMinorViewId();
    if (pane.getId().equals(id)) {
      mySelectInTargets.put(id, selectInTarget);
    }
    else {
      try {
        LOG.error("Unexpected SelectInTarget: " + selectInTarget.getClass() + "\n  created by project pane:" + pane.getClass());
      }
      catch (AssertionError ignored) {
      }
    }
    if (isInitialized) {
      doAddUninitializedPanes();
    }
  }

  @Override
  public synchronized void removeProjectPane(@NotNull AbstractProjectViewPane pane) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myUninitializedPanes.remove(pane);
    //assume we are completely initialized here
    String idToRemove = pane.getId();

    if (!myId2Pane.containsKey(idToRemove)) return;
    for (int i = getContentManager().getContentCount() - 1; i >= 0; i--) {
      Content content = getContentManager().getContent(i);
      String id = content != null ? content.getUserData(ID_KEY) : null;
      if (idToRemove.equals(id)) {
        getContentManager().removeContent(content, true);
      }
    }
    myId2Pane.remove(idToRemove);
    mySelectInTargets.remove(idToRemove);
    viewSelectionChanged();
  }

  private synchronized void doAddUninitializedPanes() {
    for (AbstractProjectViewPane pane : myUninitializedPanes) {
      doAddPane(pane);
    }
    final Content[] contents = getContentManager().getContents();
    for (int i = 1; i < contents.length; i++) {
      Content content = contents[i];
      Content prev = contents[i - 1];
      if (!StringUtil.equals(content.getUserData(ID_KEY), prev.getUserData(ID_KEY)) &&
          prev.getUserData(SUB_ID_KEY) != null && content.getSeparator() == null) {
        content.setSeparator("");
      }
    }

    String selectID = null;
    String selectSubID = null;

    // try to find saved selected view...
    for (Content content : contents) {
      final String id = content.getUserData(ID_KEY);
      final String subId = content.getUserData(SUB_ID_KEY);
      if (id != null &&
          id.equals(mySavedPaneId) &&
          StringUtil.equals(subId, mySavedPaneSubId)) {
        selectID = id;
        selectSubID = subId;
        mySavedPaneId = null;
        mySavedPaneSubId = null;
        break;
      }
    }

    // saved view not found (plugin disabled, ID changed etc.) - select first available view...
    if (selectID == null && contents.length > 0 && myCurrentViewId == null) {
      Content content = contents[0];
      selectID = content.getUserData(ID_KEY);
      selectSubID = content.getUserData(SUB_ID_KEY);
    }

    if (selectID != null) {
      changeView(selectID, selectSubID);
    }

    myUninitializedPanes.clear();
  }

  private void doAddPane(@NotNull final AbstractProjectViewPane newPane) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    int index;
    final ContentManager manager = getContentManager();
    for (index = 0; index < manager.getContentCount(); index++) {
      Content content = manager.getContent(index);
      String id = content.getUserData(ID_KEY);
      AbstractProjectViewPane pane = myId2Pane.get(id);

      int comp = PANE_WEIGHT_COMPARATOR.compare(pane, newPane);
      LOG.assertTrue(comp != 0, "Project view pane " + newPane + " has the same weight as " + pane +
                                ". Please make sure that you overload getWeight() and return a distinct weight value.");
      if (comp > 0) {
        break;
      }
    }
    final String id = newPane.getId();
    myId2Pane.put(id, newPane);
    String[] subIds = newPane.getSubIds();
    subIds = subIds.length == 0 ? new String[]{null} : subIds;
    boolean first = true;
    for (String subId : subIds) {
      final String title = subId != null ?  newPane.getPresentableSubIdName(subId) : newPane.getTitle();
      final Content content = getContentManager().getFactory().createContent(getComponent(), title, false);
      content.setTabName(title);
      content.putUserData(ID_KEY, id);
      content.putUserData(SUB_ID_KEY, subId);
      content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
      Icon icon = subId != null ? newPane.getPresentableSubIdIcon(subId) : newPane.getIcon();
      content.setIcon(icon);
      content.setPopupIcon(icon);
      content.setPreferredFocusedComponent(() -> {
        final AbstractProjectViewPane current = getCurrentProjectViewPane();
        return current != null ? current.getComponentToFocus() : null;
      });
      content.setBusyObject(this);
      if (first && subId != null) {
        content.setSeparator(newPane.getTitle());
      }
      manager.addContent(content, index++);
      first = false;
    }
  }

  private void showPane(@NotNull AbstractProjectViewPane newPane) {
    AbstractProjectViewPane currentPane = getCurrentProjectViewPane();
    PsiElement selectedPsiElement = null;
    if (currentPane != null) {
      if (currentPane != newPane) {
        currentPane.saveExpandedPaths();
      }
      final PsiElement[] elements = currentPane.getSelectedPSIElements();
      if (elements.length > 0) {
        selectedPsiElement = elements[0];
      }
    }
    myViewContentPanel.removeAll();
    JComponent component = newPane.createComponent();
    UIUtil.removeScrollBorder(component);
    myViewContentPanel.setLayout(new BorderLayout());
    myViewContentPanel.add(component, BorderLayout.CENTER);
    myCurrentViewId = newPane.getId();
    String newSubId = myCurrentViewSubId = newPane.getSubId();
    myViewContentPanel.revalidate();
    myViewContentPanel.repaint();
    createToolbarActions();

    myAutoScrollToSourceHandler.install(newPane.myTree);

    IdeFocusManager.getInstance(myProject).requestFocusInProject(newPane.getComponentToFocus(), myProject);

    newPane.restoreExpandedPaths();
    if (selectedPsiElement != null && newSubId != null) {
      final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(selectedPsiElement);
      ProjectViewSelectInTarget target = virtualFile == null ? null : getProjectViewSelectInTarget(newPane);
      if (target != null && target.isSubIdSelectable(newSubId, new FileSelectInContext(myProject, virtualFile, null))) {
        newPane.select(selectedPsiElement, virtualFile, true);
      }
    }
    myAutoScrollToSourceHandler.onMouseClicked(newPane.myTree);
  }

  // public for tests
  public synchronized void setupImpl(@NotNull ToolWindow toolWindow) {
    setupImpl(toolWindow, true);
  }

  // public for tests
  public synchronized void setupImpl(@NotNull ToolWindow toolWindow, final boolean loadPaneExtensions) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myActionGroup = new DefaultActionGroup();

    myAutoScrollFromSourceHandler.install();

    myContentManager = toolWindow.getContentManager();
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      toolWindow.setDefaultContentUiType(ToolWindowContentUiType.COMBO);
      ((ToolWindowEx)toolWindow).setAdditionalGearActions(myActionGroup);
      toolWindow.getComponent().putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true");
    }

    GuiUtils.replaceJSplitPaneWithIDEASplitter(myPanel);
    SwingUtilities.invokeLater(() -> splitterProportions.restoreSplitterProportions(myPanel));

    if (loadPaneExtensions) {
      ensurePanesLoaded();
    }
    isInitialized = true;
    doAddUninitializedPanes();

    getContentManager().addContentManagerListener(new ContentManagerAdapter() {
      @Override
      public void selectionChanged(@NotNull ContentManagerEvent event) {
        if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
          viewSelectionChanged();
        }
      }
    });
    viewSelectionChanged();
  }

  private void ensurePanesLoaded() {
    if (myExtensionsLoaded) return;
    AbstractProjectViewPane[] extensions = AbstractProjectViewPane.EP_NAME.getExtensions(myProject);
    Arrays.sort(extensions, PANE_WEIGHT_COMPARATOR);
    myExtensionsLoaded = true;
    for(AbstractProjectViewPane pane: extensions) {
      if (myUninitializedPaneState.containsKey(pane.getId())) {
        try {
          pane.readExternal(myUninitializedPaneState.get(pane.getId()));
        }
        catch (InvalidDataException e) {
          // ignore
        }
        myUninitializedPaneState.remove(pane.getId());
      }
      if (pane.isInitiallyVisible() && !myId2Pane.containsKey(pane.getId())) {
        addProjectPane(pane);
      }
    }
  }

  private void viewSelectionChanged() {
    Content content = getContentManager().getSelectedContent();
    if (content == null) return;
    String id = content.getUserData(ID_KEY);
    String subId = content.getUserData(SUB_ID_KEY);
    if (Objects.equals(id, myCurrentViewId) && Objects.equals(subId, myCurrentViewSubId)) return;
    final AbstractProjectViewPane newPane = getProjectViewPaneById(id);
    if (newPane == null) return;
    newPane.setSubId(subId);
    showPane(newPane);
    ProjectViewSelectInTarget target = getProjectViewSelectInTarget(newPane);
    if (target != null) target.setSubId(subId);
    if (isAutoscrollFromSource(id)) {
      myAutoScrollFromSourceHandler.scrollFromSource();
    }
  }

  private void createToolbarActions() {
    if (myActionGroup == null) return;
    myActionGroup.removeAll();
    if (ProjectViewDirectoryHelper.getInstance(myProject).supportsFlattenPackages()) {
      myActionGroup.addAction(new PaneOptionAction(myFlattenPackages, IdeBundle.message("action.flatten.packages"),
                                             IdeBundle.message("action.flatten.packages"), PlatformIcons.FLATTEN_PACKAGES_ICON,
                                             ourFlattenPackagesDefaults) {
        @Override
        public void setSelected(@NotNull AnActionEvent event, boolean flag) {
          final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
          final SelectionInfo selectionInfo = SelectionInfo.create(viewPane);
          if (isGlobalOptions()) {
            setFlattenPackages(viewPane.getId(), flag);
          }
          super.setSelected(event, flag);

          selectionInfo.apply(viewPane);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent event) {
          if (isGlobalOptions()) return getGlobalOptions().getFlattenPackages();
          return super.isSelected(event);
        }
      }).setAsSecondary(true);
    }

    class FlattenPackagesDependableAction extends PaneOptionAction {
      FlattenPackagesDependableAction(@NotNull Map<String, Boolean> optionsMap,
                                      @NotNull String text,
                                      @NotNull String description,
                                      @NotNull Icon icon,
                                      boolean optionDefaultValue) {
        super(optionsMap, text, description, icon, optionDefaultValue);
      }

      @Override
      public void setSelected(@NotNull AnActionEvent event, boolean flag) {
        if (isGlobalOptions()) {
          getGlobalOptions().setFlattenPackages(flag);
        }
        super.setSelected(event, flag);
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        super.update(e);

        e.getPresentation().setVisible(isFlattenPackages(myCurrentViewId));
      }
    }
    if (ProjectViewDirectoryHelper.getInstance(myProject).supportsHideEmptyMiddlePackages()) {
      myActionGroup.addAction(new HideEmptyMiddlePackagesAction()).setAsSecondary(true);
    }
    if (ProjectViewDirectoryHelper.getInstance(myProject).supportsFlattenPackages()) {
      myActionGroup.addAction(new FlattenPackagesDependableAction(myAbbreviatePackageNames,
                                                            IdeBundle.message("action.abbreviate.qualified.package.names"),
                                                            IdeBundle.message("action.abbreviate.qualified.package.names"),
                                                            AllIcons.ObjectBrowser.AbbreviatePackageNames,
                                                            ourAbbreviatePackagesDefaults) {
        @Override
        public boolean isSelected(@NotNull AnActionEvent event) {
          return isFlattenPackages(myCurrentViewId) && isAbbreviatePackageNames(myCurrentViewId);
        }

        @Override
        public void setSelected(@NotNull AnActionEvent event, boolean flag) {
          if (isGlobalOptions()) {
            setAbbreviatePackageNames(myCurrentViewId, flag);
          }
          setPaneOption(myOptionsMap, flag, myCurrentViewId, true);
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
          super.update(event);
          Presentation presentation = event.getPresentation();
          presentation.setEnabledAndVisible(!ScopeViewPane.ID.equals(myCurrentViewId));
        }
      }).setAsSecondary(true);
    }

    myActionGroup.addAction(new PaneOptionAction(myCompactDirectories,
                                                 IdeBundle.message("action.compact.directories.text"),
                                                 IdeBundle.message("action.compact.directories.description"),
                                                 null, ourCompactDirectoriesDefaults) {

      @Override
      public void update(@NotNull AnActionEvent event) {
        super.update(event);
        Presentation presentation = event.getPresentation();
        presentation.setEnabledAndVisible(ScopeViewPane.ID.equals(myCurrentViewId));
      }

      @Override
      public boolean isSelected(@NotNull AnActionEvent event) {
        return isGlobalOptions()
               ? getGlobalOptions().getCompactDirectories()
               : super.isSelected(event);
      }

      @Override
      public void setSelected(@NotNull AnActionEvent event, boolean compactDirectories) {
        AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
        SelectionInfo selectionInfo = SelectionInfo.create(viewPane);
        if (isGlobalOptions()) getGlobalOptions().setCompactDirectories(compactDirectories);
        super.setSelected(event, compactDirectories);
        setPaneOption(myCompactDirectories, compactDirectories, viewPane.getId(), true);
        selectionInfo.apply(viewPane);
      }
    }).setAsSecondary(true);

    if (isShowMembersOptionSupported()) {
      myActionGroup.addAction(new PaneOptionAction(myShowMembers, IdeBundle.message("action.show.members"),
                                                   IdeBundle.message("action.show.hide.members"),
                                                   AllIcons.ObjectBrowser.ShowMembers, ourShowMembersDefaults) {
        @Override
        public boolean isSelected(@NotNull AnActionEvent event) {
          if (isGlobalOptions()) return getGlobalOptions().getShowMembers();
          return super.isSelected(event);
        }

        @Override
        public void setSelected(@NotNull AnActionEvent event, boolean flag) {
          if (isGlobalOptions()) {
            getGlobalOptions().setShowMembers(flag);
          }
          super.setSelected(event, flag);
        }
      })
        .setAsSecondary(true);
    }
    myActionGroup.addAction(myAutoScrollToSourceHandler.createToggleAction()).setAsSecondary(true);
    myActionGroup.addAction(myAutoScrollFromSourceHandler.createToggleAction()).setAsSecondary(true);
    myActionGroup.addAction(new ManualOrderAction()).setAsSecondary(true);
    myActionGroup.addAction(new SortByTypeAction()).setAsSecondary(true);
    myActionGroup.addAction(new FoldersAlwaysOnTopAction()).setAsSecondary(true);
    myActionGroup.addAction(ShowExcludedFilesAction.INSTANCE).setAsSecondary(true);

    getProjectViewPaneById(myCurrentViewId == null ? getDefaultViewId() : myCurrentViewId).addToolbarActions(myActionGroup);

    List<AnAction> titleActions = ContainerUtil.newSmartList();
    createTitleActions(titleActions);
    if (!titleActions.isEmpty()) {
      ToolWindowEx window = (ToolWindowEx)ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.PROJECT_VIEW);
      if (window != null) {
        window.setTitleActions(titleActions.toArray(AnAction.EMPTY_ARRAY));
      }
    }
  }

  protected void createTitleActions(@NotNull List<? super AnAction> titleActions) {
    if (!myAutoScrollFromSourceHandler.isAutoScrollEnabled()) {
      titleActions.add(new ScrollFromSourceAction());
    }
    AnAction collapseAllAction = CommonActionsManager.getInstance().createCollapseAllAction(new TreeExpander() {
      @Override
      public boolean canExpand() {
        return false;
      }

      @Override
      public void collapseAll() {
        AbstractProjectViewPane pane = getCurrentProjectViewPane();
        JTree tree = pane.myTree;
        if (tree != null) {
          TreeUtil.collapseAll(tree, 0);
        }
      }

      @Override
      public boolean canCollapse() {
        return true;
      }
    }, getComponent());
    collapseAllAction.getTemplatePresentation().setIcon(AllIcons.General.CollapseAll);
    collapseAllAction.getTemplatePresentation().setHoveredIcon(AllIcons.General.CollapseAllHover);
    titleActions.add(collapseAllAction);
  }

  protected boolean isShowMembersOptionSupported() {
    return true;
  }

  @Override
  public AbstractProjectViewPane getProjectViewPaneById(String id) {
    if (!ApplicationManager.getApplication().isUnitTestMode() && ApplicationManager.getApplication().isDispatchThread()) {
      // most tests don't need all panes to be loaded, but also we should not initialize panes on background threads
      ensurePanesLoaded();
    }

    final AbstractProjectViewPane pane = myId2Pane.get(id);
    if (pane != null) {
      return pane;
    }
    for (AbstractProjectViewPane viewPane : myUninitializedPanes) {
      if (viewPane.getId().equals(id)) {
        return viewPane;
      }
    }
    return null;
  }

  @Override
  public AbstractProjectViewPane getCurrentProjectViewPane() {
    return getProjectViewPaneById(myCurrentViewId);
  }

  @Override
  public void refresh() {
    AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
    if (currentProjectViewPane != null) {
      // may be null for e.g. default project
      currentProjectViewPane.updateFromRoot(false);
    }
  }

  private boolean isCurrentSelectionObsolete(boolean requestFocus) {
    if (myCurrentSelectionObsolete == ThreeState.YES) {
      myCurrentSelectionObsolete = ThreeState.NO;
      return true;
    }
    if (myCurrentSelectionObsolete == ThreeState.UNSURE) {
      myCurrentSelectionObsolete = requestFocus ? ThreeState.YES : ThreeState.NO;
    }
    return false;
  }

  @Override
  public void select(final Object element, VirtualFile file, boolean requestFocus) {
    if (isCurrentSelectionObsolete(requestFocus)) return;
    final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
    if (viewPane != null) {
      viewPane.select(element, file, requestFocus);
    }
  }

  @NotNull
  @Override
  public ActionCallback selectCB(Object element, VirtualFile file, boolean requestFocus) {
    if (isCurrentSelectionObsolete(requestFocus)) return ActionCallback.REJECTED;
    final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
    if (viewPane instanceof AbstractProjectViewPSIPane) {
      return ((AbstractProjectViewPSIPane)viewPane).selectCB(element, file, requestFocus);
    }
    select(element, file, requestFocus);
    return ActionCallback.DONE;
  }

  @Override
  public void dispose() {
    myConnection.disconnect();
  }

  @Override
  public JComponent getComponent() {
    return myDataProvider;
  }

  @Override
  public String getCurrentViewId() {
    return myCurrentViewId;
  }

  private SelectInTarget getCurrentSelectInTarget() {
    return getSelectInTarget(getCurrentViewId());
  }

  private SelectInTarget getSelectInTarget(String id) {
    return mySelectInTargets.get(id);
  }

  private ProjectViewSelectInTarget getProjectViewSelectInTarget(AbstractProjectViewPane pane) {
    SelectInTarget target = getSelectInTarget(pane.getId());
    return target instanceof ProjectViewSelectInTarget
           ? (ProjectViewSelectInTarget)target
           : null;
  }

  @Override
  public PsiElement getParentOfCurrentSelection() {
    final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
    if (viewPane == null) {
      return null;
    }
    TreePath path = viewPane.getSelectedPath();
    if (path == null) {
      return null;
    }
    path = path.getParentPath();
    if (path == null) {
      return null;
    }
    ProjectViewNode descriptor = TreeUtil.getLastUserObject(ProjectViewNode.class, path);
    if (descriptor != null) {
      Object element = descriptor.getValue();
      if (element instanceof PsiElement) {
        PsiElement psiElement = (PsiElement)element;
        if (!psiElement.isValid()) return null;
        return psiElement;
      }
      else {
        return null;
      }
    }
    return null;
  }

  public ContentManager getContentManager() {
    if (myContentManager == null) {
      ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.PROJECT_VIEW).getContentManager();
    }
    return myContentManager;
  }


  private class PaneOptionAction extends ToggleAction implements DumbAware {
    final Map<String, Boolean> myOptionsMap;
    private final boolean myOptionDefaultValue;

    PaneOptionAction(@NotNull Map<String, Boolean> optionsMap,
                     @NotNull String text,
                     @NotNull String description,
                     Icon icon,
                     boolean optionDefaultValue) {
      super(text, description, icon);
      myOptionsMap = optionsMap;
      myOptionDefaultValue = optionDefaultValue;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
      return getPaneOptionValue(myOptionsMap, myCurrentViewId, myOptionDefaultValue);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean flag) {
      setPaneOption(myOptionsMap, flag, myCurrentViewId, true);
    }
  }

  @Override
  public void changeView() {
    final List<AbstractProjectViewPane> views = new ArrayList<>(myId2Pane.values());
    views.remove(getCurrentProjectViewPane());
    Collections.sort(views, PANE_WEIGHT_COMPARATOR);

    IPopupChooserBuilder<AbstractProjectViewPane> builder = JBPopupFactory.getInstance()
      .createPopupChooserBuilder(views)
      .setRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          AbstractProjectViewPane pane = (AbstractProjectViewPane)value;
          setText(pane.getTitle());
          return this;
        }
      })
      .setTitle(IdeBundle.message("title.popup.views"))
      .setItemChosenCallback(pane -> changeView(pane.getId()));
    if (!views.isEmpty()) {
      builder = builder.setSelectedValue(views.get(0), true);
    }
    builder
      .createPopup()
      .showInCenterOf(getComponent());
  }

  @Override
  public void changeView(@NotNull String viewId) {
    changeView(viewId, null);
  }

  @Override
  public void changeView(@NotNull String viewId, @Nullable String subId) {
    changeViewCB(viewId, subId);
  }

  @NotNull
  @Override
  public ActionCallback changeViewCB(@NotNull String viewId, @Nullable String subId) {
    AbstractProjectViewPane pane = getProjectViewPaneById(viewId);
    LOG.assertTrue(pane != null, "Project view pane not found: " + viewId + "; subId:" + subId + "; project: " + myProject);

    boolean hasSubViews = pane.getSubIds().length > 0;
    if (hasSubViews) {
      if (subId == null) {
        // we try not to change subview
        // get currently selected subId from the pane
        subId = pane.getSubId();
      }
    }
    else {
      if (subId != null) {
        LOG.error("View doesn't have subviews: " + viewId + "; subId:" + subId + "; project: " + myProject);
      }
    }
    if (viewId.equals(myCurrentViewId) && Objects.equals(subId, myCurrentViewSubId)) return ActionCallback.REJECTED;

    // at this point null subId means that view has no subviews OR subview was never selected
    // we then search first content with the right viewId ignoring subIds of contents

    for (Content content : getContentManager().getContents()) {
      if (viewId.equals(content.getUserData(ID_KEY)) && (subId == null || subId.equals(content.getUserData(SUB_ID_KEY)))) {
        return getContentManager().setSelectedContentCB(content);
      }
    }

    return ActionCallback.REJECTED;
  }

  private final class MyDeletePSIElementProvider implements DeleteProvider {
    @Override
    public boolean canDeleteElement(@NotNull DataContext dataContext) {
      final PsiElement[] elements = getElementsToDelete();
      return DeleteHandler.shouldEnableDeleteAction(elements);
    }

    @Override
    public void deleteElement(@NotNull DataContext dataContext) {
      List<PsiElement> validElements = new ArrayList<>();
      for (PsiElement psiElement : getElementsToDelete()) {
        if (psiElement != null && psiElement.isValid()) validElements.add(psiElement);
      }
      final PsiElement[] elements = PsiUtilCore.toPsiElementArray(validElements);

      LocalHistoryAction a = LocalHistory.getInstance().startAction(IdeBundle.message("progress.deleting"));
      try {
        TransactionGuard.getInstance().submitTransactionAndWait(() -> DeleteHandler.deletePsiElement(elements, myProject));
      }
      finally {
        a.finish();
      }
    }

    @NotNull
    private PsiElement[] getElementsToDelete() {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      PsiElement[] elements = viewPane.getSelectedPSIElements();
      for (int idx = 0; idx < elements.length; idx++) {
        final PsiElement element = elements[idx];
        if (element instanceof PsiDirectory) {
          PsiDirectory directory = (PsiDirectory)element;
          final ProjectViewDirectoryHelper directoryHelper = ProjectViewDirectoryHelper.getInstance(myProject);
          if (isHideEmptyMiddlePackages(viewPane.getId()) && directory.getChildren().length == 0 && !directoryHelper.skipDirectory(directory)) {
            while (true) {
              PsiDirectory parent = directory.getParentDirectory();
              if (parent == null) break;
              if (directoryHelper.skipDirectory(parent) ||
                  PsiDirectoryFactory.getInstance(myProject).getQualifiedName(parent, false).isEmpty()) break;
              PsiElement[] children = parent.getChildren();
              if (children.length == 0 || children.length == 1 && children[0] == directory) {
                directory = parent;
              }
              else {
                break;
              }
            }
            elements[idx] = directory;
          }
          final VirtualFile virtualFile = directory.getVirtualFile();
          final String path = virtualFile.getPath();
          if (path.endsWith(JarFileSystem.JAR_SEPARATOR)) { // if is jar-file root
            final VirtualFile vFile =
              LocalFileSystem.getInstance().findFileByPath(path.substring(0, path.length() - JarFileSystem.JAR_SEPARATOR.length()));
            if (vFile != null) {
              final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vFile);
              if (psiFile != null) {
                elements[idx] = psiFile;
              }
            }
          }
        }
      }
      return elements;
    }

  }

  private final class MyPanel extends JPanel implements DataProvider {
    MyPanel() {
      super(new BorderLayout());
      Collection<AbstractProjectViewPane> snapshot = new ArrayList<>(myId2Pane.values());
      UIUtil.putClientProperty(
        this, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, (Iterable<JComponent>)() -> JBIterable.from(snapshot)
          .map(pane -> {
            JComponent last = null;
            for (Component c : UIUtil.uiParents(pane.getComponentToFocus(), false)) {
              if (c == this || !(c instanceof JComponent)) return null;
              last = (JComponent)c;
            }
            return last;
          })
          .filter(Conditions.notNull())
          .iterator());
    }

    @Nullable
    private Object getSelectedNodeElement() {
      final AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
      if (currentProjectViewPane == null) { // can happen if not initialized yet
        return null;
      }
      NodeDescriptor descriptor = TreeUtil.getLastUserObject(NodeDescriptor.class, currentProjectViewPane.getSelectedPath());
      if (descriptor == null) {
        return null;
      }
      return descriptor instanceof AbstractTreeNode
             ? ((AbstractTreeNode)descriptor).getValue()
             : descriptor.getElement();
    }

    @Override
    public Object getData(@NotNull String dataId) {
      final AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
      if (currentProjectViewPane != null) {
        final Object paneSpecificData = currentProjectViewPane.getData(dataId);
        if (paneSpecificData != null) return paneSpecificData;
      }

      if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
        if (currentProjectViewPane == null) return null;
        final PsiElement[] elements = currentProjectViewPane.getSelectedPSIElements();
        return elements.length == 1 ? elements[0] : null;
      }
      if (LangDataKeys.PSI_ELEMENT_ARRAY.is(dataId)) {
        if (currentProjectViewPane == null) {
          return null;
        }
        PsiElement[] elements = currentProjectViewPane.getSelectedPSIElements();
        return elements.length == 0 ? null : elements;
      }
      if (LangDataKeys.MODULE.is(dataId)) {
        VirtualFile[] virtualFiles = (VirtualFile[])getData(CommonDataKeys.VIRTUAL_FILE_ARRAY.getName());
        if (virtualFiles == null || virtualFiles.length <= 1) return null;
        final Set<Module> modules = new HashSet<>();
        for (VirtualFile virtualFile : virtualFiles) {
          modules.add(ModuleUtilCore.findModuleForFile(virtualFile, myProject));
        }
        return modules.size() == 1 ? modules.iterator().next() : null;
      }
      if (LangDataKeys.TARGET_PSI_ELEMENT.is(dataId)) {
        return null;
      }
      if (PlatformDataKeys.CUT_PROVIDER.is(dataId)) {
        return myCopyPasteDelegator.getCutProvider();
      }
      if (PlatformDataKeys.COPY_PROVIDER.is(dataId)) {
        return myCopyPasteDelegator.getCopyProvider();
      }
      if (PlatformDataKeys.PASTE_PROVIDER.is(dataId)) {
        return myCopyPasteDelegator.getPasteProvider();
      }
      if (LangDataKeys.IDE_VIEW.is(dataId)) {
        return myIdeView;
      }
      if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.is(dataId)) {
        final Module[] modules = getSelectedModules();
        if (modules != null || !getSelectedUnloadedModules().isEmpty()) {
          return myDeleteModuleProvider;
        }
        final LibraryOrderEntry orderEntry = getSelectedLibrary();
        if (orderEntry != null) {
          return new DeleteProvider() {
            @Override
            public void deleteElement(@NotNull DataContext dataContext) {
              detachLibrary(orderEntry, myProject);
            }

            @Override
            public boolean canDeleteElement(@NotNull DataContext dataContext) {
              return true;
            }
          };
        }
        return myDeletePSIElementProvider;
      }
      if (PlatformDataKeys.HELP_ID.is(dataId)) {
        return HelpID.PROJECT_VIEWS;
      }
      if (DATA_KEY.is(dataId)) {
        return ProjectViewImpl.this;
      }
      if (PlatformDataKeys.PROJECT_CONTEXT.is(dataId)) {
        Object selected = getSelectedNodeElement();
        return selected instanceof Project ? selected : null;
      }
      if (LangDataKeys.MODULE_CONTEXT.is(dataId)) {
        Object selected = getSelectedNodeElement();
        if (selected instanceof Module) {
          return !((Module)selected).isDisposed() ? selected : null;
        }
        else if (selected instanceof PsiDirectory) {
          return moduleBySingleContentRoot(((PsiDirectory)selected).getVirtualFile());
        }
        else if (selected instanceof VirtualFile) {
          return moduleBySingleContentRoot((VirtualFile)selected);
        }
        else {
          return null;
        }
      }

      if (LangDataKeys.MODULE_CONTEXT_ARRAY.is(dataId)) {
        return getSelectedModules();
      }
      if (UNLOADED_MODULES_CONTEXT_KEY.is(dataId)) {
        return Collections.unmodifiableList(getSelectedUnloadedModules());
      }
      if (ModuleGroup.ARRAY_DATA_KEY.is(dataId)) {
        final List<ModuleGroup> selectedElements = getSelectedElements(ModuleGroup.class);
        return selectedElements.isEmpty() ? null : selectedElements.toArray(new ModuleGroup[0]);
      }
      if (LibraryGroupElement.ARRAY_DATA_KEY.is(dataId)) {
        final List<LibraryGroupElement> selectedElements = getSelectedElements(LibraryGroupElement.class);
        return selectedElements.isEmpty() ? null : selectedElements.toArray(new LibraryGroupElement[0]);
      }
      if (NamedLibraryElement.ARRAY_DATA_KEY.is(dataId)) {
        final List<NamedLibraryElement> selectedElements = getSelectedElements(NamedLibraryElement.class);
        return selectedElements.isEmpty() ? null : selectedElements.toArray(new NamedLibraryElement[0]);
      }

      if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
        final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
        return viewPane == null ?  null : viewPane.getSelectedElements();
      }

      if (QuickActionProvider.KEY.is(dataId)) {
        return ProjectViewImpl.this;
      }

      return null;
    }

    @Nullable
    private LibraryOrderEntry getSelectedLibrary() {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      if (viewPane == null) return null;
      TreePath path = viewPane.getSelectedPath();
      if (path == null) return null;
      TreePath parent = path.getParentPath();
      if (parent == null) return null;
      Object userObject = TreeUtil.getLastUserObject(parent);
      if (userObject instanceof LibraryGroupNode) {
        userObject = TreeUtil.getLastUserObject(path);
        if (userObject instanceof NamedLibraryElementNode) {
          NamedLibraryElement element = ((NamedLibraryElementNode)userObject).getValue();
          OrderEntry orderEntry = element.getOrderEntry();
          return orderEntry instanceof LibraryOrderEntry ? (LibraryOrderEntry)orderEntry : null;
        }
        PsiDirectory directory = ((PsiDirectoryNode)userObject).getValue();
        VirtualFile virtualFile = directory.getVirtualFile();
        Module module = (Module)TreeUtil.getLastUserObject(AbstractTreeNode.class, parent.getParentPath()).getValue();

        if (module == null) return null;
        ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
        OrderEntry entry = index.getOrderEntryForFile(virtualFile);
        if (entry instanceof LibraryOrderEntry) {
          return (LibraryOrderEntry)entry;
        }
      }

      return null;
    }

    private void detachLibrary(@NotNull final LibraryOrderEntry orderEntry, @NotNull Project project) {
      final Module module = orderEntry.getOwnerModule();
      String message = IdeBundle.message("detach.library.from.module", orderEntry.getPresentableName(), module.getName());
      String title = IdeBundle.message("detach.library");
      int ret = Messages.showOkCancelDialog(project, message, title, Messages.getQuestionIcon());
      if (ret != Messages.OK) return;
      CommandProcessor.getInstance().executeCommand(module.getProject(), () -> {
        final Runnable action = () -> {
          ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
          OrderEntry[] orderEntries = rootManager.getOrderEntries();
          ModifiableRootModel model = rootManager.getModifiableModel();
          OrderEntry[] modifiableEntries = model.getOrderEntries();
          for (int i = 0; i < orderEntries.length; i++) {
            OrderEntry entry = orderEntries[i];
            if (entry instanceof LibraryOrderEntry && ((LibraryOrderEntry)entry).getLibrary() == orderEntry.getLibrary()) {
              model.removeOrderEntry(modifiableEntries[i]);
            }
          }
          model.commit();
        };
        ApplicationManager.getApplication().runWriteAction(action);
      }, title, null);
    }

    @Nullable
    private Module[] getSelectedModules() {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      if (viewPane == null) return null;
      final Object[] elements = viewPane.getSelectedElements();
      ArrayList<Module> result = new ArrayList<>();
      for (Object element : elements) {
        if (element instanceof Module) {
          final Module module = (Module)element;
          if (!module.isDisposed()) {
            result.add(module);
          }
        }
        else if (element instanceof ModuleGroup) {
          Collection<Module> modules = ((ModuleGroup)element).modulesInGroup(myProject, true);
          result.addAll(modules);
        }
        else if (element instanceof PsiDirectory) {
          Module module = moduleBySingleContentRoot(((PsiDirectory)element).getVirtualFile());
          if (module != null) result.add(module);
        }
        else if (element instanceof VirtualFile) {
          Module module = moduleBySingleContentRoot((VirtualFile)element);
          if (module != null) result.add(module);
        }
      }

      return result.isEmpty() ? null : result.toArray(Module.EMPTY_ARRAY);
    }

    private List<UnloadedModuleDescription> getSelectedUnloadedModules() {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      if (viewPane == null) return Collections.emptyList();
      List<UnloadedModuleDescription> result = new SmartList<>();
      for (Object element : viewPane.getSelectedElements()) {
        if (element instanceof PsiDirectory) {
          ContainerUtil.addIfNotNull(result, getUnloadedModuleByContentRoot(((PsiDirectory)element).getVirtualFile()));
        }
        else if (element instanceof VirtualFile) {
          ContainerUtil.addIfNotNull(result, getUnloadedModuleByContentRoot((VirtualFile)element));
        }
      }
      return result;
    }
  }

  /** Project view has the same node for module and its single content root
   *   => MODULE_CONTEXT data key should return the module when its content root is selected
   *  When there are multiple content roots, they have different nodes under the module node
   *   => MODULE_CONTEXT should be only available for the module node
   *      otherwise VirtualFileArrayRule will return all module's content roots when just one of them is selected
   */
  @Nullable
  private Module moduleBySingleContentRoot(@NotNull VirtualFile file) {
    if (ProjectRootsUtil.isModuleContentRoot(file, myProject)) {
      Module module = ProjectRootManager.getInstance(myProject).getFileIndex().getModuleForFile(file);
      if (module != null && !module.isDisposed() && ModuleRootManager.getInstance(module).getContentRoots().length == 1) {
        return module;
      }
    }

    return null;
  }

  @Nullable
  private UnloadedModuleDescription getUnloadedModuleByContentRoot(@NotNull VirtualFile file) {
    String moduleName = ProjectRootsUtil.findUnloadedModuleByContentRoot(file, myProject);
    if (moduleName != null) {
      return ModuleManager.getInstance(myProject).getUnloadedModuleDescription(moduleName);
    }
    return null;
  }

  @NotNull
  private <T> List<T> getSelectedElements(@NotNull Class<T> klass) {
    List<T> result = new ArrayList<>();
    final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
    if (viewPane == null) return result;
    final Object[] elements = viewPane.getSelectedElements();
    for (Object element : elements) {
      //element still valid
      if (element != null && klass.isAssignableFrom(element.getClass())) {
        result.add((T)element);
      }
    }
    return result;
  }

  @Override
  public void selectPsiElement(@NotNull PsiElement element, boolean requestFocus) {
    VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
    select(element, virtualFile, requestFocus);
  }


  private static void readOption(@Nullable Element node, @NotNull Map<String, Boolean> options) {
    if (node == null) {
      return;
    }
    for (Attribute attribute : node.getAttributes()) {
      options.put(attribute.getName(), Boolean.parseBoolean(attribute.getValue()));
    }
  }

  private static void writeOption(@NotNull Element parentNode, @NotNull Map<String, Boolean> optionsForPanes, @NotNull String optionName) {
    if (optionsForPanes.isEmpty()) {
      return;
    }

    Element e = new Element(optionName);
    for (Map.Entry<String, Boolean> entry : optionsForPanes.entrySet()) {
      final String key = entry.getKey();
      //SCR48267
      if (key != null) {
        e.setAttribute(key, Boolean.toString(entry.getValue()));
      }
    }

    parentNode.addContent(e);
  }

  @Override
  public void loadState(@NotNull Element parentNode) {
    Element navigatorElement = parentNode.getChild(ELEMENT_NAVIGATOR);
    if (navigatorElement != null) {
      mySavedPaneId = navigatorElement.getAttributeValue(ATTRIBUTE_CURRENT_VIEW);
      mySavedPaneSubId = navigatorElement.getAttributeValue(ATTRIBUTE_CURRENT_SUBVIEW);
      if (mySavedPaneId == null) {
        mySavedPaneId = getDefaultViewId();
        mySavedPaneSubId = null;
      }

      readOption(navigatorElement.getChild(ELEMENT_FLATTEN_PACKAGES), myFlattenPackages);
      readOption(navigatorElement.getChild(ELEMENT_SHOW_MEMBERS), myShowMembers);
      readOption(navigatorElement.getChild(ELEMENT_SHOW_MODULES), myShowModules);
      readOption(navigatorElement.getChild(ELEMENT_SHOW_EXCLUDED_FILES), myShowExcludedFiles);
      readOption(navigatorElement.getChild(ELEMENT_SHOW_LIBRARY_CONTENTS), myShowLibraryContents);
      readOption(navigatorElement.getChild(ELEMENT_HIDE_EMPTY_PACKAGES), myHideEmptyPackages);
      readOption(navigatorElement.getChild(ELEMENT_COMPACT_DIRECTORIES), myCompactDirectories);
      readOption(navigatorElement.getChild(ELEMENT_ABBREVIATE_PACKAGE_NAMES), myAbbreviatePackageNames);
      readOption(navigatorElement.getChild(ELEMENT_AUTOSCROLL_TO_SOURCE), myAutoscrollToSource);
      readOption(navigatorElement.getChild(ELEMENT_AUTOSCROLL_FROM_SOURCE), myAutoscrollFromSource);
      readOption(navigatorElement.getChild(ELEMENT_SORT_BY_TYPE), mySortByType);
      readOption(navigatorElement.getChild(ELEMENT_MANUAL_ORDER), myManualOrder);

      Element foldersElement = navigatorElement.getChild(ELEMENT_FOLDERS_ALWAYS_ON_TOP);
      if (foldersElement != null) myFoldersAlwaysOnTop = Boolean.valueOf(foldersElement.getAttributeValue("value"));

      try {
        splitterProportions.readExternal(navigatorElement);
      }
      catch (InvalidDataException ignored) {
      }
    }
    Element panesElement = parentNode.getChild(ELEMENT_PANES);
    if (panesElement != null) {
      readPaneState(panesElement);
    }
  }

  @NotNull
  public static String getDefaultViewId() {
    //noinspection SpellCheckingInspection
    if ("AndroidStudio".equals(PlatformUtils.getPlatformPrefix()) && !Boolean.getBoolean("studio.projectview")) {
      // the default in Android Studio unless studio.projectview is set: issuetracker.google.com/37091465
      return "AndroidView";
    }
    else {
      return ProjectViewPane.ID;
    }
  }

  private void readPaneState(@NotNull Element panesElement) {
    final List<Element> paneElements = panesElement.getChildren(ELEMENT_PANE);

    for (Element paneElement : paneElements) {
      String paneId = paneElement.getAttributeValue(ATTRIBUTE_ID);
      if (StringUtil.isEmptyOrSpaces(paneId)) {
        continue;
      }

      final AbstractProjectViewPane pane = myId2Pane.get(paneId);
      if (pane != null) {
        try {
          pane.readExternal(paneElement);
        }
        catch (InvalidDataException ignore) {
        }
      }
      else {
        myUninitializedPaneState.put(paneId, paneElement);
      }
    }
  }

  @Override
  public Element getState() {
    Element parentNode = new Element("projectView");
    Element navigatorElement = new Element(ELEMENT_NAVIGATOR);

    AbstractProjectViewPane currentPane = getCurrentProjectViewPane();
    if (currentPane != null) {
      String subId = currentPane.getSubId();
      if (subId != null || !currentPane.getId().equals(getDefaultViewId())) {
        navigatorElement.setAttribute(ATTRIBUTE_CURRENT_VIEW, currentPane.getId());
        if (subId != null) {
          navigatorElement.setAttribute(ATTRIBUTE_CURRENT_SUBVIEW, subId);
        }
      }
    }

    writeOption(navigatorElement, myFlattenPackages, ELEMENT_FLATTEN_PACKAGES);
    writeOption(navigatorElement, myShowMembers, ELEMENT_SHOW_MEMBERS);
    writeOption(navigatorElement, myShowModules, ELEMENT_SHOW_MODULES);
    writeOption(navigatorElement, myShowExcludedFiles, ELEMENT_SHOW_EXCLUDED_FILES);
    writeOption(navigatorElement, myShowLibraryContents, ELEMENT_SHOW_LIBRARY_CONTENTS);
    writeOption(navigatorElement, myHideEmptyPackages, ELEMENT_HIDE_EMPTY_PACKAGES);
    writeOption(navigatorElement, myCompactDirectories, ELEMENT_COMPACT_DIRECTORIES);
    writeOption(navigatorElement, myAbbreviatePackageNames, ELEMENT_ABBREVIATE_PACKAGE_NAMES);
    writeOption(navigatorElement, myAutoscrollToSource, ELEMENT_AUTOSCROLL_TO_SOURCE);
    writeOption(navigatorElement, myAutoscrollFromSource, ELEMENT_AUTOSCROLL_FROM_SOURCE);
    writeOption(navigatorElement, mySortByType, ELEMENT_SORT_BY_TYPE);
    writeOption(navigatorElement, myManualOrder, ELEMENT_MANUAL_ORDER);

    Element foldersElement = new Element(ELEMENT_FOLDERS_ALWAYS_ON_TOP);
    foldersElement.setAttribute("value", Boolean.toString(myFoldersAlwaysOnTop));
    navigatorElement.addContent(foldersElement);

    splitterProportions.saveSplitterProportions(myPanel);
    try {
      splitterProportions.writeExternal(navigatorElement);
    }
    catch (WriteExternalException ignored) {
    }

    if (!JDOMUtil.isEmpty(navigatorElement)) {
      parentNode.addContent(navigatorElement);
    }

    Element panesElement = new Element(ELEMENT_PANES);
    writePaneState(panesElement);
    parentNode.addContent(panesElement);
    return parentNode;
  }

  private void writePaneState(@NotNull Element panesElement) {
    for (AbstractProjectViewPane pane : myId2Pane.values()) {
      Element paneElement = new Element(ELEMENT_PANE);
      paneElement.setAttribute(ATTRIBUTE_ID, pane.getId());
      try {
        pane.writeExternal(paneElement);
      }
      catch (WriteExternalException e) {
        continue;
      }
      panesElement.addContent(paneElement);
    }
    for (Element element : myUninitializedPaneState.values()) {
      panesElement.addContent(element.clone());
    }
  }

  private static boolean isGlobalOptions() {
    return Registry.is("ide.projectView.globalOptions");
  }

  private static ProjectViewSharedSettings getGlobalOptions() {
    return ProjectViewSharedSettings.Companion.getInstance();
  }

  @Override
  public boolean isAutoscrollToSource(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getAutoscrollToSource();
    }

    return getPaneOptionValue(myAutoscrollToSource, paneId, UISettings.getInstance().getState().getDefaultAutoScrollToSource());
  }

  public void setAutoscrollToSource(boolean autoscrollMode, String paneId) {
    if (isGlobalOptions()) {
      getGlobalOptions().setAutoscrollToSource(autoscrollMode);
    }
    myAutoscrollToSource.put(paneId, autoscrollMode);
  }

  @Override
  public boolean isAutoscrollFromSource(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getAutoscrollFromSource();
    }

    return getPaneOptionValue(myAutoscrollFromSource, paneId, ourAutoscrollFromSourceDefaults);
  }

  public void setAutoscrollFromSource(boolean autoscrollMode, String paneId) {
    if (isGlobalOptions()) {
      getGlobalOptions().setAutoscrollFromSource(autoscrollMode);
    }
    setPaneOption(myAutoscrollFromSource, autoscrollMode, paneId, false);
  }

  @Override
  public boolean isFlattenPackages(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getFlattenPackages();
    }

    return getPaneOptionValue(myFlattenPackages, paneId, ourFlattenPackagesDefaults);
  }

  public void setFlattenPackages(String paneId, boolean flattenPackages) {
    if (isGlobalOptions()) {
      getGlobalOptions().setFlattenPackages(flattenPackages);
      for (String pane : myFlattenPackages.keySet()) {
        setPaneOption(myFlattenPackages, flattenPackages, pane, true);
      }
    }
    setPaneOption(myFlattenPackages, flattenPackages, paneId, true);
  }

  @Override
  public boolean isFoldersAlwaysOnTop(String paneId) {
    //noinspection deprecation
    return isFoldersAlwaysOnTop();
  }

  /**
   * @deprecated use {@link ProjectView#isFoldersAlwaysOnTop(String)} instead
   */
  @Deprecated
  @SuppressWarnings("DeprecatedIsStillUsed")
  public boolean isFoldersAlwaysOnTop() {
    if (isGlobalOptions()) {
      return getGlobalOptions().getFoldersAlwaysOnTop();
    }

    return myFoldersAlwaysOnTop;
  }

  public void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop) {
    if (isGlobalOptions()) {
      getGlobalOptions().setFoldersAlwaysOnTop(foldersAlwaysOnTop);
    }

    if (myFoldersAlwaysOnTop != foldersAlwaysOnTop) {
      myFoldersAlwaysOnTop = foldersAlwaysOnTop;
      for (AbstractProjectViewPane pane : myId2Pane.values()) {
        if (pane.getTree() != null) {
          pane.updateFromRoot(false);
        }
      }
    }
  }

  @Override
  public boolean isShowMembers(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getShowMembers();
    }

    return getPaneOptionValue(myShowMembers, paneId, ourShowMembersDefaults);
  }

  @Override
  public boolean isHideEmptyMiddlePackages(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getHideEmptyPackages();
    }

    return getPaneOptionValue(myHideEmptyPackages, paneId, ourHideEmptyPackagesDefaults);
  }

  @Override
  public boolean isAbbreviatePackageNames(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getAbbreviatePackages();
    }

    return getPaneOptionValue(myAbbreviatePackageNames, paneId, ourAbbreviatePackagesDefaults);
  }

  @Override
  public boolean isShowExcludedFiles(String paneId) {
    boolean showExcludedFiles = isGlobalOptions()
                                ? getGlobalOptions().getShowExcludedFiles()
                                : getPaneOptionValue(myShowExcludedFiles, paneId, ourShowExcludedFilesDefaults);

    if (showExcludedFiles == ourShowExcludedFilesDefaults && EventQueue.isDispatchThread()) {
      AbstractProjectViewPane pane = getProjectViewPaneById(ProjectViewPane.ID);
      if (pane instanceof ProjectViewPane) {
        ProjectViewPane old = (ProjectViewPane)pane;
        showExcludedFiles = old.myShowExcludedFiles;
        if (showExcludedFiles != ourShowExcludedFilesDefaults) {
          setShowExcludedFiles(paneId, showExcludedFiles, false);
          setPaneOption(myShowExcludedFiles, showExcludedFiles, ProjectViewPane.ID, false);
          old.myShowExcludedFiles = ourShowExcludedFilesDefaults; // reset old state after copying it
        }
      }
    }
    return showExcludedFiles;
  }

  void setShowExcludedFiles(@NotNull String paneId, boolean showExcludedFiles, boolean updatePane) {
    if (isGlobalOptions()) {
      getGlobalOptions().setShowExcludedFiles(showExcludedFiles);
      for (String id : getPaneIds()) {
        if (ShowExcludedFilesAction.INSTANCE.isSupported(this, id)) {
          setPaneOption(myShowExcludedFiles, showExcludedFiles, id, updatePane);
        }
      }
    }
    else if (ShowExcludedFilesAction.INSTANCE.isSupported(this, paneId)) {
      setPaneOption(myShowExcludedFiles, showExcludedFiles, paneId, updatePane);
    }
  }

  @Override
  public boolean isShowLibraryContents(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getShowLibraryContents();
    }

    return getPaneOptionValue(myShowLibraryContents, paneId, ourShowLibraryContentsDefaults);
  }

  @Override
  public void setShowLibraryContents(@NotNull String paneId, boolean showLibraryContents) {
    if (isGlobalOptions()) {
      getGlobalOptions().setShowLibraryContents(showLibraryContents);
    }
    setPaneOption(myShowLibraryContents, showLibraryContents, paneId, true);
  }

  @NotNull
  public ActionCallback setShowLibraryContentsCB(String paneId, boolean showLibraryContents) {
    return setPaneOption(myShowLibraryContents, showLibraryContents, paneId, true);
  }

  @Override
  public boolean isShowModules(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getShowModules();
    }

    return getPaneOptionValue(myShowModules, paneId, ourShowModulesDefaults);
  }

  @Override
  public void setShowModules(@NotNull String paneId, boolean showModules) {
    if (isGlobalOptions()) {
      getGlobalOptions().setShowModules(showModules);
    }
    setPaneOption(myShowModules, showModules, paneId, true);
  }

  @Override
  public boolean isFlattenModules(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getFlattenModules();
    }

    return getPaneOptionValue(myFlattenModules, paneId, ourFlattenModulesDefaults);
  }

  @Override
  public void setFlattenModules(@NotNull String paneId, boolean flattenModules) {
    if (isGlobalOptions()) {
      getGlobalOptions().setFlattenModules(flattenModules);
    }
    setPaneOption(myFlattenModules, flattenModules, paneId, true);
  }

  @Override
  public boolean isShowURL(String paneId) {
    return Registry.is("project.tree.structure.show.url");
  }

  @Override
  public void setHideEmptyPackages(@NotNull String paneId, boolean hideEmptyPackages) {
    if (isGlobalOptions()) {
      getGlobalOptions().setHideEmptyPackages(hideEmptyPackages);
      for (String pane : myHideEmptyPackages.keySet()) {
        setPaneOption(myHideEmptyPackages, hideEmptyPackages, pane, true);
      }
    }
    setPaneOption(myHideEmptyPackages, hideEmptyPackages, paneId, true);
  }

  @Override
  public boolean isCompactDirectories(String paneId) {
    return isGlobalOptions()
           ? getGlobalOptions().getCompactDirectories()
           : getPaneOptionValue(myCompactDirectories, paneId, ourCompactDirectoriesDefaults);
  }

  @Override
  public void setCompactDirectories(@NotNull String paneId, boolean compactDirectories) {
    if (isGlobalOptions()) {
      getGlobalOptions().setCompactDirectories(compactDirectories);
      for (String pane: myCompactDirectories.keySet()) {
        setPaneOption(myCompactDirectories, compactDirectories, pane, true);
      }
    }
    setPaneOption(myCompactDirectories, compactDirectories, paneId, true);
  }

  @Override
  public void setAbbreviatePackageNames(@NotNull String paneId, boolean abbreviatePackageNames) {
    if (isGlobalOptions()) {
      getGlobalOptions().setAbbreviatePackages(abbreviatePackageNames);
    }
    setPaneOption(myAbbreviatePackageNames, abbreviatePackageNames, paneId, true);
  }

  @NotNull
  private ActionCallback setPaneOption(@NotNull Map<String, Boolean> optionsMap, boolean value, String paneId, final boolean updatePane) {
    if (paneId != null) {
      optionsMap.put(paneId, value);
      if (updatePane) {
        final AbstractProjectViewPane pane = getProjectViewPaneById(paneId);
        if (pane != null) {
          return pane.updateFromRoot(false);
        }
      }
    }
    return ActionCallback.DONE;
  }

  private static boolean getPaneOptionValue(@NotNull Map<String, Boolean> optionsMap, String paneId, boolean defaultValue) {
    final Boolean value = optionsMap.get(paneId);
    return value == null ? defaultValue : value.booleanValue();
  }

  private class HideEmptyMiddlePackagesAction extends PaneOptionAction {
    private HideEmptyMiddlePackagesAction() {
      super(myHideEmptyPackages, "", "", null, ourHideEmptyPackagesDefaults);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean flag) {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      final SelectionInfo selectionInfo = SelectionInfo.create(viewPane);

      if (isGlobalOptions()) {
        getGlobalOptions().setHideEmptyPackages(flag);
      }
      super.setSelected(event, flag);

      selectionInfo.apply(viewPane);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
      if (isGlobalOptions()) return getGlobalOptions().getHideEmptyPackages();
      return super.isSelected(event);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      // see com.intellij.ide.favoritesTreeView.actions.FavoritesCompactEmptyMiddlePackagesAction.updateButton
      if (isFlattenPackages(myCurrentViewId)) {
        presentation.setText(IdeBundle.message("action.hide.empty.middle.packages"));
        presentation.setDescription(IdeBundle.message("action.show.hide.empty.middle.packages"));
        presentation.setEnabledAndVisible(true);
      }
      else {
        presentation.setText(IdeBundle.message("action.compact.empty.middle.packages"));
        presentation.setDescription(IdeBundle.message("action.show.compact.empty.middle.packages"));
        presentation.setEnabledAndVisible(!ScopeViewPane.ID.equals(myCurrentViewId));
      }
    }
  }

  private static class SelectionInfo {
    @NotNull
    private final Object[] myElements;

    private SelectionInfo(@NotNull Object[] elements) {
      myElements = elements;
    }

    public void apply(final AbstractProjectViewPane viewPane) {
      if (viewPane == null) {
        return;
      }
      AbstractTreeBuilder treeBuilder = viewPane.getTreeBuilder();
      JTree tree = viewPane.myTree;
      if (treeBuilder != null) {
        DefaultTreeModel treeModel = (DefaultTreeModel)tree.getModel();
        List<TreePath> paths = new ArrayList<>(myElements.length);
        for (final Object element : myElements) {
          DefaultMutableTreeNode node = treeBuilder.getNodeForElement(element);
          if (node == null) {
            treeBuilder.buildNodeForElement(element);
            node = treeBuilder.getNodeForElement(element);
          }
          if (node != null) {
            paths.add(new TreePath(treeModel.getPathToRoot(node)));
          }
        }
        if (!paths.isEmpty()) {
          tree.setSelectionPaths(paths.toArray(new TreePath[0]));
        }
      }
      else {
        List<TreeVisitor> visitors = AbstractProjectViewPane.createVisitors(myElements);
        if (1 == visitors.size()) {
          TreeUtil.promiseSelect(tree, visitors.get(0));
        }
        else if (!visitors.isEmpty()) {
          TreeUtil.promiseSelect(tree, visitors.stream());
        }
      }
    }

    @NotNull
    public static SelectionInfo create(final AbstractProjectViewPane viewPane) {
      List<Object> selectedElements = Collections.emptyList();
      if (viewPane != null) {
        final TreePath[] selectionPaths = viewPane.getSelectionPaths();
        if (selectionPaths != null) {
          selectedElements = new ArrayList<>();
          for (TreePath path : selectionPaths) {
            NodeDescriptor descriptor = TreeUtil.getLastUserObject(NodeDescriptor.class, path);
            if (descriptor != null) selectedElements.add(descriptor.getElement());
          }
        }
      }
      return new SelectionInfo(selectedElements.toArray());
    }
  }

  private class MyAutoScrollFromSourceHandler extends AutoScrollFromSourceHandler {
    private MyAutoScrollFromSourceHandler() {
      super(ProjectViewImpl.this.myProject, myViewContentPanel, ProjectViewImpl.this);
    }

    @Override
    protected void selectElementFromEditor(@NotNull FileEditor fileEditor) {
      if (myProject.isDisposed() || !myViewContentPanel.isShowing()) return;
      if (isAutoscrollFromSource(getCurrentViewId()) && !isCurrentProjectViewPaneFocused()) {
        SimpleSelectInContext context = getSelectInContext(fileEditor);
        if (context != null) context.selectInCurrentTarget();
      }
    }

    void scrollFromSource() {
      SimpleSelectInContext context = findSelectInContext();
      if (context != null) context.selectInCurrentTarget();
    }

    @Nullable
    SimpleSelectInContext findSelectInContext() {
      FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
      SimpleSelectInContext context = getSelectInContext(fileEditorManager.getSelectedEditor());
      if (context != null) return context;
      for (FileEditor fileEditor : fileEditorManager.getSelectedEditors()) {
        context = getSelectInContext(fileEditor);
        if (context != null) return context;
      }
      return null;
    }

    @Nullable
    private SimpleSelectInContext getSelectInContext(@Nullable FileEditor fileEditor) {
      if (fileEditor instanceof TextEditor) {
        TextEditor textEditor = (TextEditor)fileEditor;
        PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(textEditor.getEditor().getDocument());
        return psiFile == null ? null : new EditorSelectInContext(psiFile, textEditor.getEditor());
      }
      PsiFile psiFile = getPsiFile(getVirtualFile(fileEditor));
      return psiFile == null ? null : new SimpleSelectInContext(psiFile);
    }

    private PsiFile getPsiFile(VirtualFile file) {
      return file == null || !file.isValid() ? null : PsiManager.getInstance(myProject).findFile(file);
    }

    private VirtualFile getVirtualFile(FileEditor fileEditor) {
      return fileEditor == null ? null : FileEditorManagerEx.getInstanceEx(myProject).getFile(fileEditor);
    }

    private boolean isCurrentProjectViewPaneFocused() {
      AbstractProjectViewPane pane = getCurrentProjectViewPane();
      return pane != null && IJSwingUtilities.hasFocus(pane.getComponentToFocus());
    }

    @Override
    protected boolean isAutoScrollEnabled() {
      return isAutoscrollFromSource(myCurrentViewId);
    }

    @Override
    protected void setAutoScrollEnabled(boolean state) {
      setAutoscrollFromSource(state, myCurrentViewId);
      if (state && !isCurrentProjectViewPaneFocused()) {
        scrollFromSource();
      }
      createToolbarActions();
    }
  }

  private class SimpleSelectInContext extends SmartSelectInContext {
    SimpleSelectInContext(@NotNull PsiFile psiFile) {
      super(psiFile, psiFile);
    }

    void selectInCurrentTarget() {
      SelectInTarget target = getCurrentSelectInTarget();
      if (target != null && getPsiFile() != null) {
        selectIn(target, false);
      }
    }

    @Override
    @NotNull
    public FileEditorProvider getFileEditorProvider() {
      return () -> ArrayUtil.getFirstElement(FileEditorManager.getInstance(myProject).openFile(getVirtualFile(), false));
    }
  }

  private class EditorSelectInContext extends SimpleSelectInContext {
    private final Editor editor;

    EditorSelectInContext(@NotNull PsiFile psiFile, @NotNull Editor editor) {
      super(psiFile);
      this.editor = editor;
    }

    @Override
    void selectInCurrentTarget() {
      PsiDocumentManager manager = PsiDocumentManager.getInstance(getProject());
      if (manager != null) manager.performLaterWhenAllCommitted(super::selectInCurrentTarget);
    }

    @Override
    public Object getSelectorInFile() {
      PsiFile file = getPsiFile();
      if (file != null) {
        int offset = editor.getCaretModel().getOffset();
        PsiDocumentManager manager = PsiDocumentManager.getInstance(getProject());
        LOG.assertTrue(manager.isCommitted(editor.getDocument()));
        PsiElement element = file.findElementAt(offset);
        if (element != null) return element;
      }
      return file;
    }
  }

  @Override
  public boolean isManualOrder(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getManualOrder();
    }
    return getPaneOptionValue(myManualOrder, paneId, ourManualOrderDefaults);
  }

  @Override
  public void setManualOrder(@NotNull String paneId, final boolean enabled) {
    if (isGlobalOptions()) {
      getGlobalOptions().setManualOrder(enabled);
    }
    setPaneOption(myManualOrder, enabled, paneId, false);
    final AbstractProjectViewPane pane = getProjectViewPaneById(paneId);
    pane.installComparator();
  }

  @Override
  public boolean isSortByType(String paneId) {
    return getPaneOptionValue(mySortByType, paneId, ourSortByTypeDefaults);
  }

  @Override
  public void setSortByType(@NotNull String paneId, final boolean sortByType) {
    setPaneOption(mySortByType, sortByType, paneId, false);
    final AbstractProjectViewPane pane = getProjectViewPaneById(paneId);
    pane.installComparator();
  }

  private class ManualOrderAction extends ToggleAction implements DumbAware {
    private ManualOrderAction() {
      super(IdeBundle.message("action.manual.order"), null, AllIcons.ObjectBrowser.Sorted);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
      return isManualOrder(getCurrentViewId());
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean flag) {
      setManualOrder(getCurrentViewId(), flag);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      AbstractProjectViewPane pane = getCurrentProjectViewPane();
      if (pane == null) {
        presentation.setEnabledAndVisible(false);
      }
      else {
        presentation.setEnabledAndVisible(pane.supportsManualOrder());
        presentation.setText(pane.getManualOrderOptionText());
      }
    }
  }

  private class SortByTypeAction extends ToggleAction implements DumbAware {
    private SortByTypeAction() {
      super(IdeBundle.message("action.sort.by.type"), IdeBundle.message("action.sort.by.type"), AllIcons.ObjectBrowser.SortByType);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
      return isSortByType(getCurrentViewId());
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean flag) {
      setSortByType(getCurrentViewId(), flag);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      AbstractProjectViewPane pane = getCurrentProjectViewPane();
      presentation.setVisible(pane != null && pane.supportsSortByType());
    }
  }

  private class FoldersAlwaysOnTopAction extends ToggleAction implements DumbAware {
    private FoldersAlwaysOnTopAction() {
      super("Folders Always on Top");
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
      return isFoldersAlwaysOnTop(getCurrentViewId());
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean flag) {
      setFoldersAlwaysOnTop(flag);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      AbstractProjectViewPane pane = getCurrentProjectViewPane();
      presentation.setEnabledAndVisible(pane != null && pane.supportsFoldersAlwaysOnTop());
    }
  }

  private class ScrollFromSourceAction extends AnAction implements DumbAware {
    private ScrollFromSourceAction() {
      super("Scroll from Source", "Select the file open in the active editor", AllIcons.General.Locate);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myAutoScrollFromSourceHandler.scrollFromSource();
    }
  }

  @NotNull
  @Override
  public Collection<String> getPaneIds() {
    return Collections.unmodifiableCollection(myId2Pane.keySet());
  }

  @NotNull
  @Override
  public Collection<SelectInTarget> getSelectInTargets() {
    ensurePanesLoaded();
    return mySelectInTargets.values();
  }

  @NotNull
  @Override
  public ActionCallback getReady(@NotNull Object requestor) {
    AbstractProjectViewPane pane = myId2Pane.get(myCurrentViewSubId);
    if (pane == null) {
      pane = myId2Pane.get(myCurrentViewId);
    }
    return pane != null ? pane.getReady(requestor) : ActionCallback.DONE;
  }
}
