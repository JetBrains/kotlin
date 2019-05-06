// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.AutoScrollFromSourceHandler;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.xmlb.annotations.Property;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;

@State(name = "ServiceViewManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class ServiceViewManagerImpl implements ServiceViewManager, PersistentStateComponent<ServiceViewManagerImpl.State> {
  private static final String AUTO_SCROLL_TO_SOURCE_PROPERTY = "service.view.auto.scroll.to.source";
  private static final String AUTO_SCROLL_FROM_SOURCE_PROPERTY = "service.view.auto.scroll.from.source";

  static final ExtensionPointName<ServiceViewContributor> EP_NAME =
    ExtensionPointName.create("com.intellij.serviceViewContributor");

  // TODO [konstantin.aleev] provide help id
  @NonNls private static final String HELP_ID = "run-dashboard.reference";

  @NotNull private final Project myProject;

  @NotNull private State myState = new State();
  private ServiceView myServiceView;

  public ServiceViewManagerImpl(@NotNull Project project) {
    myProject = project;
    myProject.getMessageBus().connect(myProject).subscribe(ServiceViewEventListener.TOPIC, this::updateToolWindow);
  }

  boolean hasServices() {
    for (ServiceViewContributor<?> contributor : EP_NAME.getExtensions()) {
      if (!contributor.getServices(myProject).isEmpty()) return true;
    }
    return false;
  }

  void createToolWindowContent(@NotNull ToolWindow toolWindow) {
    ServiceViewModel model = new ServiceViewModel.AllServicesModel(myProject);
    myProject.getMessageBus().connect(model).subscribe(ServiceViewEventListener.TOPIC, model::refresh);
    myServiceView = ServiceView.createTreeView(myProject, model, myState.viewState);

    Content toolWindowContent = ContentFactory.SERVICE.getInstance().createContent(myServiceView, null, false);
    toolWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    toolWindowContent.setHelpId(getToolWindowContextHelpId());
    toolWindowContent.setCloseable(false);

    Disposer.register(toolWindowContent, myServiceView);
    Disposer.register(toolWindowContent, model);
    Disposer.register(toolWindowContent, () -> myServiceView = null);

    toolWindow.getContentManager().addContent(toolWindowContent);

    ToolWindowEx toolWindowEx = (ToolWindowEx)toolWindow;
    toolWindowEx.setAdditionalGearActions(new DefaultActionGroup(ToggleAutoScrollAction.toSource(), ToggleAutoScrollAction.fromSource()));
    toolWindowEx.setTitleActions(ServiceViewAutoScrollFromSourceHandler.install(myProject, toolWindow));
  }

  @NotNull
  @Override
  public Promise<Void> select(@NotNull Object service, @NotNull Class<?> contributorClass, boolean activate, boolean focus) {
    AsyncPromise<Void> result = new AsyncPromise<>();
    AppUIUtil.invokeLaterIfProjectAlive(myProject, () -> {
      Runnable runnable = () -> {
        ServiceView view = myServiceView;
        if (view == null) {
          result.setError("Content not initialized");
        }
        else {
          view.select(service, contributorClass).processed(result);
        }
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

  private void updateToolWindow(ServiceViewEventListener.ServiceEvent event) {
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

  @NotNull
  @Override
  public State getState() {
    ServiceView serviceView = myServiceView;
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

  static boolean isAutoScrollToSourceEnabled(@NotNull Project project) {
    return PropertiesComponent.getInstance(project).getBoolean(AUTO_SCROLL_TO_SOURCE_PROPERTY);
  }

  private static boolean isAutoScrollFromSourceEnabled(@NotNull Project project) {
    return PropertiesComponent.getInstance(project).getBoolean(AUTO_SCROLL_FROM_SOURCE_PROPERTY);
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
      for (ServiceViewContributor extension : EP_NAME.getExtensions()) {
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
