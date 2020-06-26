// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.application.options.OptionsApplicabilityFilter;
import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.ide.impl.ProjectViewSelectInGroupTarget;
import com.intellij.ide.impl.ProjectViewSelectInTarget;
import com.intellij.ide.projectView.HelpID;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.nodes.*;
import com.intellij.ide.ui.SplitterProportionsDataImpl;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.idea.ActionsBundle;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.keymap.KeymapUtil;
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowContentUiType;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.ui.switcher.QuickActionProvider;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.util.*;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static com.intellij.application.options.OptionId.PROJECT_VIEW_SHOW_VISIBILITY_ICONS;
import static com.intellij.ui.tree.TreePathUtil.toTreePathArray;

@State(name = "ProjectView", storages = {
  @Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE),
  @Storage(value = StoragePathMacros.WORKSPACE_FILE, deprecated = true)
})
public class ProjectViewImpl extends ProjectView implements PersistentStateComponent<Element>, QuickActionProvider, BusyObject {
  private static final Logger LOG = Logger.getInstance(ProjectViewImpl.class);
  private static final Key<String> ID_KEY = Key.create("pane-id");
  private static final Key<String> SUB_ID_KEY = Key.create("pane-sub-id");

  private final CopyPasteDelegator myCopyPasteDelegator;
  private boolean isInitialized;
  private final AtomicBoolean myExtensionsLoaded = new AtomicBoolean(false);
  @NotNull private final Project myProject;

