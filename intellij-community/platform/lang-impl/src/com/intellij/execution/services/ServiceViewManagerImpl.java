// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.services.ServiceEventListener.ServiceEvent;
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
import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.ui.content.*;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.containers.SmartHashSet;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

@State(name = "ServiceViewManager", storages = {
  @Storage(value = StoragePathMacros.PRODUCT_WORKSPACE_FILE),
  @Storage(value = StoragePathMacros.WORKSPACE_FILE, deprecated = true)
})
public final class ServiceViewManagerImpl implements ServiceViewManager, PersistentStateComponent<ServiceViewManagerImpl.State> {
  @NonNls private static final String HELP_ID = "services.tool.window";

  private final Project myProject;
  private State myState = new State();

  private final ServiceModel myModel;
  private final ServiceModelFilter myModelFilter;
  private final Map<String, Collection<ServiceViewContributor<?>>> myGroups = new ConcurrentHashMap<>();
  private final List<ServiceViewContentHolder> myContentHolders = new SmartList<>();
  private boolean myActivationActionsRegistered;
  private AutoScrollToSourceHandler myAutoScrollToSourceHandler;

  private final Set<String> myActiveToolWindowIds = new SmartHashSet<>();
  private boolean myRegisteringToolWindowAvailable;

  public ServiceViewManagerImpl(@NotNull Project project) {
    myProject = project;
    myModel = new ServiceModel(myProject);
    Disposer.register(myProject, myModel);
    myModelFilter = new ServiceModelFilter();
    loadGroups(ServiceModel.CONTRIBUTOR_EP_NAME.getExtensionList());
    myProject.getMessageBus().connect(myModel).subscribe(ServiceEventListener.TOPIC,
                                                         e -> myModel.handle(e).onSuccess(o -> eventHandled(e)));
    initRoots();
    ServiceModel.CONTRIBUTOR_EP_NAME.addExtensionPointListener(new ServiceViewExtensionPointListener(), myProject);
  }

  private void eventHandled(@NotNull ServiceEvent e) {
    String toolWindowId = getToolWindowId(e.contributorClass);
    if (toolWindowId == null) {
      return;
    }

    ServiceViewItem eventRoot = ContainerUtil.find(myModel.getRoots(), root -> e.contributorClass.isInstance(root.getRootContributor()));
    if (eventRoot != null) {
      boolean show = !(eventRoot.getViewDescriptor() instanceof ServiceViewNonActivatingDescriptor);
      updateToolWindow(toolWindowId, true, show);
    }
    else {
      Set<? extends ServiceViewContributor<?>> activeContributors = getActiveContributors();
      Collection<ServiceViewContributor<?>> toolWindowContributors = myGroups.get(toolWindowId);
      updateToolWindow(toolWindowId, ContainerUtil.intersects(activeContributors, toolWindowContributors), false);
    }
  }

  private void initRoots() {
    myModel.getInvoker().invokeLater(() -> {
      myModel.initRoots().onSuccess(o -> {
        Set<? extends ServiceViewContributor<?>> activeContributors = getActiveContributors();
        Map<String, Boolean> toolWindowIds = new HashMap<>();
        for (ServiceViewContributor<?> contributor : ServiceModel.CONTRIBUTOR_EP_NAME.getExtensionList()) {
          String toolWindowId = getToolWindowId(contributor.getClass());
          if (toolWindowId != null) {
            Boolean active = toolWindowIds.putIfAbsent(toolWindowId, activeContributors.contains(contributor));
            if (Boolean.FALSE == active && activeContributors.contains(contributor)) {
              toolWindowIds.put(toolWindowId, Boolean.TRUE);
            }
          }
        }
        for (Map.Entry<String, Boolean> entry : toolWindowIds.entrySet()) {
          registerToolWindow(entry.getKey(), entry.getValue());
        }
      });
    });
  }

