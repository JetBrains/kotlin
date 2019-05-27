// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.execution.services.ServiceModelFilter.ServiceViewFilter;
import com.intellij.execution.services.ServiceViewDragHelper.ServiceViewDragBean;
import com.intellij.execution.services.ServiceViewModel.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.dnd.DnDDropHandler;
import com.intellij.ide.dnd.DnDEvent;
import com.intellij.ide.dnd.DnDSupport;
import com.intellij.ide.dnd.DnDTargetChecker;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
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
import com.intellij.openapi.wm.impl.InternalDecorator;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.AutoScrollFromSourceHandler;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

@State(name = "ServiceViewManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class ServiceViewManagerImpl implements ServiceViewManager, PersistentStateComponent<ServiceViewManagerImpl.State> {
  private static final String AUTO_SCROLL_TO_SOURCE_PROPERTY = "service.view.auto.scroll.to.source";
  private static final String AUTO_SCROLL_FROM_SOURCE_PROPERTY = "service.view.auto.scroll.from.source";

  // TODO [konstantin.aleev] provide help id
  @NonNls private static final String HELP_ID = "run-dashboard.reference";

  private final Project myProject;
  private State myState = new State();

  private ServiceModel myModel;
  private ServiceModelFilter myModelFilter;
  private ServiceView myAllServicesView;
  private final List<ServiceView> myServiceViews = ContainerUtil.newSmartList();

  private ContentManager myContentManager;
  private Content myAllServicesContent;
  private final Content myDropTargetContent = createDropTargetContent();

  public ServiceViewManagerImpl(@NotNull Project project) {
    myProject = project;
    myProject.getMessageBus().connect(myProject).subscribe(ServiceEventListener.TOPIC, this::updateToolWindow);
  }

  boolean hasServices() {
    for (ServiceViewContributor<?> contributor : ServiceModel.EP_NAME.getExtensions()) {
      if (!contributor.getServices(myProject).isEmpty()) return true;
    }
    return false;
  }

  void createToolWindowContent(@NotNull ToolWindow toolWindow) {
    myModel = new ServiceModel(myProject);
    myModelFilter = new ServiceModelFilter();
    myProject.getMessageBus().connect(myModel).subscribe(ServiceEventListener.TOPIC, e -> myModel.refresh(e).onSuccess(o -> {
      myAllServicesView.getModel().eventProcessed(e);
      for (ServiceView serviceView : myServiceViews) {
        serviceView.getModel().eventProcessed(e);
      }
    }));
    myContentManager = toolWindow.getContentManager();
    myContentManager.addContentManagerListener(new MyContentMangerListener());

    createAllServicesView();

    ToolWindowEx toolWindowEx = (ToolWindowEx)toolWindow;
    toolWindowEx.setAdditionalGearActions(new DefaultActionGroup(ToggleAutoScrollAction.toSource(), ToggleAutoScrollAction.fromSource()));
    toolWindowEx.setTitleActions(ServiceViewAutoScrollFromSourceHandler.install(myProject, toolWindow));

    installDnDSupport(toolWindowEx.getDecorator());

    loadViews();

    registerActivateByContributorActions();
  }

  private void createAllServicesView() {
    myAllServicesView = ServiceView.createView(myProject, new AllServicesModel(myModel, myModelFilter), myState.allServicesViewState);

    myAllServicesContent = ContentFactory.SERVICE.getInstance().createContent(myAllServicesView, null, false);
    myAllServicesContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    myAllServicesContent.setHelpId(getToolWindowContextHelpId());
    myAllServicesContent.setCloseable(false);

    Disposer.register(myAllServicesContent, myModel);
    Disposer.register(myAllServicesContent, myAllServicesView);
    Disposer.register(myAllServicesContent, myAllServicesView.getModel());
    Disposer.register(myAllServicesContent, () -> {
      myAllServicesView = null;
      myAllServicesContent = null;
      myServiceViews.clear();
      myContentManager = null;
      myModel = null;
      myModelFilter = null;
    });

    myContentManager.addContent(myAllServicesContent);

    myAllServicesView.getModel().addModelListener(() -> {
      boolean isEmpty = myAllServicesView.getModel().getRoots().isEmpty();
      AppUIUtil.invokeOnEdt(() -> {
        if (isEmpty) {
          if (myContentManager.getIndexOfContent(myAllServicesContent) >= 0) {
            myContentManager.removeContent(myAllServicesContent, false);
          }
        }
        else {
          if (myContentManager.getIndexOfContent(myAllServicesContent) < 0) {
            myContentManager.addContent(myAllServicesContent, 0);
          }
        }
      }, myProject.getDisposed());
    });
  }

  private void installDnDSupport(InternalDecorator decorator) {
    DnDSupport.createBuilder(decorator)
      .setTargetChecker(new DnDTargetChecker() {
        @Override
        public boolean update(DnDEvent event) {
          Object o = event.getAttachedObject();
          boolean dropPossible = o instanceof ServiceViewDragBean && event.getPoint().y < decorator.getHeaderHeight();
          event.setDropPossible(dropPossible, "");
          if (dropPossible) {
            if (myContentManager.getIndexOfContent(myDropTargetContent) < 0) {
              myContentManager.addContent(myDropTargetContent);
            }

            ServiceViewDragBean dragBean = (ServiceViewDragBean)o;
            ItemPresentation presentation;
            if (dragBean.getItems().size() > 1 && dragBean.getContributor() != null) {
              presentation = dragBean.getContributor().getViewDescriptor().getPresentation();
            }
            else {
              presentation = dragBean.getItems().get(0).getViewDescriptor().getPresentation();
            }
            myDropTargetContent.setDisplayName(ServiceViewDragHelper.getDisplayName(presentation));
            myDropTargetContent.setIcon(presentation.getIcon(false));
          }
          else if (myContentManager.getIndexOfContent(myDropTargetContent) >= 0) {
            myContentManager.removeContent(myDropTargetContent, false);
          }
          return true;
        }
      })
      .setCleanUpOnLeaveCallback(() -> {
        ContentManager contentManager = myContentManager;
        if (contentManager != null && contentManager.getIndexOfContent(myDropTargetContent) >= 0) {
          contentManager.removeContent(myDropTargetContent, false);
        }
      })
      .setDropHandler(new DnDDropHandler() {
        @Override
        public void drop(DnDEvent event) {
          Object o = event.getAttachedObject();
          if (o instanceof ServiceViewDragBean) {
            extract((ServiceViewDragBean)o);
          }
        }
      })
      .install();
    decorator.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        if (myContentManager.getIndexOfContent(myDropTargetContent) >= 0) {
          myContentManager.removeContent(myDropTargetContent, false);
        }
      }
    });
  }

  private void loadViews() {
    myModel.getInvoker().invokeLater(() -> {
      Map<String, ServiceViewContributor> contributors = FactoryMap.create(className -> {
        for (ServiceViewContributor<?> contributor : ServiceModel.EP_NAME.getExtensions()) {
          if (className.equals(contributor.getClass().getName())) {
            return contributor;
          }
        }
        return null;
      });
      List<ServiceViewFilter> filters = new ArrayList<>();

      List<Pair<ServiceViewModel, ServiceViewState>> loadedModels = new ArrayList<>();
      ServiceViewModel toSelect = null;

      for (int i = 0; i < myState.viewStates.size(); i++) {
        ServiceViewState viewState = myState.viewStates.get(i);
        ServiceViewFilter parentFilter = null;
        if (viewState.parentView >= 0 && viewState.parentView < filters.size()) {
          parentFilter = filters.get(viewState.parentView);
        }
        ServiceViewFilter filter = parentFilter;
        ServiceViewModel viewModel = ServiceViewModel.loadModel(viewState, myModel, myModelFilter, parentFilter, contributors);
        if (viewModel != null) {
          loadedModels.add(Pair.create(viewModel, viewState));
          if (myState.selectedView == i) {
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
          selectContentByModel(modelToSelect);
          filtersChanged();
        }, myProject.getDisposed());
      }
    });
  }

  private void selectContentByModel(@Nullable ServiceViewModel modelToSelect) {
    if (modelToSelect != null) {
      for (Content content : myContentManager.getContents()) {
        if (((ServiceView)content.getComponent()).getModel() == modelToSelect) {
          myContentManager.setSelectedContent(content);
          break;
        }
      }
    }
    else {
      Content content = myContentManager.getContent(0);
      if (content != null) {
        myContentManager.setSelectedContent(content);
      }
    }
  }

  private void filtersChanged() {
    myModel.getInvoker().invokeLater(() -> {
      myAllServicesView.getModel().filtersChanged();
      for (ServiceView serviceView : myServiceViews) {
        serviceView.getModel().filtersChanged();
      }
    });
  }

  private void registerActivateByContributorActions() {
    for (ServiceViewContributor contributor : ServiceModel.EP_NAME.getExtensions()) {
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

  @NotNull
  @Override
  public Promise<Void> select(@NotNull Object service, @NotNull Class<?> contributorClass, boolean activate, boolean focus) {
    AsyncPromise<Void> result = new AsyncPromise<>();
    AppUIUtil.invokeLaterIfProjectAlive(myProject, () -> {
      Runnable runnable = () -> {
        ContentManager contentManager = myContentManager;
        List<Content> contents =
          contentManager == null ? Collections.emptyList() : ContainerUtil.newSmartList(contentManager.getContents());
        if (contents.isEmpty()) {
          result.setError("Content not initialized");
          return;
        }

        Collections.reverse(contents);
        select(myProject, contents.iterator(), result, service, contributorClass);
      };
      ToolWindow window = activate ? ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.SERVICES) : null;
      if (window != null) {
        window.activate(runnable, false, focus);
      }
      else {
        runnable.run();
      }
    });
    return result;
  }

  private static void select(Project project, Iterator<Content> iterator, AsyncPromise<Void> result,
                             @NotNull Object service, @NotNull Class<?> contributorClass) {
    Content content = iterator.next();
    ((ServiceView)content.getComponent()).select(service, contributorClass)
      .onSuccess(v -> {
        AppUIUtil.invokeOnEdt(() -> {
          ContentManager contentManager = content.getManager();
          if (contentManager == null) return;

          if (contentManager.getSelectedContent() != content && contentManager.getIndexOfContent(content) >= 0) {
            contentManager.setSelectedContent(content);
          }
        }, project.getDisposed());

        result.setResult(null);
      })
      .onError(e -> {
        if (iterator.hasNext()) {
          select(project, iterator, result, service, contributorClass);
        }
        else {
          result.setError(e);
        }
      });
  }

  private void updateToolWindow(ServiceEventListener.ServiceEvent event) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    if (toolWindowManager == null) return;

    toolWindowManager.invokeLater(() -> {
      if (myProject.isDisposed()) {
        return;
      }

      boolean available = hasServices();
      ToolWindow toolWindow = toolWindowManager.getToolWindow(ToolWindowId.SERVICES);
      if (toolWindow == null) {
        toolWindow = createToolWindow(toolWindowManager, available);
        if (available) {
          toolWindow.show(null);
        }
        return;
      }

      boolean doShow = !toolWindow.isAvailable() && available;
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

    ServiceView selectedView = getSelectedView();
    ServiceViewFilter parentFilter = selectedView == null ? null : selectedView.getModel().getFilter();
    ServiceViewModel viewModel = ServiceViewModel.createModel(items, dragBean.getContributor(), myModel, myModelFilter, parentFilter);
    extract(viewModel, new ServiceViewState(), true);
  }

  private void extract(ServiceViewModel viewModel, ServiceViewState viewState, boolean select) {
    ServiceView serviceView = ServiceView.createView(myProject, viewModel, viewState);
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
    addServiceView(serviceView, viewModel.getContributor().getViewDescriptor().getContentPresentation(), select);
  }

  private void extractGroup(GroupModel viewModel, ServiceView serviceView, boolean select) {
    Content content = addServiceView(serviceView, viewModel.getGroup().getViewDescriptor().getContentPresentation(), select);
    viewModel.addModelListener(() -> updateContentTab(viewModel.getGroup(), content));
  }

  private void extractService(SingeServiceModel viewModel, ServiceView serviceView, boolean select) {
    Content content = addServiceView(serviceView, viewModel.getService().getViewDescriptor().getContentPresentation(), select);
    viewModel.addModelListener(() -> {
      ServiceViewItem item = viewModel.getService();
      if (item != null && !viewModel.getChildren(item).isEmpty()) {
        AppUIUtil.invokeOnEdt(() -> {
          int index = myContentManager.getIndexOfContent(content);
          myContentManager.removeContent(content, true);
          ServiceListModel listModel = new ServiceListModel(myModel, myModelFilter, ContainerUtil.newSmartList(item),
                                                            viewModel.getFilter().getParent());
          ServiceView listView = ServiceView.createView(myProject, listModel, new ServiceViewState());
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

    Content content = addServiceView(serviceView, presentation, select, index);
    viewModel.addModelListener(() -> updateContentTab(ContainerUtil.getOnlyItem(viewModel.getRoots()), content));
  }

  private Content addServiceView(ServiceView serviceView, ItemPresentation presentation, boolean select) {
    return addServiceView(serviceView, presentation, select, -1);
  }

  private Content addServiceView(ServiceView serviceView, ItemPresentation presentation, boolean select, int index) {
    int viewIndex = index == -1 ? -1 : myContentManager.getIndexOfContent(myAllServicesContent) >= 0 ? index - 1 : index;
    myServiceViews.add(viewIndex == -1 ? myServiceViews.size() : viewIndex, serviceView);
    List<ServiceViewFilter> filters = myModelFilter.getFilters();
    filters.add(viewIndex == -1 ? filters.size() : viewIndex, serviceView.getModel().getFilter());

    Content content =
      ContentFactory.SERVICE.getInstance().createContent(serviceView, ServiceViewDragHelper.getDisplayName(presentation), false);
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    content.setHelpId(getToolWindowContextHelpId());
    content.setCloseable(true);
    content.setIcon(presentation.getIcon(false));

    Disposer.register(content, serviceView);
    Disposer.register(content, serviceView.getModel());

    myContentManager.addContent(content, index);
    if (select) {
      myContentManager.setSelectedContent(content);
    }
    serviceView.getModel().addModelListener(() -> {
      if (serviceView.getModel().getRoots().isEmpty()) {
        AppUIUtil.invokeOnEdt(() -> myContentManager.removeContent(content, true), myProject.getDisposed());
      }
    });
    filtersChanged();
    return content;
  }

  @Nullable
  ServiceView getSelectedView() {
    ContentManager contentManager = myContentManager;
    Content content = contentManager == null ? null : contentManager.getSelectedContent();
    return content == null ? null : ObjectUtils.tryCast(content.getComponent(), ServiceView.class);
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
    if (myAllServicesView != null) {
      myAllServicesView.saveState(myState.allServicesViewState);
      myState.allServicesViewState.treeStateElement = new Element("root");
      myState.allServicesViewState.treeState.writeExternal(myState.allServicesViewState.treeStateElement);
    }
    myState.viewStates.clear();
    for (ServiceView serviceView : myServiceViews) {
      ServiceViewState viewState = new ServiceViewState();
      myState.viewStates.add(viewState);
      serviceView.saveState(viewState);
      ServiceViewModel viewModel = serviceView.getModel();
      if (viewModel instanceof ServiceListModel) {
        viewState.id = myContentManager.getContent(serviceView).getDisplayName();
      }
      viewState.parentView = myModelFilter.getFilters().indexOf(viewModel.getFilter().getParent());

      viewState.treeStateElement = new Element("root");
      viewState.treeState.writeExternal(viewState.treeStateElement);
    }
    if (myContentManager != null) {
      myState.selectedView = myServiceViews.indexOf(getSelectedView());
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
    public int selectedView = -1;
  }

  private static String getToolWindowId() {
    return ToolWindowId.SERVICES;
  }

  private static Icon getToolWindowIcon() {
    return AllIcons.Toolwindows.ToolWindowServices;
  }

  static String getToolWindowContextHelpId() {
    return HELP_ID;
  }

  private static Content createDropTargetContent() {
    Content content = ContentFactory.SERVICE.getInstance().createContent(new JPanel(), null, false);
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    content.setCloseable(true);
    return content;
  }

  boolean isSplitByTypeEnabled() {
    ContentManager contentManager = myContentManager;
    if (contentManager == null) return false;

    if (contentManager.getIndexOfContent(myAllServicesContent) < 0) return false;

    for (ServiceView serviceView : myServiceViews) {
      if (!(serviceView.getModel() instanceof ContributorModel)) return false;
    }

    return true;
  }

  void splitByType() {
    myModel.getInvoker().invokeLater(() -> {
      List<ServiceViewContributor> contributors = new ArrayList<>();
      for (ServiceViewContributor contributor : ServiceModel.EP_NAME.getExtensions()) {
        if (!contributor.getServices(myProject).isEmpty()) {
          contributors.add(contributor);
        }
      }
      AppUIUtil.invokeOnEdt(() -> {
        for (ServiceViewContributor contributor : contributors) {
          splitByType(contributor);
        }
      });
    });
  }

  private void splitByType(ServiceViewContributor contributor) {
    for (ServiceView serviceView : myServiceViews) {
      ServiceViewModel viewModel = serviceView.getModel();
      if (viewModel instanceof ContributorModel && contributor.equals(((ContributorModel)viewModel).getContributor())) {
        return;
      }
    }

    ContributorModel contributorModel = new ContributorModel(myModel, myModelFilter, contributor, null);
    ServiceView contributorView = ServiceView.createView(myProject, contributorModel, new ServiceViewState());
    extractContributor(contributorModel, contributorView, true);
  }

  private class MyContentMangerListener extends ContentManagerAdapter {
    @Override
    public void contentAdded(@NotNull ContentManagerEvent event) {
      if (myContentManager.getContentCount() > 1) {
        myAllServicesContent.setDisplayName("All Services");
      }
    }

    @Override
    public void contentRemoved(@NotNull ContentManagerEvent event) {
      if (event.getContent() != myDropTargetContent && event.getContent() != myAllServicesContent) {
        ServiceView removedView = (ServiceView)event.getContent().getComponent();
        myModelFilter.removeFilter(removedView.getModel().getFilter());
        myServiceViews.remove(removedView);
        filtersChanged();
      }
      if (myContentManager.getContentCount() == 1) {
        myAllServicesContent.setDisplayName(null);
      }
    }

    @Override
    public void selectionChanged(@NotNull ContentManagerEvent event) {
      if (event.getContent() == myDropTargetContent) return;

      ServiceView serviceView = (ServiceView)event.getContent().getComponent();
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
      ServiceViewManagerImpl manager = (ServiceViewManagerImpl)ServiceViewManager.getInstance(project);
      for (ServiceView serviceView : manager.myServiceViews) {
        if (serviceView.getModel() instanceof ContributorModel &&
            myContributor.equals(((ContributorModel)serviceView.getModel()).getContributor())) {
          Content content = manager.myContentManager.getContent(serviceView);
          if (content != null) {
            manager.myContentManager.setSelectedContent(content, true);
          }
          return;
        }
      }
      if (manager.myContentManager.getIndexOfContent(manager.myAllServicesContent) >= 0) {
        manager.myContentManager.setSelectedContent(manager.myAllServicesContent, true);
      }
    }
  }

  static boolean isAutoScrollToSourceEnabled(@NotNull Project project) {
    return PropertiesComponent.getInstance(project).getBoolean(AUTO_SCROLL_TO_SOURCE_PROPERTY);
  }

  private static boolean isAutoScrollFromSourceEnabled(@NotNull Project project) {
    return PropertiesComponent.getInstance(project).getBoolean(AUTO_SCROLL_FROM_SOURCE_PROPERTY);
  }

  private static class ToggleAutoScrollAction extends ToggleAction implements DumbAware {
    private final String myProperty;

    ToggleAutoScrollAction(@NotNull String text, @NotNull String property) {
      super(text);
      myProperty = property;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      return project != null && PropertiesComponent.getInstance(project).getBoolean(myProperty);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      Project project = e.getProject();
      if (project != null) PropertiesComponent.getInstance(project).setValue(myProperty, state);
    }

    @NotNull
    static AnAction toSource() {
      return new ToggleAutoScrollAction("Autoscroll to Source", AUTO_SCROLL_TO_SOURCE_PROPERTY);
    }

    @NotNull
    static AnAction fromSource() {
      return new ToggleAutoScrollAction("Autoscroll from Source", AUTO_SCROLL_FROM_SOURCE_PROPERTY);
    }
  }

  private static class ServiceViewAutoScrollFromSourceHandler extends AutoScrollFromSourceHandler {
    ServiceViewAutoScrollFromSourceHandler(@NotNull Project project, @NotNull ToolWindow toolWindow) {
      super(project, toolWindow.getComponent(), toolWindow.getContentManager());
    }

    @Override
    protected boolean isAutoScrollEnabled() {
      return isAutoScrollFromSourceEnabled(myProject);
    }

    @Override
    protected void setAutoScrollEnabled(boolean enabled) {
      PropertiesComponent.getInstance(myProject).setValue(AUTO_SCROLL_FROM_SOURCE_PROPERTY, true);
    }

    @Override
    protected void selectElementFromEditor(@NotNull FileEditor editor) {
      select(editor);
    }

    private boolean select(@NotNull FileEditor editor) {
      for (ServiceViewContributor extension : ServiceModel.EP_NAME.getExtensions()) {
        if (!(extension instanceof ServiceViewFileEditorContributor)) continue;
        if (selectContributorNode(editor, (ServiceViewFileEditorContributor)extension, extension.getClass())) return true;
      }
      return false;
    }

    private boolean selectContributorNode(@NotNull FileEditor editor, @NotNull ServiceViewFileEditorContributor extension,
                                          @NotNull Class<?> contributorClass) {
      Object service = extension.findService(myProject, editor);
      if (service == null) return false;
      if (service instanceof ServiceViewFileEditorContributor) {
        if (selectContributorNode(editor, (ServiceViewFileEditorContributor)service, contributorClass)) return true;
      }
      ServiceViewManager.getInstance(myProject).select(service, contributorClass, true, true);
      return true;
    }

    @NotNull
    public static AnAction install(@NotNull Project project, @NotNull ToolWindow window) {
      ServiceViewAutoScrollFromSourceHandler handler = new ServiceViewAutoScrollFromSourceHandler(project, window);
      handler.install();
      return new ScrollFromEditorAction(handler);
    }
  }

  private static class ScrollFromEditorAction extends DumbAwareAction {
    private final ServiceViewAutoScrollFromSourceHandler myScrollFromHandler;

    ScrollFromEditorAction(ServiceViewAutoScrollFromSourceHandler scrollFromHandler) {
      super("Scroll from Source", "Select node open in the active editor", AllIcons.General.Locate);
      myScrollFromHandler = scrollFromHandler;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project == null) return;
      FileEditorManager manager = FileEditorManager.getInstance(project);
      FileEditor[] editors = manager.getSelectedEditors();
      if (editors.length == 0) return;
      for (FileEditor editor : editors) {
        if (myScrollFromHandler.select(editor)) return;
      }
    }
  }
}