  private final ProjectViewState myCurrentState;
  // + options
  private final Option myAbbreviatePackageNames = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return myFlattenPackages.isSelected() && myFlattenPackages.isEnabled(pane) && pane.supportsAbbreviatePackageNames();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getAbbreviatePackageNames();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setAbbreviatePackageNames(selected);
      getDefaultState().setAbbreviatePackageNames(selected);
      getGlobalOptions().setAbbreviatePackages(selected);
      if (updated) updatePanes(false);
    }
  };

  private final Option myAutoscrollFromSource = new Option() {
    @Override
    public boolean isSelected() {
      return myCurrentState.getAutoscrollFromSource();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      myCurrentState.setAutoscrollFromSource(selected);
      getDefaultState().setAutoscrollFromSource(selected);
      getGlobalOptions().setAutoscrollFromSource(selected);
      if (selected && !myAutoScrollFromSourceHandler.isCurrentProjectViewPaneFocused()) {
        myAutoScrollFromSourceHandler.scrollFromSource();
      }
    }
  };

  private final Option myAutoscrollToSource = new Option() {
    @Override
    public boolean isSelected() {
      return myCurrentState.getAutoscrollToSource();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      myCurrentState.setAutoscrollToSource(selected);
      getDefaultState().setAutoscrollToSource(selected);
      getGlobalOptions().setAutoscrollToSource(selected);
    }
  };

  private final Option myCompactDirectories = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return pane.supportsCompactDirectories();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getCompactDirectories();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setCompactDirectories(selected);
      getDefaultState().setCompactDirectories(selected);
      getGlobalOptions().setCompactDirectories(selected);
      if (updated) updatePanes(false);
    }
  };

  private final Option myFlattenModules = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return pane.supportsFlattenModules();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getFlattenModules();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setFlattenModules(selected);
      getDefaultState().setFlattenModules(selected);
      getGlobalOptions().setFlattenModules(selected);
      if (updated) updatePanes(false);
    }
  };

  private final Option myFlattenPackages = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return ProjectViewDirectoryHelper.getInstance(myProject).supportsFlattenPackages();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getFlattenPackages();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setFlattenPackages(selected);
      getDefaultState().setFlattenPackages(selected);
      getGlobalOptions().setFlattenPackages(selected);
      if (updated) updatePanes(false);
    }
  };

  private final Option myFoldersAlwaysOnTop = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return pane.supportsFoldersAlwaysOnTop();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getFoldersAlwaysOnTop();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setFoldersAlwaysOnTop(selected);
      getDefaultState().setFoldersAlwaysOnTop(selected);
      getGlobalOptions().setFoldersAlwaysOnTop(selected);
      if (updated) updatePanes(true);
    }
  };

  private final Option myHideEmptyMiddlePackages = new Option() {
    @NotNull
    @Override
    public String getName() {
      return myFlattenPackages.isSelected()
             ? IdeBundle.message("action.hide.empty.middle.packages")
             : IdeBundle.message("action.compact.empty.middle.packages");
    }

    @Override
    public String getDescription() {
      return myFlattenPackages.isSelected()
             ? IdeBundle.message("action.show.hide.empty.middle.packages")
             : IdeBundle.message("action.show.compact.empty.middle.packages");
    }

    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return pane.supportsHideEmptyMiddlePackages() && ProjectViewDirectoryHelper.getInstance(myProject).supportsHideEmptyMiddlePackages();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getHideEmptyMiddlePackages();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setHideEmptyMiddlePackages(selected);
      getDefaultState().setHideEmptyMiddlePackages(selected);
      getGlobalOptions().setHideEmptyPackages(selected);
      if (updated) updatePanes(false);
    }
  };

  private final Option myManualOrder = new Option() {
    @NotNull
    @Override
    public String getName() {
      AbstractProjectViewPane pane = getCurrentProjectViewPane();
      return pane != null
             ? pane.getManualOrderOptionText()
             : IdeBundle.message("action.manual.order");
    }

    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return pane.supportsManualOrder();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getManualOrder();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setManualOrder(selected);
      getDefaultState().setManualOrder(selected);
      getGlobalOptions().setManualOrder(selected);
      if (updated) updatePanes(true);
    }
  };

  private final Option myShowExcludedFiles = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return pane.supportsShowExcludedFiles();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getShowExcludedFiles();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setShowExcludedFiles(selected);
      getDefaultState().setShowExcludedFiles(selected);
      getGlobalOptions().setShowExcludedFiles(selected);
      if (updated) updatePanes(false);
    }
  };

  private final Option myShowLibraryContents = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return pane.supportsShowLibraryContents();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getShowLibraryContents();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setShowLibraryContents(selected);
      getDefaultState().setShowLibraryContents(selected);
      getGlobalOptions().setShowLibraryContents(selected);
      if (updated) updatePanes(false);
    }
  };

  private final Option myShowMembers = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return isShowMembersOptionSupported();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getShowMembers();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setShowMembers(selected);
      getDefaultState().setShowMembers(selected);
      getGlobalOptions().setShowMembers(selected);
      if (updated) updatePanes(false);
    }
  };

  private final Option myShowModules = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return pane.supportsShowModules();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getShowModules();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setShowModules(selected);
      getDefaultState().setShowModules(selected);
      getGlobalOptions().setShowModules(selected);
      if (updated) updatePanes(false);
    }
  };

  private final Option myShowVisibilityIcons = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return OptionsApplicabilityFilter.isApplicable(PROJECT_VIEW_SHOW_VISIBILITY_ICONS);
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getShowVisibilityIcons();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setShowVisibilityIcons(selected);
      getDefaultState().setShowVisibilityIcons(selected);
      getGlobalOptions().setShowVisibilityIcons(selected);
      if (updated) updatePanes(false);
    }
  };

  private final Option mySortByType = new Option() {
    @Override
    public boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return pane.supportsSortByType();
    }

    @Override
    public boolean isSelected() {
      return myCurrentState.getSortByType();
    }

    @Override
    public void setSelected(boolean selected) {
      if (myProject.isDisposed()) return;
      boolean updated = selected != isSelected();
      myCurrentState.setSortByType(selected);
      getDefaultState().setSortByType(selected);
      getGlobalOptions().setSortByType(selected);
      if (updated) updatePanes(true);
    }
  };

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
  private @Nullable String mySavedPaneId = null;
  private @Nullable String mySavedPaneSubId;
  @NonNls private static final String ELEMENT_NAVIGATOR = "navigator";
  @NonNls private static final String ELEMENT_PANES = "panes";
  @NonNls private static final String ELEMENT_PANE = "pane";
  @NonNls private static final String ATTRIBUTE_CURRENT_VIEW = "currentView";
  @NonNls private static final String ATTRIBUTE_CURRENT_SUBVIEW = "currentSubView";

  private static final String ATTRIBUTE_ID = "id";
  private JPanel myViewContentPanel;
  private static final Comparator<AbstractProjectViewPane> PANE_WEIGHT_COMPARATOR = Comparator.comparingInt(AbstractProjectViewPane::getWeight);
  private final MyPanel myDataProvider;
  private final SplitterProportionsData splitterProportions = new SplitterProportionsDataImpl();
  private final Map<String, Element> myUninitializedPaneState = new THashMap<>();
  private final Map<String, SelectInTarget> mySelectInTargets = new LinkedHashMap<>();
  private ContentManager myContentManager;

  public ProjectViewImpl(@NotNull Project project) {
    myProject = project;
    myCurrentState = ProjectViewState.getInstance(project);

    constructUi();

    myAutoScrollFromSourceHandler = new MyAutoScrollFromSourceHandler();

    myDataProvider = new MyPanel();
    myDataProvider.add(myPanel, BorderLayout.CENTER);
    myCopyPasteDelegator = new CopyPasteDelegator(myProject, myPanel);
    myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
      @Override
      protected boolean isAutoScrollMode() {
        return myAutoscrollToSource.isSelected();
      }

      @Override
      protected void setAutoScrollMode(boolean state) {
        myAutoscrollToSource.setSelected(state);
      }

      @Override
      protected boolean isAutoScrollEnabledFor(@NotNull VirtualFile file) {
        if (!super.isAutoScrollEnabledFor(file)) return false;
        AbstractProjectViewPane pane = getCurrentProjectViewPane();
        return pane == null || pane.isAutoScrollEnabledFor(file);
      }

      @Override
      protected String getActionName() {
        return ActionsBundle.message("action.ProjectView.AutoscrollToSource.text" );
      }

      @Override
      protected String getActionDescription() {
        return ActionsBundle.message("action.ProjectView.AutoscrollToSource.description" );
      }
    };

    project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
      @Override
      public void toolWindowShown(@NotNull String id, @NotNull ToolWindow toolWindow) {
        if (ToolWindowId.PROJECT_VIEW.equals(id)) {
          myCurrentSelectionObsolete = ThreeState.NO;
          AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
          if (currentProjectViewPane != null && isAutoscrollFromSource(currentProjectViewPane.getId())) {
            SimpleSelectInContext context = myAutoScrollFromSourceHandler.findSelectInContext();
            if (context != null) {
              myCurrentSelectionObsolete = ThreeState.UNSURE;
              context.selectInCurrentTarget();
            }
          }
        }
      }
    });

    AbstractProjectViewPane.EP.addExtensionPointListener(project, new ExtensionPointListener<AbstractProjectViewPane>() {
      @Override
      public void extensionAdded(@NotNull AbstractProjectViewPane extension, @NotNull PluginDescriptor pluginDescriptor) {
        reloadPanes();
      }

      @Override
      public void extensionRemoved(@NotNull AbstractProjectViewPane extension, @NotNull PluginDescriptor pluginDescriptor) {
        if (myId2Pane.containsKey(extension.getId()) || myUninitializedPanes.contains(extension)) {
          reloadPanes();
        }
        else {
          Disposer.dispose(extension);
        }
      }
    }, project);
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
    DefaultActionGroup views = DefaultActionGroup.createPopupGroup(() -> LangBundle.message("action.change.view.text"));

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

  private static ProjectViewState getDefaultState() {
    return ProjectViewState.getDefaultInstance();
  }

  private final class ChangeViewAction extends AnAction {
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

    Content[] contents = getContentManager().getContents();
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

    String savedPaneId = mySavedPaneId;
    if (savedPaneId == null) {
      savedPaneId = getDefaultViewId();
      mySavedPaneSubId = null;
    }

    // try to find saved selected view...
    for (Content content : contents) {
      String id = content.getUserData(ID_KEY);
      String subId = content.getUserData(SUB_ID_KEY);
      if (id != null &&
          id.equals(savedPaneId) &&
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
      final String title = subId != null ? newPane.getPresentableSubIdName(subId) : newPane.getTitle();
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
    createToolbarActions(newPane);

    myAutoScrollToSourceHandler.install(newPane.myTree);

    newPane.restoreExpandedPaths();
    if (selectedPsiElement != null && newSubId != null) {
      final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(selectedPsiElement);
      ProjectViewSelectInTarget target = virtualFile == null ? null : getProjectViewSelectInTarget(newPane);
      if (target != null && target.isSubIdSelectable(newSubId, new FileSelectInContext(myProject, virtualFile, null))) {
        newPane.select(selectedPsiElement, virtualFile, true);
      }
    }
    myAutoScrollToSourceHandler.onMouseClicked(newPane.myTree);
    myProject.getMessageBus().syncPublisher(ProjectViewListener.TOPIC).paneShown(newPane, currentPane);
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

    getContentManager().addContentManagerListener(new ContentManagerListener() {
      @Override
      public void selectionChanged(@NotNull ContentManagerEvent event) {
        if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
          viewSelectionChanged();
        }
      }
    });
    viewSelectionChanged();
  }

  private synchronized void reloadPanes() {
    if (myProject.isDisposed() || !myExtensionsLoaded.get()) return; // panes will be loaded later

    Map<String, AbstractProjectViewPane> newPanes = loadPanes();
    Map<AbstractProjectViewPane, Boolean> oldPanes = new IdentityHashMap<>();
    myUninitializedPanes.forEach(pane -> oldPanes.put(pane, pane == newPanes.get(pane.getId())));
    myId2Pane.forEach((id, pane) -> oldPanes.put(pane, pane == newPanes.get(id)));
    oldPanes.forEach((pane, exists) -> {
      if (Boolean.FALSE.equals(exists)) {
        removeProjectPane(pane);
        Disposer.dispose(pane);
      }
    });
    for (AbstractProjectViewPane pane : newPanes.values()) {
      if (!Boolean.TRUE.equals(oldPanes.get(pane)) && pane.isInitiallyVisible()) {
        addProjectPane(pane);
      }
    }
  }

  private void ensurePanesLoaded() {
    if (myProject.isDisposed() || myExtensionsLoaded.getAndSet(true)) return; // avoid recursive loading

    for (AbstractProjectViewPane pane : loadPanes().values()) {
      if (pane.isInitiallyVisible()) {
        addProjectPane(pane);
      }
    }
  }

  private Map<String, AbstractProjectViewPane> loadPanes() {
    HashMap<String, AbstractProjectViewPane> map = new LinkedHashMap<>();
    AbstractProjectViewPane.EP.getExtensions(myProject).stream().sorted(PANE_WEIGHT_COMPARATOR).forEach(pane -> {
      AbstractProjectViewPane added = map.computeIfAbsent(pane.getId(), id -> pane);
      if (pane != added) {
        LOG.warn("ignore duplicated pane with id=" + pane.getId() + "\nold " + added.getClass() + "\nnew " + pane.getClass());
      }
      else {
        Element element = myUninitializedPaneState.remove(pane.getId());
        if (element != null) applyPaneState(pane, element);
      }
    });
    return map;
  }

  private void viewSelectionChanged() {
    Content content = getContentManager().getSelectedContent();
    if (content == null) {
      return;
    }

    String id = content.getUserData(ID_KEY);
    String subId = content.getUserData(SUB_ID_KEY);
    if (Objects.equals(id, myCurrentViewId) && Objects.equals(subId, myCurrentViewSubId)) {
      return;
    }

    AbstractProjectViewPane newPane = getProjectViewPaneById(id);
    if (newPane == null) {
      return;
    }

    newPane.setSubId(subId);
    showPane(newPane);
    ProjectViewSelectInTarget target = getProjectViewSelectInTarget(newPane);
    if (target != null) {
      target.setSubId(subId);
    }
    if (isAutoscrollFromSource(id)) {
      myAutoScrollFromSourceHandler.scrollFromSource();
    }
  }

  private void createToolbarActions(@NotNull AbstractProjectViewPane pane) {
    if (myActionGroup == null) return;
    myActionGroup.removeAll();

    DefaultActionGroup group = (DefaultActionGroup)ActionManager.getInstance().getAction("ProjectView.ToolWindow.SecondaryActions");
    for (AnAction action : group.getChildActionsOrStubs()) {
      myActionGroup.addAction(action).setAsSecondary(true);
    }

    pane.addToolbarActions(myActionGroup);

    List<AnAction> titleActions = new SmartList<>();
    createTitleActions(titleActions);
    if (!titleActions.isEmpty()) {
      ToolWindowEx window = (ToolWindowEx)ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.PROJECT_VIEW);
      if (window != null) {
        window.setTitleActions(titleActions.toArray(AnAction.EMPTY_ARRAY));
      }
    }
  }

  protected void createTitleActions(@NotNull List<? super AnAction> titleActions) {
    titleActions.add(new ScrollFromSourceAction());
    AnAction collapseAllAction = CommonActionsManager.getInstance().createCollapseAllAction(new DefaultTreeExpander(() -> {
      AbstractProjectViewPane pane = getCurrentProjectViewPane();
      return pane == null ? null : pane.myTree;
    }) {
      @Override
      protected void collapseAll(@NotNull JTree tree, boolean strict, int keepSelectionLevel) {
        super.collapseAll(tree, false, keepSelectionLevel);
      }
    }, getComponent());
    collapseAllAction.getTemplatePresentation().setIcon(AllIcons.Actions.Collapseall);
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

    ProjectViewNode<?> descriptor = TreeUtil.getLastUserObject(ProjectViewNode.class, path);
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

  @Override
  public void changeView() {
    final List<AbstractProjectViewPane> views = new ArrayList<>(myId2Pane.values());
    views.remove(getCurrentProjectViewPane());
    views.sort(PANE_WEIGHT_COMPARATOR);

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
    else if (subId != null) {
      LOG.error("View doesn't have subviews: " + viewId + "; subId:" + subId + "; project: " + myProject);
    }

    if (viewId.equals(myCurrentViewId) && Objects.equals(subId, myCurrentViewSubId)) {
      return ActionCallback.REJECTED;
    }

    // at this point null subId means that view has no subviews OR subview was never selected
    // we then search first content with the right viewId ignoring subIds of contents
    for (Content content : getContentManager().getContents()) {
      if (viewId.equals(content.getUserData(ID_KEY)) && (subId == null || subId.equals(content.getUserData(SUB_ID_KEY)))) {
        return getContentManager().setSelectedContentCB(content);
      }
    }

    return ActionCallback.REJECTED;
  }

  private final class MyDeletePSIElementProvider extends ProjectViewDeleteElementProvider {
    @Override
    protected PsiElement @NotNull [] getSelectedPSIElements(@NotNull DataContext dataContext) {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      return viewPane.getSelectedPSIElements();
    }

    @Override
    protected Boolean hideEmptyMiddlePackages(@NotNull DataContext dataContext) {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      return isHideEmptyMiddlePackages(viewPane.getId());
    }
  }

  private final class MyPanel extends JPanel implements DataProvider {
    MyPanel() {
      super(new BorderLayout());
      Collection<AbstractProjectViewPane> snapshot = new ArrayList<>(myId2Pane.values());
      ComponentUtil.putClientProperty(this, UIUtil.NOT_IN_HIERARCHY_COMPONENTS,
                                      (Iterable<? extends Component>)(Iterable<JComponent>)() -> JBIterable.from(snapshot)
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
      NodeDescriptor<?> descriptor = TreeUtil.getLastUserObject(NodeDescriptor.class, currentProjectViewPane.getSelectedPath());
      if (descriptor == null) {
        return null;
      }

      return descriptor instanceof AbstractTreeNode
             ? ((AbstractTreeNode<?>)descriptor).getValue()
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
        return viewPane == null ? null : viewPane.getSelectedElements();
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

    private Module @Nullable [] getSelectedModules() {
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

  @Override
  public void loadState(@NotNull Element parentNode) {
    Element navigatorElement = parentNode.getChild(ELEMENT_NAVIGATOR);
    if (navigatorElement != null) {
      mySavedPaneId = navigatorElement.getAttributeValue(ATTRIBUTE_CURRENT_VIEW);
      if (mySavedPaneId == null) {
        mySavedPaneSubId = null;
      }
      else {
        mySavedPaneSubId = navigatorElement.getAttributeValue(ATTRIBUTE_CURRENT_SUBVIEW);
      }

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

  @Override
  public @NotNull String getDefaultViewId() {
    //noinspection SpellCheckingInspection
    if ("AndroidStudio".equals(PlatformUtils.getPlatformPrefix()) && !Boolean.getBoolean("studio.projectview")) {
      // the default in Android Studio unless studio.projectview is set: issuetracker.google.com/37091465
      return "AndroidView";
    }
    else {
      for (AbstractProjectViewPane extension : AbstractProjectViewPane.EP.getExtensions(myProject)) {
        if (extension.isDefaultPane(myProject)) {
          return extension.getId();
        }
      }
      return ProjectViewPane.ID;
    }
  }

  private void readPaneState(@NotNull Element panesElement) {
    List<Element> paneElements = panesElement.getChildren(ELEMENT_PANE);
    for (Element paneElement : paneElements) {
      String paneId = paneElement.getAttributeValue(ATTRIBUTE_ID);
      if (StringUtil.isEmptyOrSpaces(paneId)) {
        continue;
      }

      AbstractProjectViewPane pane = myId2Pane.get(paneId);
      if (pane != null) {
        applyPaneState(pane, paneElement);
      }
      else {
        myUninitializedPaneState.put(paneId, paneElement);
      }
    }
  }

  private static void applyPaneState(@NotNull AbstractProjectViewPane pane, @NotNull Element element) {
    try {
      pane.readExternal(element);
    }
    catch (InvalidDataException ignored) {
    }
  }

  @Override
  public Element getState() {
    Element parentNode = new Element("projectView");
    Element navigatorElement = new Element(ELEMENT_NAVIGATOR);

    AbstractProjectViewPane currentPane = getCurrentProjectViewPane();
    if (currentPane != null) {
      String subId = currentPane.getSubId();
      navigatorElement.setAttribute(ATTRIBUTE_CURRENT_VIEW, currentPane.getId());
      if (subId != null) {
        navigatorElement.setAttribute(ATTRIBUTE_CURRENT_SUBVIEW, subId);
      }
    }

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

  private static ProjectViewSharedSettings getGlobalOptions() {
    return ProjectViewSharedSettings.Companion.getInstance();
  }

  @Override
  public boolean isAutoscrollToSource(String paneId) {
    return myAutoscrollToSource.isSelected() && myAutoscrollToSource.isEnabled(paneId);
  }

  public void setAutoscrollToSource(boolean autoscrollMode, String paneId) {
    if (myAutoscrollToSource.isEnabled(paneId)) {
      myAutoscrollToSource.setSelected(autoscrollMode);
    }
  }

  @Override
  public boolean isAutoscrollFromSource(String paneId) {
    return myAutoscrollFromSource.isSelected() && myAutoscrollFromSource.isEnabled(paneId);
  }

  public void setAutoscrollFromSource(boolean autoscrollMode, String paneId) {
    if (myAutoscrollFromSource.isEnabled(paneId)) {
      myAutoscrollFromSource.setSelected(autoscrollMode);
    }
  }

  @Override
  public boolean isFlattenPackages(String paneId) {
    return myFlattenPackages.isSelected() && myFlattenPackages.isEnabled(paneId);
  }

  public void setFlattenPackages(String paneId, boolean flattenPackages) {
    if (myFlattenPackages.isEnabled(paneId)) {
      myFlattenPackages.setSelected(flattenPackages);
    }
  }

  @Override
  public boolean isFoldersAlwaysOnTop(String paneId) {
    return myFoldersAlwaysOnTop.isSelected() && myFoldersAlwaysOnTop.isEnabled(paneId);
  }

  /**
   * @deprecated use {@link ProjectView#isFoldersAlwaysOnTop(String)} instead
   */
  @Deprecated
  public boolean isFoldersAlwaysOnTop() {
    return myFoldersAlwaysOnTop.isSelected() && myFoldersAlwaysOnTop.isEnabled();
  }

  public void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop) {
    if (myFoldersAlwaysOnTop.isEnabled()) {
      myFoldersAlwaysOnTop.setSelected(foldersAlwaysOnTop);
    }
  }

  @Override
  public boolean isShowMembers(String paneId) {
    return myShowMembers.isSelected() && myShowMembers.isEnabled(paneId);
  }

  @Override
  public boolean isHideEmptyMiddlePackages(String paneId) {
    return myHideEmptyMiddlePackages.isSelected() && myHideEmptyMiddlePackages.isEnabled(paneId);
  }

  @Override
  public boolean isAbbreviatePackageNames(String paneId) {
    return myAbbreviatePackageNames.isSelected() && myAbbreviatePackageNames.isEnabled(paneId);
  }

  @Override
  public boolean isShowExcludedFiles(String paneId) {
    return myShowExcludedFiles.isSelected() && myShowExcludedFiles.isEnabled(paneId);
  }

  @Override
  public boolean isShowVisibilityIcons(String paneId) {
    return myShowVisibilityIcons.isSelected() && myShowVisibilityIcons.isEnabled(paneId);
  }

  @Override
  public boolean isShowLibraryContents(String paneId) {
    return myShowLibraryContents.isSelected() && myShowLibraryContents.isEnabled(paneId);
  }

  @Override
  public void setShowLibraryContents(@NotNull String paneId, boolean showLibraryContents) {
    if (myShowLibraryContents.isEnabled(paneId)) myShowLibraryContents.setSelected(showLibraryContents);
  }

  @Override
  public boolean isShowModules(String paneId) {
    return myShowModules.isSelected() && myShowModules.isEnabled(paneId);
  }

  @Override
  public void setShowModules(@NotNull String paneId, boolean showModules) {
    if (myShowModules.isEnabled(paneId)) myShowModules.setSelected(showModules);
  }

  @Override
  public boolean isFlattenModules(String paneId) {
    return myFlattenModules.isSelected() && myFlattenModules.isEnabled(paneId);
  }

  @Override
  public void setFlattenModules(@NotNull String paneId, boolean flattenModules) {
    if (myFlattenModules.isEnabled(paneId)) myFlattenModules.setSelected(flattenModules);
  }

  @Override
  public boolean isShowURL(String paneId) {
    return Registry.is("project.tree.structure.show.url");
  }

  @Override
  public void setHideEmptyPackages(@NotNull String paneId, boolean hideEmptyPackages) {
    if (myHideEmptyMiddlePackages.isEnabled(paneId)) myHideEmptyMiddlePackages.setSelected(hideEmptyPackages);
  }

  @Override
  public boolean isCompactDirectories(String paneId) {
    return myCompactDirectories.isSelected() && myCompactDirectories.isEnabled(paneId);
  }

  @Override
  public void setCompactDirectories(@NotNull String paneId, boolean compactDirectories) {
    if (myCompactDirectories.isEnabled(paneId)) myCompactDirectories.setSelected(compactDirectories);
  }

  @Override
  public void setAbbreviatePackageNames(@NotNull String paneId, boolean abbreviatePackageNames) {
    if (myAbbreviatePackageNames.isEnabled(paneId)) myAbbreviatePackageNames.setSelected(abbreviatePackageNames);
  }

  private static final class SelectionInfo {
    private final Object @NotNull [] myElements;

    private SelectionInfo(Object @NotNull [] elements) {
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
          tree.setSelectionPaths(toTreePathArray(paths));
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
      super(ProjectViewImpl.this.myProject, myViewContentPanel, ProjectViewImpl.this.myProject);
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
      return myAutoscrollFromSource.isSelected();
    }

    @Override
    protected void setAutoScrollEnabled(boolean state) {
      myAutoscrollFromSource.setSelected(state);
    }

    @Override
    protected String getActionName() {
      return ActionsBundle.message("action.ProjectView.AutoscrollFromSource.text");
    }

    @Override
    protected String getActionDescription() {
      return ActionsBundle.message("action.ProjectView.AutoscrollFromSource.description");
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
      if (PsiDocumentManager.getInstance(getProject()) == null) return;

      runWhenPsiAtCaretIsParsed(super::selectInCurrentTarget);
    }

    private void runWhenPsiAtCaretIsParsed(Runnable runnable) {
      int offset = editor.getCaretModel().getOffset();
      ReadAction
        .nonBlocking(() -> {
          PsiFile file = getPsiFile();
          return file == null ? null : file.findElementAt(offset);
        })
        .withDocumentsCommitted(getProject())
        .finishOnUiThread(ModalityState.defaultModalityState(), parsedLeaf -> {
          if (editor.getCaretModel().getOffset() != offset) {
            runWhenPsiAtCaretIsParsed(runnable);
          } else {
            runnable.run();
          }
        })
        .coalesceBy(EditorSelectInContext.class, ProjectViewImpl.this)
        .expireWhen(editor::isDisposed)
        .submit(AppExecutorUtil.getAppExecutorService());
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
    return myManualOrder.isSelected() && myManualOrder.isEnabled(paneId);
  }

  @Override
  public void setManualOrder(@NotNull String paneId, final boolean enabled) {
    if (myManualOrder.isEnabled(paneId)) myManualOrder.setSelected(enabled);
  }

  @Override
  public boolean isSortByType(String paneId) {
    return mySortByType.isSelected() && mySortByType.isEnabled(paneId);
  }

  @Override
  public void setSortByType(@NotNull String paneId, final boolean sortByType) {
    if (mySortByType.isEnabled(paneId)) mySortByType.setSelected(sortByType);
  }

  private class ScrollFromSourceAction extends AnAction implements DumbAware {
  ScrollFromSourceAction() {
    super(IdeBundle.messagePointer("action.AnAction.text.select.opened.file"),
          IdeBundle.messagePointer("action.AnAction.description.select.opened.file"), AllIcons.General.Locate);
  }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myAutoScrollFromSourceHandler.scrollFromSource();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
      Presentation presentation = event.getPresentation();
      presentation.setText(LangBundle.message("action.select.opened.file.text", getScrollToSourceShortcut()));
      presentation.setEnabledAndVisible(!isAutoscrollFromSource(myCurrentViewId));
    }
  }

  private String getScrollToSourceShortcut() {
    String selectInProjectViewShortcut = KeymapUtil.getFirstKeyboardShortcutText("SelectInProjectView");
    if (!selectInProjectViewShortcut.isEmpty()) {
      return " (" + selectInProjectViewShortcut + ")";
    }

    String selectInShortcut = KeymapUtil.getFirstKeyboardShortcutText("SelectIn");
    if (!selectInShortcut.isEmpty()) {
      List<SelectInTarget> targets = SelectInManager.getInstance(myProject).getTargetList();
      int index = ContainerUtil.indexOf(targets, (target) -> target instanceof ProjectViewSelectInGroupTarget);
      if (index >= 0) {
        return " (" + selectInShortcut + ", " + (index + 1) + ")";
      }
    }

    return "";
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

  private void updatePanes(boolean withComparator) {
    for (AbstractProjectViewPane pane : myId2Pane.values()) {
      JTree tree = pane.getTree();
      if (tree != null) {
        SelectionInfo info = pane.getId().equals(myCurrentViewId) ? SelectionInfo.create(pane) : null;
        if (withComparator) pane.installComparator();
        pane.updateFromRoot(false);
        if (info != null) info.apply(pane);
      }
    }
  }


  abstract class Option implements ToggleOptionAction.Option {
    @Override
    public boolean isEnabled() {
      return isEnabled(getCurrentViewId());
    }

    boolean isEnabled(@Nullable String paneId) {
      AbstractProjectViewPane pane = myId2Pane.get(paneId);
      return pane != null ? isEnabled(pane) : ApplicationManager.getApplication().isUnitTestMode();
    }

    boolean isEnabled(@NotNull AbstractProjectViewPane pane) {
      return true;
    }
  }

  static class Action extends ToggleOptionAction implements DumbAware {
    private Action(@NotNull Function<ProjectViewImpl, Option> optionSupplier) {
      super(event -> {
        Project project = event.getProject();
        ProjectView view = project == null || project.isDisposed() ? null : getInstance(project);
        return view instanceof ProjectViewImpl ? optionSupplier.apply((ProjectViewImpl)view) : null;
      });
    }

    static final class AbbreviatePackageNames extends Action {
      AbbreviatePackageNames() {
        super(view -> view.myAbbreviatePackageNames);
      }
    }

    static final class AutoscrollFromSource extends Action {
      AutoscrollFromSource() {
        super(view -> view.myAutoscrollFromSource);
      }
    }

    static final class AutoscrollToSource extends Action {
      AutoscrollToSource() {
        super(view -> view.myAutoscrollToSource);
      }
    }

    static final class CompactDirectories extends Action {
      CompactDirectories() {
        super(view -> view.myCompactDirectories);
      }
    }

    static final class FlattenModules extends Action {
      FlattenModules() {
        super(view -> view.myFlattenModules);
      }
    }

    static final class FlattenPackages extends Action {
      FlattenPackages() {
        super(view -> view.myFlattenPackages);
      }
    }

    static final class FoldersAlwaysOnTop extends Action {
      FoldersAlwaysOnTop() {
        super(view -> view.myFoldersAlwaysOnTop);
      }
    }

    static final class HideEmptyMiddlePackages extends Action {
      HideEmptyMiddlePackages() {
        super(view -> view.myHideEmptyMiddlePackages);
      }
    }

    static final class ManualOrder extends Action {
      ManualOrder() {
        super(view -> view.myManualOrder);
      }
    }

    static final class ShowExcludedFiles extends Action {
      ShowExcludedFiles() {
        super(view -> view.myShowExcludedFiles);
      }
    }

    static final class ShowLibraryContents extends Action {
      ShowLibraryContents() {
        super(view -> view.myShowLibraryContents);
      }
    }

    static final class ShowMembers extends Action {
      ShowMembers() {
        super(view -> view.myShowMembers);
      }
    }

    static final class ShowModules extends Action {
      ShowModules() {
        super(view -> view.myShowModules);
      }
    }

    static final class ShowVisibilityIcons extends Action {
      ShowVisibilityIcons() {
        super(view -> view.myShowVisibilityIcons);
      }
    }

    static final class SortByType extends Action {
      SortByType() {
        super(view -> view.mySortByType);
      }
    }
  }
}