  private Set<? extends ServiceViewContributor<?>> getActiveContributors() {
    return ContainerUtil.map2Set(myModel.getRoots(), ServiceViewItem::getRootContributor);
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

  private void registerToolWindow(@NotNull String toolWindowId, boolean active) {
    if (myProject.isDefault()) {
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      if (!myActivationActionsRegistered) {
        myActivationActionsRegistered = true;
        Collection<ServiceViewContributor<?>> contributors = myGroups.get(getToolWindowId());
        if (contributors != null) {
          registerActivateByContributorActions(myProject, contributors);
        }
      }

      myRegisteringToolWindowAvailable = active;
      try {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
        ToolWindow toolWindow = toolWindowManager.registerToolWindow(RegisterToolWindowTask.lazyAndClosable(toolWindowId, new ServiceViewToolWindowFactory(), AllIcons.Toolwindows.ToolWindowServices));
        if (active) {
          myActiveToolWindowIds.add(toolWindowId);
        }
        else {
          toolWindow.setShowStripeButton(false);
        }
      }
      finally {
        myRegisteringToolWindowAvailable = false;
      }
    }, ModalityState.NON_MODAL, myProject.getDisposed());
  }

  private void updateToolWindow(@NotNull String toolWindowId, boolean active, boolean show) {
    if (myProject.isDisposed() || myProject.isDefault()) {
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(toolWindowId);
      if (toolWindow == null) {
        return;
      }

      if (active) {
        boolean doShow = show && !myActiveToolWindowIds.contains(toolWindowId) && !toolWindow.isShowStripeButton();
        myActiveToolWindowIds.add(toolWindowId);
        if (doShow) {
          toolWindow.show();
        }
      }
      else if (myActiveToolWindowIds.remove(toolWindowId)) {
        // Hide tool window only if model roots became empty and there were some services shown before update.
        toolWindow.hide();
        toolWindow.setShowStripeButton(false);
      }
    }, ModalityState.NON_MODAL, myProject.getDisposed());
  }

  boolean shouldBeAvailable() {
    return myRegisteringToolWindowAvailable;
  }

  void createToolWindowContent(@NotNull ToolWindow toolWindow) {
    String toolWindowId = toolWindow.getId();
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

    addMainContent(toolWindow.getContentManager(), mainView);
    loadViews(contentManager, mainView, contributors, states.second);
    ServiceViewDragHelper.installDnDSupport(myProject, toolWindowEx.getDecorator(), contentManager);
  }

  private void addMainContent(ContentManager contentManager, ServiceView mainView) {
    Content mainContent = ContentFactory.SERVICE.getInstance().createContent(mainView, null, false);
    mainContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    mainContent.setHelpId(getToolWindowContextHelpId());
    mainContent.setCloseable(false);

    Disposer.register(mainContent, mainView);
    Disposer.register(mainContent, mainView.getModel());

    contentManager.addContent(mainContent);
    mainView.getModel().addModelListener(() -> {
      boolean isEmpty = mainView.getModel().getRoots().isEmpty();
      AppUIExecutor.onUiThread().expireWith(myProject).submit(() -> {
        if (contentManager.isDisposed()) return;

        if (isEmpty) {
          if (contentManager.getIndexOfContent(mainContent) < 0) {
            if (contentManager.getContentCount() == 0) {
              contentManager.addContent(mainContent, 0);
            }
          }
          else if (contentManager.getContentCount() > 1) {
              contentManager.removeContent(mainContent, false);
          }
        }
        else {
          if (contentManager.getIndexOfContent(mainContent) < 0) {
            contentManager.addContent(mainContent, 0);
          }
        }
      });
    });
  }

  private void loadViews(ContentManager contentManager,
                         ServiceView mainView,
                         Collection<? extends ServiceViewContributor<?>> contributors,
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
        AppUIExecutor.onUiThread().expireWith(myProject).submit(() -> {
          for (Pair<ServiceViewModel, ServiceViewState> pair : loadedModels) {
            extract(contentManager, pair.first, pair.second, false);
          }
          selectContentByModel(contentManager, modelToSelect);
        });
      }
    });
  }

  @NotNull
  @Override
  public Promise<Void> select(@NotNull Object service, @NotNull Class<?> contributorClass, boolean activate, boolean focus) {
    AsyncPromise<Void> result = new AsyncPromise<>();
    // Ensure model is updated, then iterate over service views on EDT in order to find view with service and select it.
    myModel.getInvoker().invoke(() -> AppUIUtil.invokeLaterIfProjectAlive(myProject, () -> {
      String toolWindowId = getToolWindowId(contributorClass);
      if (toolWindowId == null) {
        result.setError("Contributor group not found");
        return;
      }
      Runnable runnable = () -> promiseFindView(contributorClass, result,
                                                serviceView -> serviceView.select(service, contributorClass),
                                                content -> selectContent(content, focus, myProject));
      ToolWindow toolWindow = activate ? ToolWindowManager.getInstance(myProject).getToolWindow(toolWindowId) : null;
      if (toolWindow != null) {
        toolWindow.activate(runnable, focus, focus);
      }
      else {
        runnable.run();
      }
    }));
    return result;
  }

  private void promiseFindView(Class<?> contributorClass, AsyncPromise<Void> result,
                               Function<? super ServiceView, ? extends Promise<?>> action, Consumer<? super Content> onSuccess) {
    ServiceViewContentHolder holder = getContentHolder(contributorClass);
    if (holder == null) {
      result.setError("Content manager not initialized");
      return;
    }
    List<Content> contents = new SmartList<>(holder.contentManager.getContents());
    if (contents.isEmpty()) {
      result.setError("Content not initialized");
      return;
    }
    Collections.reverse(contents);

    promiseFindView(contents.iterator(), result, action, onSuccess);
  }

  private static void promiseFindView(Iterator<? extends Content> iterator, AsyncPromise<Void> result,
                                      Function<? super ServiceView, ? extends Promise<?>> action, Consumer<? super Content> onSuccess) {
    Content content = iterator.next();
    ServiceView serviceView = getServiceView(content);
    if (serviceView == null) {
      if (iterator.hasNext()) {
        promiseFindView(iterator, result, action, onSuccess);
      }
      else {
        result.setError("Not services content");
      }
      return;
    }
    action.apply(serviceView)
      .onSuccess(v -> {
        if (onSuccess != null) {
          onSuccess.accept(content);
        }
        result.setResult(null);
      })
      .onError(e -> {
        if (iterator.hasNext()) {
          promiseFindView(iterator, result, action, onSuccess);
        }
        else {
          result.setError(e);
        }
      });
  }

  private static void selectContent(Content content, boolean focus, Project project) {
    AppUIExecutor.onUiThread().expireWith(project).submit(() -> {
      ContentManager contentManager = content.getManager();
      if (contentManager == null) return;

      if (contentManager.getSelectedContent() != content && contentManager.getIndexOfContent(content) >= 0) {
        contentManager.setSelectedContent(content, focus);
      }
    });
  }

  @NotNull
  @Override
  public Promise<Void> expand(@NotNull Object service, @NotNull Class<?> contributorClass) {
    AsyncPromise<Void> result = new AsyncPromise<>();
    // Ensure model is updated, then iterate over service views on EDT in order to find view with service and select it.
    myModel.getInvoker().invoke(() -> AppUIUtil.invokeLaterIfProjectAlive(myProject, () ->
      promiseFindView(contributorClass, result,
                    serviceView -> serviceView.expand(service, contributorClass),
                      null)));
    return result;
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
        Promise<Void> promise = select(fileItem.getValue(), fileItem.getRootContributor().getClass(), false, false);
        promise.processed(result);
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
        AppUIExecutor.onUiThread().expireWith(myProject).submit(() -> {
          ServiceViewItem viewItem = viewModel.getService();
          if (viewItem == null) return;

          int index = contentManager.getIndexOfContent(content);
          if (index < 0) return;

          contentManager.removeContent(content, true);
          ServiceListModel listModel = new ServiceListModel(myModel, myModelFilter, new SmartList<>(viewItem),
                                                            viewModel.getFilter().getParent());
          ServiceView listView = ServiceView.createView(myProject, listModel, prepareViewState(new ServiceViewState()));
          Content listContent =
            addServiceContent(contentManager, listView, viewItem.getViewDescriptor().getContentPresentation(), true, index);
          extractList(listModel, listContent);
        });
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
        name = Messages.showInputDialog(project,
                                        ExecutionBundle.message("service.view.group.label"),
                                        ExecutionBundle.message("service.view.group.title"),
                                        null, null, null);
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
      WeakReference<ServiceViewItem> itemRef = new WeakReference<>(item);
      AppUIExecutor.onUiThread().expireWith(content).submit(() -> {
        ServiceViewItem viewItem = itemRef.get();
        if (viewItem == null) return;

        ItemPresentation itemPresentation = viewItem.getViewDescriptor().getContentPresentation();
        content.setDisplayName(ServiceViewDragHelper.getDisplayName(itemPresentation));
        content.setIcon(itemPresentation.getIcon(false));
      });
    }
  }

  private void loadGroups(Collection<? extends ServiceViewContributor<?>> contributors) {
    if (Registry.is("ide.service.view.split")) {
      for (ServiceViewContributor<?> contributor : contributors) {
        myGroups.put(contributor.getViewDescriptor(myProject).getId(), new SmartList<>(contributor));
      }
    }
    else if (!contributors.isEmpty()) {
      String servicesToolWindowId = getToolWindowId();
      Collection<ServiceViewContributor<?>> servicesContributors =
        myGroups.computeIfAbsent(servicesToolWindowId, __ -> ContainerUtil.newConcurrentSet());
      servicesContributors.addAll(contributors);
    }
  }

  @NotNull
  private Pair<ServiceViewState, List<ServiceViewState>> getServiceViewStates(@NotNull String groupId) {
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
      mainState.clearTreeState();

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
        viewState.clearTreeState();
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

  static final class State {
    public List<ServiceViewState> viewStates = new ArrayList<>();
    public boolean showServicesTree = true;
  }

  private static String getToolWindowId() {
    return ToolWindowId.SERVICES;
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
    if (!isMainView(selectedView) ||
        selectedView.getModel().getVisibleRoots().isEmpty()) {
      return false;
    }

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

  @NotNull
  public List<Object> getChildrenSafe(@NotNull AnActionEvent e, @NotNull List<Object> valueSubPath) {
    ServiceView serviceView = ServiceViewActionProvider.getSelectedView(e);
    return serviceView != null ? serviceView.getChildrenSafe(valueSubPath) : Collections.emptyList();
  }

  @Nullable
  public String getToolWindowId(@NotNull Class<?> contributorClass) {
    for (Map.Entry<String, Collection<ServiceViewContributor<?>>> entry : myGroups.entrySet()) {
      if (entry.getValue().stream().anyMatch(contributorClass::isInstance)) {
        return entry.getKey();
      }
    }
    return null;
  }

  private static boolean isMainView(@NotNull ServiceView serviceView) {
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

  private static final class ServiceViewContentMangerListener implements ContentManagerListener {
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
            AppUIExecutor.onUiThread().expireWith(myContentManager).submit(() -> myContentManager.removeContent(content, true));
          }
        });
      }

      if (myContentManager.getContentCount() > 1) {
        Content mainContent = getMainContent(myContentManager);
        if (mainContent != null) {
          mainContent.setDisplayName(ExecutionBundle.message("service.view.all.services"));
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

  private static void registerActivateByContributorActions(Project project, Collection<? extends ServiceViewContributor<?>> contributors) {
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

  private final class ServiceViewExtensionPointListener implements ExtensionPointListener<ServiceViewContributor<?>> {
    @Override
    public void extensionAdded(@NotNull ServiceViewContributor<?> extension, @NotNull PluginDescriptor pluginDescriptor) {
      List<ServiceViewContributor<?>> contributors = new SmartList<>(extension);
      loadGroups(contributors);
      String toolWindowId = getToolWindowId(extension.getClass());
      boolean register = myGroups.get(toolWindowId).size() == 1;
      ServiceEvent e = ServiceEvent.createResetEvent(extension.getClass());
      myModel.handle(e).onSuccess(o -> {
        if (register) {
          ServiceViewItem eventRoot = ContainerUtil.find(myModel.getRoots(), root -> {
            return extension.getClass().isInstance(root.getRootContributor());
          });
          assert toolWindowId != null;
          registerToolWindow(toolWindowId, eventRoot != null);
        }
        else {
          eventHandled(e);
        }
        if (getToolWindowId().equals(toolWindowId)) {
          AppUIExecutor.onUiThread().expireWith(myProject).submit(() -> registerActivateByContributorActions(myProject, contributors));
        }
      });
    }

    @Override
    public void extensionRemoved(@NotNull ServiceViewContributor<?> extension, @NotNull PluginDescriptor pluginDescriptor) {
      ServiceEvent e = ServiceEvent.createSyncResetEvent(extension.getClass());
      myModel.handle(e).onProcessed(o -> {
        eventHandled(e);

        for (Map.Entry<String, Collection<ServiceViewContributor<?>>> entry : myGroups.entrySet()) {
          if (entry.getValue().remove(extension)) {
            if (entry.getValue().isEmpty()) {
              unregisterToolWindow(entry.getKey());
            }
            break;
          }
        }

        unregisterActivateByContributorActions(extension);
      });
    }

    private void unregisterToolWindow(String toolWindowId) {
      myActiveToolWindowIds.remove(toolWindowId);
      myGroups.remove(toolWindowId);
      for (ServiceViewContentHolder holder : myContentHolders) {
        if (holder.toolWindowId.equals(toolWindowId)) {
          myContentHolders.remove(holder);
          break;
        }
      }
      ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
      toolWindowManager.invokeLater(() -> {
        if (myProject.isDisposed() || myProject.isDefault()) return;

        ToolWindow toolWindow = toolWindowManager.getToolWindow(toolWindowId);
        if (toolWindow != null) {
          toolWindow.remove();
        }
      });
    }

    private void unregisterActivateByContributorActions(ServiceViewContributor<?> extension) {
      String actionId = getActivateContributorActionId(extension);
      if (actionId != null) {
        ActionManager actionManager = ActionManager.getInstance();
        AnAction action = actionManager.getAction(actionId);
        if (action != null) {
          actionManager.unregisterAction(actionId);
        }
      }
    }
  }

  private static class ActivateToolWindowByContributorAction extends DumbAwareAction {
    private final ServiceViewContributor<?> myContributor;

    ActivateToolWindowByContributorAction(ServiceViewContributor<?> contributor, ItemPresentation contributorPresentation) {
      myContributor = contributor;
      Presentation templatePresentation = getTemplatePresentation();
      templatePresentation.setText(ExecutionBundle.messagePointer("service.view.activate.tool.window.action.name",
                                                               ServiceViewDragHelper.getDisplayName(contributorPresentation)));
      templatePresentation.setIcon(contributorPresentation.getIcon(false));
      templatePresentation.setDescription(ExecutionBundle.messagePointer("service.view.activate.tool.window.action.description"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project == null) return;

      String toolWindowId =
        ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).getToolWindowId(myContributor.getClass());
      if (toolWindowId == null) return;

      ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolWindowId);
      if (toolWindow != null) {
        toolWindow.activate(() -> {
          ServiceViewContentHolder holder =
            ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).getContentHolder(myContributor.getClass());
          if (holder != null) {
            selectContentByContributor(holder.contentManager, myContributor);
          }
        });
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

    private void processAllModels(Consumer<? super ServiceViewModel> consumer) {
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
