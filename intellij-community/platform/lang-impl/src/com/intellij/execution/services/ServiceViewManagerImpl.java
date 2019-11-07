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
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.ui.content.*;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
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
  private final Map<String, Collection<ServiceViewContributor<?>>> myGroups;
  private final List<ServiceViewContentHolder> myContentHolders = new SmartList<>();
  private boolean myActivationActionsRegistered;
  private AutoScrollToSourceHandler myAutoScrollToSourceHandler;

  public ServiceViewManagerImpl(@NotNull Project project) {
    myProject = project;
    myModel = new ServiceModel(myProject);
    Disposer.register(myProject, myModel);
    myModelFilter = new ServiceModelFilter();
    myGroups = loadGroups();
    myProject.getMessageBus().connect(myModel).subscribe(ServiceEventListener.TOPIC, e -> myModel.handle(e).onSuccess(o -> {
      ServiceViewItem eventRoot = ContainerUtil.find(myModel.getRoots(), root -> e.contributorClass.isInstance(root.getRootContributor()));
      if (eventRoot != null) {
        activateToolWindow(e.contributorClass,
                           !(eventRoot.getViewDescriptor() instanceof ServiceViewNonActivatingDescriptor), false);
      }
      AppUIUtil.invokeOnEdt(() -> {
        ServiceViewContentHolder holder = getContentHolder(e.contributorClass);
        if (holder != null) {
          holder.processAllModels(viewModel -> viewModel.eventProcessed(e));
        }
      }, myProject.getDisposed());
    }));
    myModel.initRoots().onSuccess(o -> {
      for (ServiceViewContributor<?> contributor : ServiceModel.getContributors()) {
        activateToolWindow(contributor.getClass(),
                           !(contributor.getViewDescriptor(myProject) instanceof ServiceViewNonActivatingDescriptor), true);
      }
    });
  }

  @Nullable
  private ServiceViewContentHolder getContentHolder(@NotNull Class<?> contributorClass) {
    for (ServiceViewContentHolder holder : myContentHolders) {
      for (ServiceViewContributor<?> rootContributor : holder.rootContributors) {
        if (contributorClass.isInstance(rootContributor)) {
          return holder;
        }
      }
    }
    return null;
  }

  private void activateToolWindow(@NotNull Class<?> contributorClass, boolean show, boolean onInit) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    if (toolWindowManager == null) return;

    toolWindowManager.invokeLater(() -> {
      if (myProject.isDisposed()) return;

      if (!myActivationActionsRegistered) {
        myActivationActionsRegistered = true;
        Collection<ServiceViewContributor<?>> contributors = myGroups.get(getToolWindowId());
        if (contributors != null) {
          registerActivateByContributorActions(myProject, contributors);
        }
      }

      String toolWindowId = getToolWindowId(contributorClass);
      if (toolWindowId == null) return;

      boolean doShow = !onInit || getContentHolder(contributorClass) != null;
      ToolWindow toolWindow = toolWindowManager.getToolWindow(toolWindowId);
      if (toolWindow == null) {
        toolWindow = createToolWindow(toolWindowId, toolWindowManager);
        if (doShow) {
          toolWindow.show(null);
        }
        return;
      }

      ContentManager contentManager = toolWindow.getContentManager();
      if (contentManager.getContentCount() > 0) {
        doShow = show && (!toolWindow.isAvailable() || !toolWindow.isShowStripeButton()) && doShow;
        toolWindow.setAvailable(true, null);
        Content content = getMainContent(contentManager);
        ServiceView mainView = content == null ? null : getServiceView(content);
        if (mainView != null && mainView.getModel().getRoots().isEmpty() && contentManager.getContentCount() == 1) {
          hideToolWindow(toolWindowId, toolWindow);
        }
        else if (doShow) {
          toolWindow.show(null);
        }
      }
    });
  }

  private void hideToolWindow(String toolWindowId, ToolWindow toolWindow) {
    ToolWindowManagerEx.getInstanceEx(myProject).hideToolWindow(toolWindowId, false);
    toolWindow.setShowStripeButton(false);
  }

  @NotNull
  private ToolWindowEx createToolWindow(String toolWindowId, ToolWindowManager toolWindowManager) {
    ToolWindowEx toolWindow =
      (ToolWindowEx)toolWindowManager.registerToolWindow(toolWindowId, true, ToolWindowAnchor.BOTTOM, myProject, true);
    toolWindow.setIcon(getToolWindowIcon());
    createToolWindowContent(toolWindowId, toolWindow);
    return toolWindow;
  }

  public void createToolWindowContent(@NotNull String toolWindowId, @NotNull ToolWindow toolWindow) {
    Collection<ServiceViewContributor<?>> contributors = myGroups.get(toolWindowId);
    if (contributors == null) return;

    if (myAutoScrollToSourceHandler == null) {
      myAutoScrollToSourceHandler = ServiceViewSourceScrollHelper.createAutoScrollToSourceHandler(myProject);
    }
    ToolWindowEx toolWindowEx = (ToolWindowEx)toolWindow;
    ServiceViewSourceScrollHelper.installAutoScrollSupport(myProject, toolWindowEx, myAutoScrollToSourceHandler);

    Pair<ServiceViewState, List<ServiceViewState>> states = getServiceViewStates(toolWindowId);
    AllServicesModel mainModel = new AllServicesModel(myModel, myModelFilter, contributors);
    ServiceView mainView = ServiceView.createView(myProject, mainModel, prepareViewState(states.first));
    mainView.setAutoScrollToSourceHandler(myAutoScrollToSourceHandler);

    ContentManager contentManager = toolWindow.getContentManager();
    ServiceViewContentHolder holder = new ServiceViewContentHolder(mainView, contentManager, contributors, toolWindowId);
    myContentHolders.add(holder);
    contentManager.addContentManagerListener(new ServiceViewContentMangerListener(myModelFilter, myAutoScrollToSourceHandler, holder));

    addMainContent(toolWindowId, toolWindow, mainView);
    loadViews(contentManager, mainView, contributors, states.second);
    ServiceViewDragHelper.installDnDSupport(myProject, toolWindowEx.getDecorator(), contentManager);
  }

  private void addMainContent(String toolWindowId, ToolWindow toolWindow, ServiceView mainView) {
    Content mainContent = ContentFactory.SERVICE.getInstance().createContent(mainView, null, false);
    mainContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    mainContent.setHelpId(getToolWindowContextHelpId());
    mainContent.setCloseable(false);

    Disposer.register(mainContent, mainView);
    Disposer.register(mainContent, mainView.getModel());

    ContentManager contentManager = toolWindow.getContentManager();
    contentManager.addContent(mainContent);
    mainView.getModel().addModelListener(() -> {
      boolean isEmpty = mainView.getModel().getRoots().isEmpty();
      AppUIUtil.invokeOnEdt(() -> {
        if (isEmpty) {
          if (contentManager.getIndexOfContent(mainContent) < 0) {
            if (contentManager.getContentCount() == 0) {
              contentManager.addContent(mainContent, 0);
              hideToolWindow(toolWindowId, toolWindow);
            }
          }
          else {
            if (contentManager.getContentCount() > 1) {
              contentManager.removeContent(mainContent, false);
            }
            else if (mainView.hasItems()) {
              // Hide tool window only if model roots became empty and there were some services shown in master component before update.
              hideToolWindow(toolWindowId, toolWindow);
            }
          }
        }
        else {
          if (contentManager.getIndexOfContent(mainContent) < 0) {
            contentManager.addContent(mainContent, 0);
          }
        }
      }, myProject.getDisposed());
    });
  }

  private void loadViews(ContentManager contentManager,
                         ServiceView mainView,
                         Collection<ServiceViewContributor<?>> contributors,
                         List<ServiceViewState> viewStates) {
    myModel.getInvoker().invokeLater(() -> {
      Map<String, ServiceViewContributor<?>> contributorsMap = FactoryMap.create(className -> {
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
            extract(contentManager, pair.first, pair.second, false);
          }
          selectContentByModel(contentManager, modelToSelect);
        }, myProject.getDisposed());
      }
    });
  }

  @NotNull
  @Override
  public Promise<Void> select(@NotNull Object service, @NotNull Class<?> contributorClass, boolean activate, boolean focus) {
    AsyncPromise<Void> result = new AsyncPromise<>();
    // Ensure model is updated, then iterate over service views on EDT in order to find view with service and select it.
    myModel.getInvoker().invoke(() -> AppUIUtil.invokeLaterIfProjectAlive(myProject, () -> {
      ServiceViewContentHolder holder = getContentHolder(contributorClass);
      if (holder == null) {
        result.setError("Content manager not initialized");
        return;
      }
      Runnable runnable = () -> {
        List<Content> contents = new SmartList<>(holder.contentManager.getContents());
        if (contents.isEmpty()) {
          result.setError("Content not initialized");
          return;
        }

        Collections.reverse(contents);
        select(myProject, contents.iterator(), result, service, contributorClass, focus);
      };
      ToolWindow toolWindow = activate ? ToolWindowManager.getInstance(myProject).getToolWindow(holder.toolWindowId) : null;
      if (toolWindow != null) {
        toolWindow.activate(runnable, focus, focus);
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

  @NotNull
  Promise<Void> select(@NotNull VirtualFile virtualFile) {
    AsyncPromise<Void> result = new AsyncPromise<>();
    myModel.getInvoker().invoke(() -> {
      ServiceViewItem fileItem = myModel.findItem(
        item -> {
          ServiceViewDescriptor descriptor = item.getViewDescriptor();
          return descriptor instanceof ServiceViewLocatableDescriptor &&
                 virtualFile.equals(((ServiceViewLocatableDescriptor)descriptor).getVirtualFile());
        },
        item -> !(item instanceof ServiceModel.ServiceNode) ||
                item.getViewDescriptor() instanceof ServiceViewLocatableDescriptor
      );
      if (fileItem != null) {
        Promise<Void> promise = select(fileItem.getValue(), fileItem.getRootContributor().getClass(), true, false);
        promise.onSuccess(o -> result.setResult(null)).onError(result::setError);
      }
    });
    return result;
  }

  void extract(@NotNull ServiceViewDragBean dragBean) {
    List<ServiceViewItem> items = dragBean.getItems();
    if (items.isEmpty()) return;

    ServiceView serviceView = dragBean.getServiceView();
    ServiceViewContentHolder holder = getContentHolder(serviceView);
    if (holder == null) return;

    ServiceViewFilter parentFilter = serviceView.getModel().getFilter();
    ServiceViewModel viewModel = ServiceViewModel.createModel(items, dragBean.getContributor(), myModel, myModelFilter, parentFilter);
    ServiceViewState state = new ServiceViewState();
    serviceView.saveState(state);
    extract(holder.contentManager, viewModel, state, true);
  }

  private void extract(ContentManager contentManager, ServiceViewModel viewModel, ServiceViewState viewState, boolean select) {
    ServiceView serviceView = ServiceView.createView(myProject, viewModel, prepareViewState(viewState));
    ItemPresentation presentation = getContentPresentation(myProject, viewModel, viewState);
    if (presentation == null) return;

    Content content = addServiceContent(contentManager, serviceView, presentation, select);
    if (viewModel instanceof GroupModel) {
      extractGroup((GroupModel)viewModel, content);
    }
    else if (viewModel instanceof SingeServiceModel) {
      extractService((SingeServiceModel)viewModel, content);
    }
    else if (viewModel instanceof ServiceListModel) {
      extractList((ServiceListModel)viewModel, content);
    }
  }

  private static void extractGroup(GroupModel viewModel, Content content) {
    viewModel.addModelListener(() -> updateContentTab(viewModel.getGroup(), content));
  }

  private void extractService(SingeServiceModel viewModel, Content content) {
    ContentManager contentManager = content.getManager();
    viewModel.addModelListener(() -> {
      ServiceViewItem item = viewModel.getService();
      if (item != null && !viewModel.getChildren(item).isEmpty() && contentManager != null) {
        AppUIUtil.invokeOnEdt(() -> {
          int index = contentManager.getIndexOfContent(content);
          if (index < 0) return;

          contentManager.removeContent(content, true);
          ServiceListModel listModel = new ServiceListModel(myModel, myModelFilter, new SmartList<>(item),
                                                            viewModel.getFilter().getParent());
          ServiceView listView = ServiceView.createView(myProject, listModel, prepareViewState(new ServiceViewState()));
          Content listContent = addServiceContent(contentManager, listView, item.getViewDescriptor().getContentPresentation(), true, index);
          extractList(listModel, listContent);
        }, myProject.getDisposed());
      }
      else {
        updateContentTab(item, content);
      }
    });
  }

  private static void extractList(ServiceListModel viewModel, Content content) {
    viewModel.addModelListener(() -> updateContentTab(ContainerUtil.getOnlyItem(viewModel.getRoots()), content));
  }

  private static ItemPresentation getContentPresentation(Project project, ServiceViewModel viewModel, ServiceViewState viewState) {
    if (viewModel instanceof ContributorModel) {
      return ((ContributorModel)viewModel).getContributor().getViewDescriptor(project).getContentPresentation();
    }
    else if (viewModel instanceof GroupModel) {
      return ((GroupModel)viewModel).getGroup().getViewDescriptor().getContentPresentation();
    }
    else if (viewModel instanceof SingeServiceModel) {
      return ((SingeServiceModel)viewModel).getService().getViewDescriptor().getContentPresentation();
    }
    else if (viewModel instanceof ServiceListModel) {
      List<ServiceViewItem> items = ((ServiceListModel)viewModel).getItems();
      if (items.size() == 1) {
        return items.get(0).getViewDescriptor().getContentPresentation();
      }
      String name = viewState.id;
      if (StringUtil.isEmpty(name)) {
        name = Messages.showInputDialog(project, "Group Name:", "Group Services", null, null, null);
        if (StringUtil.isEmpty(name)) return null;
      }
      return new PresentationData(name, null, AllIcons.Nodes.Folder, null);
    }
    return null;
  }

  private static Content addServiceContent(ContentManager contentManager, ServiceView serviceView, ItemPresentation presentation,
                                           boolean select) {
    return addServiceContent(contentManager, serviceView, presentation, select, -1);
  }

  private static Content addServiceContent(ContentManager contentManager, ServiceView serviceView, ItemPresentation presentation,
                                           boolean select, int index) {
    Content content =
      ContentFactory.SERVICE.getInstance().createContent(serviceView, ServiceViewDragHelper.getDisplayName(presentation), false);
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    content.setHelpId(getToolWindowContextHelpId());
    content.setCloseable(true);
    content.setIcon(presentation.getIcon(false));

    Disposer.register(content, serviceView);
    Disposer.register(content, serviceView.getModel());

    contentManager.addContent(content, index);
    if (select) {
      contentManager.setSelectedContent(content);
    }
    return content;
  }

  private static void updateContentTab(ServiceViewItem item, Content content) {
    if (item != null) {
      Condition<?> expired = o -> {
        ContentManager contentManager = content.getManager();
        return contentManager == null || contentManager.isDisposed();
      };
      AppUIUtil.invokeOnEdt(() -> {
        ItemPresentation itemPresentation = item.getViewDescriptor().getContentPresentation();
        content.setDisplayName(ServiceViewDragHelper.getDisplayName(itemPresentation));
        content.setIcon(itemPresentation.getIcon(false));
      }, expired);
    }
  }

  private static Map<String, Collection<ServiceViewContributor<?>>> loadGroups() {
    Map<String, Collection<ServiceViewContributor<?>>> result = new HashMap<>();
    Set<ServiceViewContributor<?>> contributors = ContainerUtil.newHashSet(ServiceModel.getContributors());
    if (Registry.is("ide.service.view.split")) {
      for (ServiceViewContributor<?> contributor : contributors) {
        result.put(contributor.getClass().getName(), new SmartList<>(contributor));
      }
    }
    else {
      if (!contributors.isEmpty()) {
        result.put(getToolWindowId(), contributors);
      }
    }
    return result;
  }

  private Pair<ServiceViewState, List<ServiceViewState>> getServiceViewStates(String groupId) {
    List<ServiceViewState> states = ContainerUtil.filter(myState.viewStates, state -> groupId.equals(state.groupId));
    ServiceViewState mainState = ContainerUtil.find(states, state -> StringUtil.isEmpty(state.viewType));
    if (mainState == null) {
      mainState = new ServiceViewState();
    }
    else {
      states.remove(mainState);
    }
    return Pair.create(mainState, states);
  }

  @NotNull
  @Override
  public State getState() {
    ContainerUtil.retainAll(myState.viewStates, state -> myGroups.containsKey(state.groupId));
    for (ServiceViewContentHolder holder : myContentHolders) {
      ContainerUtil.retainAll(myState.viewStates, state -> !holder.toolWindowId.equals(state.groupId));

      ServiceViewFilter mainFilter = holder.mainView.getModel().getFilter();
      ServiceViewState mainState = new ServiceViewState();
      myState.viewStates.add(mainState);
      holder.mainView.saveState(mainState);
      mainState.groupId = holder.toolWindowId;
      mainState.treeStateElement = new Element("root");
      mainState.treeState.writeExternal(mainState.treeStateElement);

      List<ServiceView> processedViews = new SmartList<>();
      for (Content content : holder.contentManager.getContents()) {
        ServiceView serviceView = getServiceView(content);
        if (serviceView == null || isMainView(serviceView)) continue;

        ServiceViewState viewState = new ServiceViewState();
        processedViews.add(serviceView);
        myState.viewStates.add(viewState);
        serviceView.saveState(viewState);
        viewState.groupId = holder.toolWindowId;
        viewState.isSelected = holder.contentManager.isSelected(content);
        ServiceViewModel viewModel = serviceView.getModel();
        if (viewModel instanceof ServiceListModel) {
          viewState.id = content.getDisplayName();
        }
        ServiceViewFilter parentFilter = viewModel.getFilter().getParent();
        if (parentFilter != null && !parentFilter.equals(mainFilter)) {
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
    for (ServiceViewContentHolder holder : myContentHolders) {
      for (ServiceView serviceView : holder.getServiceViews()) {
        serviceView.getUi().setMasterComponentVisible(value);
      }
    }
  }

  boolean isSplitByTypeEnabled(@NotNull ServiceView selectedView) {
    ServiceViewContentHolder holder = getContentHolder(selectedView);
    if (holder == null) return false;

    for (Content content : holder.contentManager.getContents()) {
      ServiceView serviceView = getServiceView(content);
      if (serviceView != null && serviceView != selectedView && !(serviceView.getModel() instanceof ContributorModel)) return false;
    }
    return true;
  }

  void splitByType(@NotNull ServiceView selectedView) {
    ServiceViewContentHolder holder = getContentHolder(selectedView);
    if (holder == null) return;

    myModel.getInvoker().invokeLater(() -> {
      List<ServiceViewContributor<?>> contributors = ContainerUtil.map(myModel.getRoots(), ServiceViewItem::getRootContributor);
      AppUIUtil.invokeOnEdt(() -> {
        for (ServiceViewContributor<?> contributor : contributors) {
          splitByType(holder.contentManager, contributor);
        }
      });
    });
  }

  private ServiceViewContentHolder getContentHolder(ServiceView serviceView) {
    for (ServiceViewContentHolder holder : myContentHolders) {
      if (holder.getServiceViews().contains(serviceView)) {
        return holder;
      }
    }
    return null;
  }

  private void splitByType(ContentManager contentManager, ServiceViewContributor<?> contributor) {
    for (Content content : contentManager.getContents()) {
      ServiceView serviceView = getServiceView(content);
      if (serviceView != null) {
        ServiceViewModel viewModel = serviceView.getModel();
        if (viewModel instanceof ContributorModel && contributor.equals(((ContributorModel)viewModel).getContributor())) {
          return;
        }
      }
    }

    ContributorModel contributorModel = new ContributorModel(myModel, myModelFilter, contributor, null);
    extract(contentManager, contributorModel, prepareViewState(new ServiceViewState()), true);
  }

  public List<Object> getChildrenSafe(@NotNull AnActionEvent e, @NotNull List<Object> valueSubPath) {
    ServiceView serviceView = ServiceViewActionProvider.getSelectedView(e);
    return serviceView != null ? serviceView.getChildrenSafe(valueSubPath) : Collections.emptyList();
  }

  public String getToolWindowId(@NotNull Class<?> contributorClass) {
    for (Map.Entry<String, Collection<ServiceViewContributor<?>>> entry : myGroups.entrySet()) {
      if (entry.getValue().stream().anyMatch(contributorClass::isInstance)) {
        return entry.getKey();
      }
    }
    return null;
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

  @Nullable
  private static ServiceView getServiceView(Content content) {
    return ObjectUtils.tryCast(content.getComponent(), ServiceView.class);
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

  private static void selectContentByContributor(@NotNull ContentManager contentManager, @NotNull ServiceViewContributor<?> contributor) {
    Content mainContent = null;
    for (Content content : contentManager.getContents()) {
      ServiceView serviceView = getServiceView(content);
      if (serviceView != null) {
        if (serviceView.getModel() instanceof ContributorModel &&
            contributor.equals(((ContributorModel)serviceView.getModel()).getContributor())) {
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

  private static class ServiceViewContentMangerListener extends ContentManagerAdapter {
    private final ServiceModelFilter myModelFilter;
    private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;
    private final ServiceViewContentHolder myContentHolder;
    private final ContentManager myContentManager;

    ServiceViewContentMangerListener(@NotNull ServiceModelFilter modelFilter,
                                     @NotNull AutoScrollToSourceHandler toSourceHandler,
                                     @NotNull ServiceViewContentHolder contentHolder) {
      myModelFilter = modelFilter;
      myAutoScrollToSourceHandler = toSourceHandler;
      myContentHolder = contentHolder;
      myContentManager = contentHolder.contentManager;
    }

    @Override
    public void contentAdded(@NotNull ContentManagerEvent event) {
      Content content = event.getContent();
      ServiceView serviceView = getServiceView(content);
      if (serviceView != null && !isMainView(serviceView)) {
        serviceView.setAutoScrollToSourceHandler(myAutoScrollToSourceHandler);
        myModelFilter.addFilter(serviceView.getModel().getFilter());
        myContentHolder.processAllModels(ServiceViewModel::filtersChanged);

        serviceView.getModel().addModelListener(() -> {
          if (serviceView.getModel().getRoots().isEmpty()) {
            AppUIUtil.invokeOnEdt(() -> myContentManager.removeContent(content, true), o -> myContentManager.isDisposed());
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
        myContentHolder.processAllModels(ServiceViewModel::filtersChanged);
      }
      if (myContentManager.getContentCount() == 1) {
        Content mainContent = getMainContent(myContentManager);
        if (mainContent != null) {
          mainContent.setDisplayName(null);
        }
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

  private static void registerActivateByContributorActions(Project project, Collection<ServiceViewContributor<?>> contributors) {
    for (ServiceViewContributor<?> contributor : contributors) {
      ActionManager actionManager = ActionManager.getInstance();
      String actionId = getActivateContributorActionId(contributor);
      if (actionId == null) continue;

      AnAction action = actionManager.getAction(actionId);
      if (action == null) {
        action = new ActivateToolWindowByContributorAction(contributor, contributor.getViewDescriptor(project).getPresentation());
        actionManager.registerAction(actionId, action);
      }
    }
  }

  private static String getActivateContributorActionId(ServiceViewContributor<?> contributor) {
    String name = contributor.getClass().getSimpleName();
    return name.isEmpty() ? null : "ServiceView.Activate" + name;
  }

  private static class ActivateToolWindowByContributorAction extends DumbAwareAction {
    private final ServiceViewContributor<?> myContributor;

    ActivateToolWindowByContributorAction(ServiceViewContributor<?> contributor, ItemPresentation contributorPresentation) {
      myContributor = contributor;
      Presentation templatePresentation = getTemplatePresentation();
      templatePresentation.setText(ServiceViewDragHelper.getDisplayName(contributorPresentation) + " (Services)");
      templatePresentation.setIcon(contributorPresentation.getIcon(false));
      templatePresentation.setDescription("Activate " + getToolWindowId() + " window");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project == null) return;

      ServiceViewContentHolder holder =
        ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).getContentHolder(myContributor.getClass());
      if (holder == null) return;

      Runnable runnable = () -> selectContentByContributor(holder.contentManager, myContributor);
      ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(holder.toolWindowId);
      if (toolWindow != null) {
        toolWindow.activate(runnable);
      }
      else {
        runnable.run();
      }
    }
  }

  private static class ServiceViewContentHolder {
    final ServiceView mainView;
    final ContentManager contentManager;
    final Collection<ServiceViewContributor<?>> rootContributors;
    final String toolWindowId;

    ServiceViewContentHolder(ServiceView mainView,
                             ContentManager contentManager,
                             Collection<ServiceViewContributor<?>> rootContributors,
                             String toolWindowId) {
      this.mainView = mainView;
      this.contentManager = contentManager;
      this.rootContributors = rootContributors;
      this.toolWindowId = toolWindowId;
    }

    List<ServiceView> getServiceViews() {
      List<ServiceView> views = ContainerUtil.mapNotNull(contentManager.getContents(), ServiceViewManagerImpl::getServiceView);
      if (views.isEmpty()) return new SmartList<>(mainView);

      if (!views.contains(mainView)) {
        views.add(0, mainView);
      }
      return views;
    }

    private void processAllModels(Consumer<ServiceViewModel> consumer) {
      List<ServiceViewModel> models = ContainerUtil.map(getServiceViews(), ServiceView::getModel);
      ServiceViewModel model = ContainerUtil.getFirstItem(models);
      if (model != null) {
        model.getInvoker().invokeLater(() -> {
          for (ServiceViewModel viewModel : models) {
            consumer.accept(viewModel);
          }
        });
      }
    }
  }
}
