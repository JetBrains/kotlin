// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.execution.services.ServiceModelFilter.ServiceViewFilter;
import com.intellij.execution.services.ServiceViewDragHelper.ServiceViewDragBean;
import com.intellij.execution.services.ServiceViewModel.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.ui.content.*;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;

@State(name = "ServiceViewManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class ServiceViewManagerImpl implements ServiceViewManager, PersistentStateComponent<ServiceViewManagerImpl.State> {
  @NonNls private static final String HELP_ID = "services.tool.window";

  private final Project myProject;
  private State myState = new State();

  private final ServiceModel myModel;
  private final ServiceModelFilter myModelFilter;
  private boolean myActivationActionsRegistered;

  private ServiceView myAllServicesView;
  private ContentManager myContentManager;
  private AutoScrollToSourceHandler myAutoScrollToSourceHandler;

  public ServiceViewManagerImpl(@NotNull Project project) {
    myProject = project;
    myModel = new ServiceModel(myProject);
    Disposer.register(myProject, myModel);
    myModelFilter = new ServiceModelFilter();
    myProject.getMessageBus().connect(myModel).subscribe(ServiceEventListener.TOPIC, e -> myModel.refresh(e).onSuccess(o -> {
      updateToolWindow(!myModel.getRoots().isEmpty(), true);
      processAllModels(viewModel -> viewModel.eventProcessed(e));
    }));
    myModel.initRoots().onSuccess(o -> updateToolWindow(!myModel.getRoots().isEmpty(), false));
  }

  void createToolWindowContent(@NotNull ToolWindow toolWindow) {
    ContentManager contentManager = toolWindow.getContentManager();
    myContentManager = contentManager;
    contentManager.addContentManagerListener(new MyContentMangerListener(contentManager));

    ToolWindowEx toolWindowEx = (ToolWindowEx)toolWindow;
    myAutoScrollToSourceHandler = ServiceViewSourceScrollHelper.installAutoScrollSupport(myProject, toolWindowEx);

    Set<ServiceViewContributor> contributors = ContainerUtil.newHashSet(ServiceModel.getContributors());
    AllServicesModel mainModel = new AllServicesModel(myModel, myModelFilter, contributors);
    ServiceViewState mainState = prepareViewState(myState.allServicesViewState);
    myAllServicesView = createMainView(mainModel, mainState);
    loadViews(contentManager, myAllServicesView, contributors, myState.viewStates);

    ServiceViewDragHelper.installDnDSupport(myProject, toolWindowEx.getDecorator(), contentManager);
  }

  private ServiceView createMainView(ServiceViewModel viewModel, ServiceViewState viewState) {
    ServiceView mainView = ServiceView.createView(myProject, viewModel, viewState);

    Content mainContent = ContentFactory.SERVICE.getInstance().createContent(mainView, null, false);
    mainContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    mainContent.setHelpId(getToolWindowContextHelpId());
    mainContent.setCloseable(false);

    Disposer.register(mainContent, mainView);
    Disposer.register(mainContent, mainView.getModel());

    addContent(mainContent, false, -1);
    ContentManager contentManager = Objects.requireNonNull(mainContent.getManager());
    mainView.getModel().addModelListener(() -> {
      boolean isEmpty = mainView.getModel().getRoots().isEmpty();
      AppUIUtil.invokeOnEdt(() -> {
        if (isEmpty) {
          if (contentManager.getIndexOfContent(mainContent) >= 0) {
            contentManager.removeContent(mainContent, false);
          }
        }
        else {
          if (contentManager.getIndexOfContent(mainContent) < 0) {
            contentManager.addContent(mainContent, 0);
          }
        }
      }, myProject.getDisposed());
    });

    return mainView;
  }

  private void loadViews(ContentManager contentManager,
                         ServiceView mainView,
                         Collection<ServiceViewContributor> contributors,
                         List<ServiceViewState> viewStates) {
    myModel.getInvoker().invokeLater(() -> {
      Map<String, ServiceViewContributor> contributorsMap = FactoryMap.create(className -> {
        for (ServiceViewContributor<?> contributor : contributors) {
          if (className.equals(contributor.getClass().getName())) {
            return contributor;
          }
        }
        return null;
      });
      List<ServiceViewFilter> filters = new ArrayList<>();

      List<Pair<ServiceViewModel, ServiceViewState>> loadedModels = new ArrayList<>();
      ServiceViewModel toSelect = null;

      for (ServiceViewState viewState : viewStates) {
        ServiceViewFilter parentFilter = mainView.getModel().getFilter();
        if (viewState.parentView >= 0 && viewState.parentView < filters.size()) {
          parentFilter = filters.get(viewState.parentView);
        }
        ServiceViewFilter filter = parentFilter;
        ServiceViewModel viewModel = ServiceViewModel.loadModel(viewState, myModel, myModelFilter, parentFilter, contributorsMap);
        if (viewModel != null) {
          loadedModels.add(Pair.create(viewModel, viewState));
          if (viewState.isSelected) {
            toSelect = viewModel;
          }
          filter = viewModel.getFilter();
        }
        filters.add(filter);
      }

      if (!loadedModels.isEmpty()) {
        ServiceViewModel modelToSelect = toSelect;
        AppUIUtil.invokeOnEdt(() -> {
          for (Pair<ServiceViewModel, ServiceViewState> pair : loadedModels) {
            extract(pair.first, pair.second, false);
          }
          selectContentByModel(contentManager, modelToSelect);
        }, myProject.getDisposed());
      }
    });
  }

  private static void selectContentByModel(@NotNull ContentManager contentManager, @Nullable ServiceViewModel modelToSelect) {
    if (modelToSelect != null) {
      for (Content content : contentManager.getContents()) {
        ServiceView serviceView = getServiceView(content);
        if (serviceView != null && serviceView.getModel() == modelToSelect) {
          contentManager.setSelectedContent(content);
          break;
        }
      }
    }
    else {
      Content content = getMainContent(contentManager);
      if (content != null) {
        contentManager.setSelectedContent(content);
      }
    }
  }

  private void processAllModels(Consumer<ServiceViewModel> consumer) {
    AppUIUtil.invokeOnEdt(() -> {
      List<ServiceViewModel> models = ContainerUtil.map(getServiceViews(), ServiceView::getModel);
      myModel.getInvoker().invokeLater(() -> {
        for (ServiceViewModel viewModel : models) {
          consumer.accept(viewModel);
        }
      });
    }, myProject.getDisposed());
  }

  private void filtersChanged() {
    processAllModels(ServiceViewModel::filtersChanged);
  }

  private static void registerActivateByContributorActions() {
    for (ServiceViewContributor contributor : ServiceModel.getContributors()) {
      ActionManager actionManager = ActionManager.getInstance();
      String actionId = getActivateContributorActionId(contributor);
      if (actionId == null) continue;

      AnAction action = actionManager.getAction(actionId);
      if (action == null) {
        action = new ActivateToolWindowByContributorAction(contributor);
        actionManager.registerAction(actionId, action);
      }
    }
  }

  private ContentManager getContentManager() {
    return myContentManager;
  }

  private List<ServiceView> getServiceViews() {
    ServiceView allServicesView = myAllServicesView;
    if (allServicesView == null) return Collections.emptyList();

    List<ServiceView> views = ContainerUtil.mapNotNull(getContentManager().getContents(), ServiceViewManagerImpl::getServiceView);
    if (!views.contains(allServicesView)) {
      views.add(0, allServicesView);
    }
    return views;
  }

  @NotNull
  @Override
  public Promise<Void> select(@NotNull Object service, @NotNull Class<?> contributorClass, boolean activate, boolean focus) {
    AsyncPromise<Void> result = new AsyncPromise<>();
    // Ensure model is updated, then iterate over service views on EDT in order to find view with service and select it.
    myModel.getInvoker().runOrInvokeLater(() -> AppUIUtil.invokeLaterIfProjectAlive(myProject, () -> {
      Runnable runnable = () -> {
        ContentManager contentManager = getContentManager();
        List<Content> contents =
          contentManager == null ? Collections.emptyList() : ContainerUtil.newSmartList(contentManager.getContents());
        if (contents.isEmpty()) {
          result.setError("Content not initialized");
          return;
        }

        Collections.reverse(contents);
        select(myProject, contents.iterator(), result, service, contributorClass, focus);
      };
      ToolWindow window = activate ? ToolWindowManager.getInstance(myProject).getToolWindow(getToolWindowId()) : null;
      if (window != null) {
        window.activate(runnable, focus, focus);
      }
      else {
        runnable.run();
      }
    }));
    return result;
  }

  private static void select(Project project, Iterator<Content> iterator, AsyncPromise<Void> result,
                             @NotNull Object service, @NotNull Class<?> contributorClass, boolean focus) {
    Content content = iterator.next();
    ServiceView serviceView = getServiceView(content);
    if (serviceView == null) {
      if (iterator.hasNext()) {
        select(project, iterator, result, service, contributorClass, focus);
      }
      else {
        result.setError("Not services content");
      }
      return;
    }
    serviceView.select(service, contributorClass)
      .onSuccess(v -> {
        AppUIUtil.invokeOnEdt(() -> {
          ContentManager contentManager = content.getManager();
          if (contentManager == null) return;

          if (contentManager.getSelectedContent() != content && contentManager.getIndexOfContent(content) >= 0) {
            contentManager.setSelectedContent(content, focus);
          }
        }, project.getDisposed());

        result.setResult(null);
      })
      .onError(e -> {
        if (iterator.hasNext()) {
          select(project, iterator, result, service, contributorClass, focus);
        }
        else {
          result.setError(e);
        }
      });
  }

  private void updateToolWindow(boolean available, boolean show) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    if (toolWindowManager == null) return;

    toolWindowManager.invokeLater(() -> {
      if (myProject.isDisposed()) return;

      if (available && !myActivationActionsRegistered) {
        myActivationActionsRegistered = true;
        registerActivateByContributorActions();
      }

      boolean doShow = available && (show || getContentManager() != null);
      ToolWindow toolWindow = toolWindowManager.getToolWindow(getToolWindowId());
      if (toolWindow == null) {
        toolWindow = createToolWindow(toolWindowManager, available);
        if (doShow) {
          toolWindow.show(null);
        }
        return;
      }

      doShow = !toolWindow.isAvailable() && doShow;
      toolWindow.setAvailable(available, null);
      if (doShow) {
        toolWindow.show(null);
      }
    });
  }

  @NotNull
  private ToolWindowEx createToolWindow(ToolWindowManager toolWindowManager, boolean available) {
    String id = getToolWindowId();
    ToolWindowEx toolWindow = (ToolWindowEx)toolWindowManager.registerToolWindow(id, true, ToolWindowAnchor.BOTTOM, myProject, true);
    toolWindow.setIcon(getToolWindowIcon());
    toolWindow.setAvailable(available, null);
    createToolWindowContent(toolWindow);
    return toolWindow;
  }

  void extract(@NotNull ServiceViewDragBean dragBean) {
    List<ServiceViewItem> items = dragBean.getItems();
    if (items.isEmpty()) return;

    ServiceViewFilter parentFilter = dragBean.getServiceView().getModel().getFilter();
    ServiceViewModel viewModel = ServiceViewModel.createModel(items, dragBean.getContributor(), myModel, myModelFilter, parentFilter);
    extract(viewModel, new ServiceViewState(), true);
  }

  private void extract(ServiceViewModel viewModel, ServiceViewState viewState, boolean select) {
    ServiceView serviceView = ServiceView.createView(myProject, viewModel, prepareViewState(viewState));
    if (viewModel instanceof ContributorModel) {
      extractContributor((ContributorModel)viewModel, serviceView, select);
    }
    else if (viewModel instanceof GroupModel) {
      extractGroup((GroupModel)viewModel, serviceView, select);
    }
    else if (viewModel instanceof SingeServiceModel) {
      extractService((SingeServiceModel)viewModel, serviceView, select);
    }
    else if (viewModel instanceof ServiceListModel) {
      extractList((ServiceListModel)viewModel, serviceView, viewState.id, select, -1);
    }
  }

  private void extractContributor(ContributorModel viewModel, ServiceView serviceView, boolean select) {
    addServiceContent(serviceView, viewModel.getContributor().getViewDescriptor().getContentPresentation(), select);
  }

  private void extractGroup(GroupModel viewModel, ServiceView serviceView, boolean select) {
    Content content = addServiceContent(serviceView, viewModel.getGroup().getViewDescriptor().getContentPresentation(), select);
    viewModel.addModelListener(() -> updateContentTab(viewModel.getGroup(), content));
  }

  private void extractService(SingeServiceModel viewModel, ServiceView serviceView, boolean select) {
    Content content = addServiceContent(serviceView, viewModel.getService().getViewDescriptor().getContentPresentation(), select);
    ContentManager contentManager = content.getManager();
    viewModel.addModelListener(() -> {
      ServiceViewItem item = viewModel.getService();
      if (item != null && !viewModel.getChildren(item).isEmpty() && contentManager != null) {
        AppUIUtil.invokeOnEdt(() -> {
          int index = contentManager.getIndexOfContent(content);
          if (index < 0) return;

          contentManager.removeContent(content, true);
          ServiceListModel listModel = new ServiceListModel(myModel, myModelFilter, ContainerUtil.newSmartList(item),
                                                            viewModel.getFilter().getParent());
          ServiceView listView = ServiceView.createView(myProject, listModel, prepareViewState(new ServiceViewState()));
          extractList(listModel, listView, null, true, index);
        }, myProject.getDisposed());
      }
      else {
        updateContentTab(item, content);
      }
    });
  }

  private void extractList(ServiceListModel viewModel, ServiceView serviceView, String name, boolean select, int index) {
    List<ServiceViewItem> items = viewModel.getItems();
    ItemPresentation presentation;
    if (items.size() == 1) {
      presentation = items.get(0).getViewDescriptor().getContentPresentation();
    }
    else {
      if (StringUtil.isEmpty(name)) {
        name = Messages.showInputDialog(myProject, "Group Name:", "Group Services", null, null, null);
        if (StringUtil.isEmpty(name)) return;
      }
      presentation = new PresentationData(name, null, AllIcons.Nodes.Folder, null);
    }

    Content content = addServiceContent(serviceView, presentation, select, index);
    viewModel.addModelListener(() -> updateContentTab(ContainerUtil.getOnlyItem(viewModel.getRoots()), content));
  }

  private Content addServiceContent(ServiceView serviceView, ItemPresentation presentation, boolean select) {
    return addServiceContent(serviceView, presentation, select, -1);
  }

  private Content addServiceContent(ServiceView serviceView, ItemPresentation presentation, boolean select, int index) {
    Content content =
      ContentFactory.SERVICE.getInstance().createContent(serviceView, ServiceViewDragHelper.getDisplayName(presentation), false);
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    content.setHelpId(getToolWindowContextHelpId());
    content.setCloseable(true);
    content.setIcon(presentation.getIcon(false));

    Disposer.register(content, serviceView);
    Disposer.register(content, serviceView.getModel());

    addContent(content, select, index);
    return content;
  }

  private void addContent(Content content, boolean select, int index) {
    setScrollToSourceHandler(content);
    ContentManager contentManager = getContentManager();
    contentManager.addContent(content, index);
    if (select) {
      contentManager.setSelectedContent(content);
    }
  }

  private void setScrollToSourceHandler(Content content) {
    ServiceView serviceView = getServiceView(content);
    AutoScrollToSourceHandler toSourceHandler = myAutoScrollToSourceHandler;
    if (serviceView != null && toSourceHandler != null) {
      serviceView.setAutoScrollToSourceHandler(toSourceHandler);
    }
  }

  @Nullable
  private static ServiceView getServiceView(Content content) {
    return ObjectUtils.tryCast(content.getComponent(), ServiceView.class);
  }

  private void updateContentTab(ServiceViewItem item, Content content) {
    if (item != null) {
      AppUIUtil.invokeOnEdt(() -> {
        ItemPresentation itemPresentation = item.getViewDescriptor().getContentPresentation();
        content.setDisplayName(ServiceViewDragHelper.getDisplayName(itemPresentation));
        content.setIcon(itemPresentation.getIcon(false));
      }, myProject.getDisposed());
    }
  }

  @NotNull
  @Override
  public State getState() {
    ContentManager contentManager = getContentManager();
    if (contentManager == null) {
      return myState;
    }

    ServiceViewFilter allServicesFilter = null;
    if (myAllServicesView != null) {
      allServicesFilter = myAllServicesView.getModel().getFilter();
      myAllServicesView.saveState(myState.allServicesViewState);
      myState.allServicesViewState.treeStateElement = new Element("root");
      myState.allServicesViewState.treeState.writeExternal(myState.allServicesViewState.treeStateElement);
    }
    myState.viewStates.clear();
    List<ServiceView> processedViews = ContainerUtil.newSmartList();
    for (Content content : contentManager.getContents()) {
      ServiceView serviceView = getServiceView(content);
      if (serviceView == null || isMainView(serviceView)) continue;

      ServiceViewState viewState = new ServiceViewState();
      processedViews.add(serviceView);
      myState.viewStates.add(viewState);
      serviceView.saveState(viewState);
      viewState.isSelected = contentManager.isSelected(content);
      ServiceViewModel viewModel = serviceView.getModel();
      if (viewModel instanceof ServiceListModel) {
        viewState.id = content.getDisplayName();
      }
      ServiceViewFilter parentFilter = viewModel.getFilter().getParent();
      if (parentFilter != null && !parentFilter.equals(allServicesFilter)) {
        for (int i = 0; i < processedViews.size(); i++) {
          ServiceView parentView = processedViews.get(i);
          if (parentView.getModel().getFilter().equals(parentFilter)) {
            viewState.parentView = i;
            break;
          }
        }
      }

      viewState.treeStateElement = new Element("root");
      viewState.treeState.writeExternal(viewState.treeStateElement);
    }
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState = state;
    for (ServiceViewState viewState : myState.viewStates) {
      viewState.treeState = TreeState.createFrom(viewState.treeStateElement);
    }
  }

  static class State {
    public ServiceViewState allServicesViewState = new ServiceViewState();
    public List<ServiceViewState> viewStates = new ArrayList<>();
    public boolean showServicesTree = true;
  }

  private static String getToolWindowId() {
    return ToolWindowId.SERVICES;
  }

  @NotNull
  private static Icon getToolWindowIcon() {
    return AllIcons.Toolwindows.ToolWindowServices;
  }

  static String getToolWindowContextHelpId() {
    return HELP_ID;
  }

  private ServiceViewState prepareViewState(ServiceViewState state) {
    state.showServicesTree = myState.showServicesTree;
    return state;
  }

  boolean isShowServicesTree() {
    return myState.showServicesTree;
  }

  void setShowServicesTree(boolean value) {
    myState.showServicesTree = value;
    for (ServiceView serviceView : getServiceViews()) {
      serviceView.getUi().setMasterComponentVisible(value);
    }
  }

  boolean isSplitByTypeEnabled(@SuppressWarnings("unused") @NotNull ServiceView selectedView) {
    for (Content content : getContentManager().getContents()) {
      ServiceView serviceView = getServiceView(content);
      if (serviceView != null && !(serviceView.getModel() instanceof ContributorModel)) return false;
    }
    return true;
  }

  void splitByType(@SuppressWarnings("unused") @NotNull ServiceView selectedView) {
    myModel.getInvoker().invokeLater(() -> {
      List<ServiceViewContributor> contributors = ContainerUtil.map(myModel.getRoots(), ServiceViewItem::getContributor);
      AppUIUtil.invokeOnEdt(() -> {
        for (ServiceViewContributor contributor : contributors) {
          splitByType(contributor);
        }
      });
    });
  }

  private void splitByType(ServiceViewContributor contributor) {
    for (Content content : getContentManager().getContents()) {
      ServiceView serviceView = getServiceView(content);
      if (serviceView != null) {
        ServiceViewModel viewModel = serviceView.getModel();
        if (viewModel instanceof ContributorModel && contributor.equals(((ContributorModel)viewModel).getContributor())) {
          return;
        }
      }
    }

    ContributorModel contributorModel = new ContributorModel(myModel, myModelFilter, contributor, null);
    ServiceView contributorView = ServiceView.createView(myProject, contributorModel, prepareViewState(new ServiceViewState()));
    extractContributor(contributorModel, contributorView, true);
  }

  public List<Object> getChildrenSafe(@NotNull AnActionEvent e, @NotNull Object value) {
    ServiceView serviceView = ServiceViewActionProvider.getSelectedView(e);
    return serviceView != null ? serviceView.getChildrenSafe(value) : Collections.emptyList();
  }

  static boolean isMainView(@NotNull ServiceView serviceView) {
    return serviceView.getModel() instanceof AllServicesModel;
  }

  @Nullable
  private static Content getMainContent(@NotNull ContentManager contentManager) {
    for (Content content : contentManager.getContents()) {
      ServiceView serviceView = getServiceView(content);
      if (serviceView != null && isMainView(serviceView)) {
        return content;
      }
    }
    return null;
  }

  private class MyContentMangerListener extends ContentManagerAdapter {
    private final ContentManager myContentManager;

    MyContentMangerListener(@NotNull ContentManager contentManager) {
      myContentManager = contentManager;
    }

    @Override
    public void contentAdded(@NotNull ContentManagerEvent event) {
      Content content = event.getContent();
      ServiceView serviceView = getServiceView(content);
      if (serviceView != null && !isMainView(serviceView)) {
        myModelFilter.addFilter(serviceView.getModel().getFilter());
        filtersChanged();

        serviceView.getModel().addModelListener(() -> {
          if (serviceView.getModel().getRoots().isEmpty()) {
            AppUIUtil.invokeOnEdt(() -> myContentManager.removeContent(content, true), myProject.getDisposed());
          }
        });
      }

      if (myContentManager.getContentCount() > 1) {
        Content mainContent = getMainContent(myContentManager);
        if (mainContent != null) {
          mainContent.setDisplayName("All Services");
        }
      }
    }

    @Override
    public void contentRemoved(@NotNull ContentManagerEvent event) {
      ServiceView serviceView = getServiceView(event.getContent());
      if (serviceView != null && !isMainView(serviceView)) {
        myModelFilter.removeFilter(serviceView.getModel().getFilter());
        filtersChanged();
      }
      Content[] contents = myContentManager.getContents();
      if (contents.length == 1) {
        contents[0].setDisplayName(null);
      }
    }

    @Override
    public void selectionChanged(@NotNull ContentManagerEvent event) {
      ServiceView serviceView = getServiceView(event.getContent());
      if (serviceView == null) return;

      if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
        serviceView.onViewSelected();
      }
      else {
        serviceView.onViewUnselected();
      }
    }
  }

  private static String getActivateContributorActionId(ServiceViewContributor contributor) {
    String id = contributor.getViewDescriptor().getId();
    return id == null ? null : "ServiceView.Activate" + id.replaceAll(" ", "");
  }

  private static class ActivateToolWindowByContributorAction extends DumbAwareAction {
    private final ServiceViewContributor myContributor;

    private ActivateToolWindowByContributorAction(ServiceViewContributor contributor) {
      myContributor = contributor;
      ItemPresentation presentation = contributor.getViewDescriptor().getPresentation();
      Presentation templatePresentation = getTemplatePresentation();
      templatePresentation.setText(ServiceViewDragHelper.getDisplayName(presentation) + " (Services)");
      templatePresentation.setIcon(presentation.getIcon(false));
      templatePresentation.setDescription("Activate " + getToolWindowId() + " window");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project == null) {
        e.getPresentation().setEnabledAndVisible(false);
        return;
      }

      ServiceViewManagerImpl manager = (ServiceViewManagerImpl)ServiceViewManager.getInstance(project);
      for (ServiceViewItem root : manager.myModel.getRoots()) {
        if (myContributor.equals(root.getContributor())) {
          e.getPresentation().setEnabledAndVisible(true);
          return;
        }
      }
      e.getPresentation().setEnabledAndVisible(false);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project == null) return;

      ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
      ToolWindow window = windowManager.getToolWindow(getToolWindowId());

      if (window.isActive()) {
        selectContributorView(project);
      }
      else {
        window.activate(() -> selectContributorView(project));
      }
    }

    private void selectContributorView(Project project) {
      ContentManager contentManager = ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).getContentManager();
      Content mainContent = null;
      for (Content content : contentManager.getContents()) {
        ServiceView serviceView = getServiceView(content);
        if (serviceView != null) {
          if (serviceView.getModel() instanceof ContributorModel &&
              myContributor.equals(((ContributorModel)serviceView.getModel()).getContributor())) {
            contentManager.setSelectedContent(content, true);
            return;
          }
          if (isMainView(serviceView)) {
            mainContent = content;
          }
        }
      }
      if (mainContent != null) {
        contentManager.setSelectedContent(mainContent, true);
      }
    }
  }
}
