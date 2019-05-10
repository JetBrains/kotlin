// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceGroupNode;
import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.execution.services.ServiceViewDragHelper.ServiceViewDragBean;
import com.intellij.execution.services.ServiceViewModel.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.dnd.DnDDropHandler;
import com.intellij.ide.dnd.DnDEvent;
import com.intellij.ide.dnd.DnDSupport;
import com.intellij.ide.dnd.DnDTargetChecker;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
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
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.impl.InternalDecorator;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.AutoScrollFromSourceHandler;
import com.intellij.ui.content.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Property;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
  private final List<ServiceView> myServiceViews = ContainerUtil.newSmartList();

  private ContentManager myContentManager;
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
      for (ServiceView serviceView : myServiceViews) {
        serviceView.getModel().eventProcessed(e);
      }
    }));
    ServiceView serviceView = ServiceView.createTreeView(myProject, new AllServicesModel(myModel, myModelFilter), myState.viewState);
    myServiceViews.add(serviceView);

    Content allServicesContent = ContentFactory.SERVICE.getInstance().createContent(serviceView, null, false);
    allServicesContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    allServicesContent.setHelpId(getToolWindowContextHelpId());
    allServicesContent.setCloseable(false);

    Disposer.register(allServicesContent, myModel);
    Disposer.register(allServicesContent, serviceView);
    Disposer.register(allServicesContent, serviceView.getModel());
    Disposer.register(allServicesContent, () -> {
      myServiceViews.clear();
      myContentManager = null;
      myModel = null;
      myModelFilter = null;
    });

    myContentManager = toolWindow.getContentManager();
    myContentManager.addContentManagerListener(new MyContentMangerListener(allServicesContent));
    myContentManager.addContent(allServicesContent);

    ToolWindowEx toolWindowEx = (ToolWindowEx)toolWindow;
    toolWindowEx.setAdditionalGearActions(new DefaultActionGroup(ToggleAutoScrollAction.toSource(), ToggleAutoScrollAction.fromSource()));
    toolWindowEx.setTitleActions(ServiceViewAutoScrollFromSourceHandler.install(myProject, toolWindow));
    installDnDSupport(toolWindowEx.getDecorator());
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
        if (myContentManager.getIndexOfContent(myDropTargetContent) >= 0) {
          myContentManager.removeContent(myDropTargetContent, false);
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

    ServiceViewContributor contributor = dragBean.getContributor();
    if (contributor != null) {
      List<? extends ServiceViewItem> roots = myModel.getRoots();
      List<? extends ServiceViewItem> contributorRoots = ContainerUtil.filter(roots, node -> contributor == node.getContributor());
      if (contributorRoots.equals(items)) {
        extractContributor(contributor);
        return;
      }
    }

    if (items.size() == 1) {
      ServiceViewItem item = items.get(0);
      if (item instanceof ServiceGroupNode) {
        extractGroup((ServiceGroupNode)item);
      }
      else {
        extractService(item);
      }
      return;
    }
    extractList(items, -1);
  }

  private void extractContributor(ServiceViewContributor contributor) {
    ServiceView serviceView =
      ServiceView.createTreeView(myProject,
                                 new ContributorModel(myModel, myModelFilter, contributor, getSelectedView().getModel().getFilter()),
                                 myState.viewState);
    ItemPresentation presentation = contributor.getViewDescriptor().getContentPresentation();
    addServiceView(serviceView, ServiceViewDragHelper.getDisplayName(presentation), presentation.getIcon(false));
  }

  private void extractGroup(ServiceGroupNode group) {
    AtomicReference<ServiceGroupNode> ref = new AtomicReference<>(group);
    ServiceView serviceView =
      ServiceView.createTreeView(myProject,
                                 new GroupModel(myModel, myModelFilter, ref, getSelectedView().getModel().getFilter()),
                                 myState.viewState);

    ItemPresentation presentation = group.getViewDescriptor().getContentPresentation();
    Content content = addServiceView(serviceView, ServiceViewDragHelper.getDisplayName(presentation), presentation.getIcon(false));
    serviceView.getModel().addModelListener(() -> updateContentTab(ref.get(), content));
  }

  private void extractService(ServiceViewItem service) {
    AtomicReference<ServiceViewItem> ref = new AtomicReference<>(service);
    ServiceView serviceView =
      ServiceView.createSingleView(myProject,
                                   new SingeServiceModel(myModel, myModelFilter, ref, getSelectedView().getModel().getFilter()));

    ItemPresentation presentation = service.getViewDescriptor().getContentPresentation();
    Content content = addServiceView(serviceView, ServiceViewDragHelper.getDisplayName(presentation), presentation.getIcon(false));
    serviceView.getModel().addModelListener(() -> {
      ServiceViewItem item = ref.get();
      if (item != null && !serviceView.getModel().getChildren(item).isEmpty()) {
        AppUIUtil.invokeOnEdt(() -> {
          int index = myContentManager.getIndexOfContent(content);
          myContentManager.removeContent(content, true);
          extractList(ContainerUtil.newSmartList(item), index);
        }, myProject.getDisposed());
      }
      else {
        updateContentTab(item, content);
      }
    });
  }

  private void extractList(List<ServiceViewItem> items, int index) {
    String name;
    Icon icon;
    if (items.size() == 1) {
      ItemPresentation presentation = items.get(0).getViewDescriptor().getContentPresentation();
      name = ServiceViewDragHelper.getDisplayName(presentation);
      icon = presentation.getIcon(false);
    }
    else {
      name = Messages.showInputDialog(myProject, "Group Name:", "Group Services", null, null, null);
      if (name == null) return;

      icon = AllIcons.Nodes.Folder;
    }

    ServiceView serviceView =
      ServiceView.createTreeView(myProject,
                                 new ServiceListModel(myModel, myModelFilter, items, getSelectedView().getModel().getFilter()),
                                 myState.viewState);
    Content content = addServiceView(serviceView, name, icon, index);
    serviceView.getModel().addModelListener(() -> updateContentTab(ContainerUtil.getOnlyItem(serviceView.getModel().getRoots()), content));
  }

  private Content addServiceView(ServiceView serviceView, String displayName, Icon icon) {
    return addServiceView(serviceView, displayName, icon, -1);
  }

  private Content addServiceView(ServiceView serviceView, String displayName, Icon icon, int index) {
    myServiceViews.add(index == -1 ? myServiceViews.size() : index, serviceView);
    myModelFilter.addFilter(serviceView.getModel().getFilter());

    Content content = ContentFactory.SERVICE.getInstance().createContent(serviceView, displayName, false);
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    content.setHelpId(getToolWindowContextHelpId());
    content.setCloseable(true);
    content.setIcon(icon);

    Disposer.register(content, serviceView);
    Disposer.register(content, serviceView.getModel());

    ServiceView selectedView = getSelectedView();
    myContentManager.addContent(content, index);
    myContentManager.setSelectedContent(content);
    myModel.getInvoker().invokeLater(() -> selectedView.getModel().filtersChanged());
    serviceView.getModel().addModelListener(() -> {
      if (serviceView.getModel().getRoots().isEmpty()) {
        AppUIUtil.invokeOnEdt(() -> myContentManager.removeContent(content, true), myProject.getDisposed());
      }
    });
    return content;
  }

  private ServiceView getSelectedView() {
    return (ServiceView)myContentManager.getSelectedContent().getComponent();
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
    ServiceView serviceView = ContainerUtil.getFirstItem(myServiceViews);
    if (serviceView != null) {
      serviceView.saveState(myState.viewState);
      myState.viewState.treeStateElement = new Element("root");
      myState.viewState.treeState.writeExternal(myState.viewState.treeStateElement);
    }
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState = state;
    myState.viewState.treeState = TreeState.createFrom(myState.viewState.treeStateElement);
  }

  static class State {
    @Property(surroundWithTag = false)
    public ServiceViewState viewState = new ServiceViewState();
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

  private class MyContentMangerListener extends ContentManagerAdapter {
    private final Content myAllServiceContent;

    private MyContentMangerListener(Content allServiceContent) {
      myAllServiceContent = allServiceContent;
    }

    @Override
    public void contentAdded(@NotNull ContentManagerEvent event) {
      if (myContentManager.getContentCount() > 1) {
        myAllServiceContent.setDisplayName("All Services");
      }
    }

    @Override
    public void contentRemoved(@NotNull ContentManagerEvent event) {
      if (event.getContent() != myDropTargetContent) {
        ServiceView removedView = (ServiceView)event.getContent().getComponent();
        myModelFilter.removeFilter(removedView.getModel().getFilter());
        myServiceViews.remove(removedView);
        myModel.getInvoker().invokeLater(() -> {
          for (ServiceView serviceView : myServiceViews) {
            serviceView.getModel().filtersChanged();
          }
        });
      }
      if (myContentManager.getContentCount() == 1) {
        myAllServiceContent.setDisplayName(null);
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
