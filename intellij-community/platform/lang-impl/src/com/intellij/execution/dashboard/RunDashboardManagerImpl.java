// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard;

import com.google.common.collect.Sets;
import com.intellij.execution.*;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.tree.RunConfigurationNode;
import com.intellij.execution.dashboard.tree.RunDashboardGrouper;
import com.intellij.execution.dashboard.tree.RunDashboardStatusFilter;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.services.ServiceEventListener;
import com.intellij.execution.services.ServiceViewManager;
import com.intellij.execution.services.ServiceViewManagerImpl;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManagerImpl;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.impl.RunnerLayoutUiImpl;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.impl.ToolWindowImpl;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import com.intellij.ui.content.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author konstantin.aleev
 */
@State(
  name = "RunDashboard",
  storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public class RunDashboardManagerImpl implements RunDashboardManager, PersistentStateComponent<RunDashboardManagerImpl.State> {
  private static final ExtensionPointName<RunDashboardCustomizer> CUSTOMIZER_EP_NAME =
    ExtensionPointName.create("com.intellij.runDashboardCustomizer");
  private static final ExtensionPointName<RunDashboardDefaultTypesProvider> DEFAULT_TYPES_PROVIDER_EP_NAME =
    ExtensionPointName.create("com.intellij.runDashboardDefaultTypesProvider");
  private static final float DEFAULT_CONTENT_PROPORTION = 0.3f;
  @NonNls private static final String HELP_ID = "run-dashboard.reference";

  private final Project myProject;
  private final ContentManager myContentManager;
  private final ContentManagerListener myServiceContentManagerListener;
  private final ContentManagerListener myDashboardContentManagerListener;

  private State myState = new State();

  private final Set<String> myTypes = new THashSet<>();
  private volatile List<List<RunDashboardServiceImpl>> myServices = Collections.emptyList();
  private final ReentrantReadWriteLock myServiceLock = new ReentrantReadWriteLock();
  private final List<RunDashboardGrouper> myGroupers;
  private final RunDashboardStatusFilter myStatusFilter = new RunDashboardStatusFilter();
  private String myToolWindowId;
  private final Condition<Content> myReuseCondition;
  private final AtomicBoolean myListenersInitialized = new AtomicBoolean();
  private boolean myShowConfigurations = true;

  private RunDashboardContent myDashboardContent;
  private Content myToolWindowContent;
  private ContentManager myToolWindowContentManager;
  private ContentManagerListener myToolWindowContentManagerListener;
  private final Map<Content, Content> myDashboardToToolWindowContents = new HashMap<>();

  public RunDashboardManagerImpl(@NotNull final Project project) {
    myProject = project;

    ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    myContentManager = contentFactory.createContentManager(new PanelContentUI(), false, project);
    myDashboardContentManagerListener = new DashboardContentManagerListener();
    myContentManager.addContentManagerListener(myDashboardContentManagerListener);
    myServiceContentManagerListener = new ServiceContentManagerListener();
    myReuseCondition = this::canReuseContent;

    myGroupers = ContainerUtil.map(RunDashboardGroupingRule.EP_NAME.getExtensions(), RunDashboardGrouper::new);
  }

  private void initToolWindowContentListeners() {
    if (!myListenersInitialized.compareAndSet(false, true)) return;

    MessageBusConnection connection = myProject.getMessageBus().connect(myProject);
    connection.subscribe(RunManagerListener.TOPIC, new RunManagerListener() {
      private volatile boolean myUpdateStarted;

      @Override
      public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
        if (!myUpdateStarted) {
          syncConfigurations();
          updateDashboardIfNeeded(settings);
        }
      }

      @Override
      public void runConfigurationRemoved(@NotNull RunnerAndConfigurationSettings settings) {
        if (!myUpdateStarted) {
          syncConfigurations();
          updateDashboardIfNeeded(settings);
        }
      }

      @Override
      public void runConfigurationChanged(@NotNull RunnerAndConfigurationSettings settings) {
        if (!myUpdateStarted) {
          updateDashboardIfNeeded(settings);
        }
      }

      @Override
      public void beginUpdate() {
        myUpdateStarted = true;
      }

      @Override
      public void endUpdate() {
        myUpdateStarted = false;
        syncConfigurations();
        updateDashboard(true);
      }
    });
    connection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
      @Override
      public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, final @NotNull ProcessHandler handler) {
        updateToolWindowContent();
        updateDashboardIfNeeded(env.getRunnerAndConfigurationSettings());
      }

      @Override
      public void processTerminated(@NotNull String executorId,
                                    @NotNull ExecutionEnvironment env,
                                    @NotNull ProcessHandler handler,
                                    int exitCode) {
        updateToolWindowContent();
        updateDashboardIfNeeded(env.getRunnerAndConfigurationSettings());
      }
    });
    connection.subscribe(RunDashboardManager.DASHBOARD_TOPIC, new RunDashboardListener() {
      @Override
      public void configurationChanged(@NotNull RunConfiguration configuration, boolean withStructure) {
        updateDashboardIfNeeded(configuration, withStructure);
      }
    });
    connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      @Override
      public void exitDumbMode() {
        updateDashboard(false);
      }
    });
    myContentManager.addContentManagerListener(myServiceContentManagerListener);
  }

  @Override
  public ContentManager getDashboardContentManager() {
    return myContentManager;
  }

  @Override
  public String getToolWindowId() {
    if (myToolWindowId == null) {
      if (Registry.is("ide.service.view")) {
        String toolWindowId =
          ((ServiceViewManagerImpl)ServiceViewManager.getInstance(myProject))
            .getToolWindowId(RunConfigurationsServiceViewContributor.class);
        myToolWindowId = toolWindowId != null ? toolWindowId : ToolWindowId.SERVICES;
      }
      else {
        myToolWindowId = ToolWindowId.RUN_DASHBOARD;
      }
    }
    return myToolWindowId;
  }

  @Override
  @NotNull
  public Icon getToolWindowIcon() {
    return Registry.is("ide.service.view")
           ? AllIcons.Toolwindows.ToolWindowServices
           : AllIcons.Toolwindows.ToolWindowRun; // TODO [konstantin.aleev] provide new icon
  }

  @Override
  public String getToolWindowContextHelpId() {
    return HELP_ID;
  }

  @Override
  public boolean isToolWindowAvailable() {
    return hasContent();
  }

  @Override
  public void createToolWindowContent(@NotNull ToolWindow toolWindow) {
    myDashboardContent = new RunDashboardContent(myProject, myContentManager, myGroupers);
    myToolWindowContent = ContentFactory.SERVICE.getInstance().createContent(myDashboardContent, null, false);
    myToolWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    myToolWindowContent.setHelpId(getToolWindowContextHelpId());
    myToolWindowContent.setCloseable(false);
    Disposer.register(myToolWindowContent, myDashboardContent);
    Disposer.register(myToolWindowContent, () -> {
      myDashboardContent = null;
      myToolWindowContent = null;
      myToolWindowContentManager.removeContentManagerListener(myToolWindowContentManagerListener);
      myToolWindowContentManager = null;
      myToolWindowContentManagerListener = null;
      myDashboardToToolWindowContents.clear();
    });

    myToolWindowContentManager = toolWindow.getContentManager();
    myToolWindowContentManager.addContent(myToolWindowContent);

    myToolWindowContentManagerListener = new ToolWindowContentManagerListener();
    myToolWindowContentManager.addContentManagerListener(myToolWindowContentManagerListener);
  }

  @Override
  public List<RunDashboardService> getRunConfigurations() {
    myServiceLock.readLock().lock();
    try {
      return myServices.stream().flatMap(s -> s.stream()).collect(Collectors.toList());
    }
    finally {
      myServiceLock.readLock().unlock();
    }
  }

  private List<RunContentDescriptor> filterByContent(List<RunContentDescriptor> descriptors) {
    return ContainerUtil.filter(descriptors, descriptor -> {
      Content content = descriptor.getAttachedContent();
      return content != null && content.getManager() == myContentManager;
    });
  }

  @Override
  public boolean isShowConfigurations() {
    return myShowConfigurations;
  }

  @Override
  public void setShowConfigurations(boolean value) {
    myShowConfigurations = value;

    // Ensure dashboard tree gets focus before tool window content update.
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    if (toolWindowManager == null) return;
    toolWindowManager.invokeLater(() -> {
      if (myProject.isDisposed()) {
        return;
      }
      if (myToolWindowContentManager != null) {
        Content content = myToolWindowContentManager.getSelectedContent();
        if (content != null && content.equals(myToolWindowContent)) {
          myToolWindowContentManager.setSelectedContent(content, true);
        }
      }
    });
    // Hide or show dashboard tree at first in order to get focus events on tree component which will be added/removed from tool window.
    updateDashboard(false);
    // Add or remove dashboard tree content from tool window.
    updateToolWindowContent();
  }

  @Override
  public float getContentProportion() {
    return myState.contentProportion;
  }

  @Override
  public boolean isShowInDashboard(@NotNull RunConfiguration runConfiguration) {
    return myTypes.contains(runConfiguration.getType().getId());
  }

  @Override
  @NotNull
  public Set<String> getTypes() {
    return Collections.unmodifiableSet(myTypes);
  }

  @Override
  public void setTypes(@NotNull Set<String> types) {
    Set<String> removed = new HashSet<>(Sets.difference(myTypes, types));
    Set<String> added = new HashSet<>(Sets.difference(types, myTypes));

    myTypes.clear();
    myTypes.addAll(types);
    if (!myTypes.isEmpty()) {
      initToolWindowContentListeners();
    }

    Set<String> enableByDefaultTypes = getEnableByDefaultTypes();
    myState.configurationTypes.clear();
    myState.configurationTypes.addAll(myTypes);
    myState.configurationTypes.removeAll(enableByDefaultTypes);
    myState.excludedTypes.clear();
    myState.excludedTypes.addAll(enableByDefaultTypes);
    myState.excludedTypes.removeAll(myTypes);

    syncConfigurations();
    moveRemovedTypesContent(removed);
    moveAddedTypesContent(added);
    updateDashboard(true);
  }

  private void moveRemovedTypesContent(Set<String> removedTypes) {
    if (removedTypes.isEmpty()) return;

    ExecutionManagerImpl executionManager = (ExecutionManagerImpl)ExecutionManager.getInstance(myProject);
    RunContentManagerImpl runContentManager = (RunContentManagerImpl)executionManager.getContentManager();
    for (RunDashboardService service : getRunConfigurations()) {
      Content content = service.getContent();
      if (content == null || !removedTypes.contains(service.getSettings().getType().getId())) continue;

      RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
      if (descriptor == null) continue;

      Executor executor = RunContentManagerImpl.getExecutorByContent(content);
      if (executor == null) continue;

      descriptor.setContentToolWindowId(null);
      updateContentToolbar(content, true);
      runContentManager.moveContent(executor, descriptor);
    }
  }

  private void moveAddedTypesContent(Set<String> addedTypes) {
    if (addedTypes.isEmpty()) return;

    ExecutionManagerImpl executionManager = (ExecutionManagerImpl)ExecutionManager.getInstance(myProject);
    RunContentManagerImpl runContentManager = (RunContentManagerImpl)executionManager.getContentManager();
    List<RunContentDescriptor> descriptors =
      executionManager.getRunningDescriptors(settings -> addedTypes.contains(settings.getType().getId()));
    for (RunContentDescriptor descriptor : descriptors) {
      Content content = descriptor.getAttachedContent();
      if (content == null) continue;

      Executor executor = RunContentManagerImpl.getExecutorByContent(content);
      if (executor == null) continue;

      descriptor.setContentToolWindowId(getToolWindowId());
      runContentManager.moveContent(executor, descriptor);
    }
  }

  @Override
  @NotNull
  public List<RunDashboardCustomizer> getCustomizers(@NotNull RunnerAndConfigurationSettings settings,
                                                     @Nullable RunContentDescriptor descriptor) {
    List<RunDashboardCustomizer> customizers = ContainerUtil.newSmartList();
    for (RunDashboardCustomizer customizer : CUSTOMIZER_EP_NAME.getExtensions()) {
      if (customizer.isApplicable(settings, descriptor)) {
        customizers.add(customizer);
      }
    }
    return customizers;
  }

  private void updateDashboardIfNeeded(@Nullable RunnerAndConfigurationSettings settings) {
    if (settings != null) {
      updateDashboardIfNeeded(settings.getConfiguration(), true);
    }
  }

  private void updateDashboardIfNeeded(@NotNull RunConfiguration configuration, boolean withStructure) {
    if (isShowInDashboard(configuration) ||
        !filterByContent(ExecutionManagerImpl.getInstance(myProject).getDescriptors(s -> configuration.equals(s.getConfiguration())))
          .isEmpty()) {
      updateDashboard(withStructure);
    }
  }

  @NotNull
  @Override
  public Condition<Content> getReuseCondition() {
    return myReuseCondition;
  }

  private boolean canReuseContent(Content content) {
    RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
    if (descriptor == null) return false;

    ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(myProject);
    Set<RunnerAndConfigurationSettings> descriptorConfigurations = executionManager.getConfigurations(descriptor);
    if (descriptorConfigurations.isEmpty()) return true;

    Set<RunConfiguration> storedConfigurations = new HashSet<>(RunManager.getInstance(myProject).getAllConfigurationsList());

    return descriptorConfigurations.stream().noneMatch(descriptorConfiguration -> {
      RunConfiguration configuration = descriptorConfiguration.getConfiguration();
      return isShowInDashboard(configuration) && storedConfigurations.contains(configuration);
    });
  }

  @Override
  public void updateDashboard(boolean withStructure) {
    myProject.getMessageBus().syncPublisher(ServiceEventListener.TOPIC).handle(
      ServiceEventListener.ServiceEvent.createResetEvent(RunConfigurationsServiceViewContributor.class));

    if (Registry.is("ide.service.view")) return;

    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    if (toolWindowManager == null) return;

    toolWindowManager.invokeLater(() -> {
      if (myProject.isDisposed()) {
        return;
      }

      if (withStructure) {
        boolean available = hasContent();
        ToolWindow toolWindow = toolWindowManager.getToolWindow(getToolWindowId());
        if (toolWindow == null) {
          if (!myTypes.isEmpty() || available) {
            toolWindow = createToolWindow(toolWindowManager, available);
          }
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
      }

      if (myDashboardContent != null) {
        myDashboardContent.updateContent(withStructure);
      }
    });
  }

  private ToolWindow createToolWindow(ToolWindowManager toolWindowManager, boolean available) {
    ToolWindow toolWindow = toolWindowManager.registerToolWindow(getToolWindowId(), true, ToolWindowAnchor.BOTTOM,
                                                                 myProject, true);
    toolWindow.setIcon(getToolWindowIcon());
    toolWindow.setAvailable(available, null);
    createToolWindowContent(toolWindow);
    return toolWindow;
  }

  private boolean hasContent() {
    return !getRunConfigurations().isEmpty();
  }

  private void updateToolWindowContent() {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    if (toolWindowManager == null) return;

    toolWindowManager.invokeLater(() -> {
      if (myProject.isDisposed()) {
        return;
      }

      if (myToolWindowContent == null || myToolWindowContentManager == null ||
          myToolWindowContentManagerListener == null) {
        return;
      }

      boolean containsConfigurationsContent = false;
      for (Content content : myToolWindowContentManager.getContents()) {
        if (myToolWindowContent.equals(content)) {
          containsConfigurationsContent = true;
          break;
        }
      }

      if (myShowConfigurations) {
        if (!containsConfigurationsContent) {
          myToolWindowContentManager.removeContentManagerListener(myToolWindowContentManagerListener);
          myDashboardToToolWindowContents.clear();
          myToolWindowContentManager.removeAllContents(true);
          myToolWindowContentManager.addContent(myToolWindowContent);
          myToolWindowContentManager.addContentManagerListener(myToolWindowContentManagerListener);
        }
        updateToolWindowContentTabHeader(myContentManager.getSelectedContent());
      }
      else {
        if (containsConfigurationsContent) {
          myToolWindowContentManager.removeContentManagerListener(myToolWindowContentManagerListener);
          myToolWindowContentManager.removeContent(myToolWindowContent, false);
          for (Content dashboardContent : myContentManager.getContents()) {
            addToolWindowContent(dashboardContent);
          }
          Content dashboardSelectedContent = myContentManager.getSelectedContent();
          if (dashboardSelectedContent == null && myContentManager.getContentCount() > 0) {
            dashboardSelectedContent = myContentManager.getContent(0);
            if (dashboardSelectedContent != null) {
              myContentManager.setSelectedContent(dashboardSelectedContent);
            }
          }
          Content contentToSelect = myDashboardToToolWindowContents.get(dashboardSelectedContent);
          if (contentToSelect != null) {
            myToolWindowContentManager.setSelectedContent(contentToSelect, true);
          }
          myToolWindowContentManager.addContentManagerListener(myToolWindowContentManagerListener);
        }
      }

      ToolWindow toolWindow = toolWindowManager.getToolWindow(getToolWindowId());
      if (toolWindow instanceof ToolWindowImpl) {
        ToolWindowContentUi contentUi = ((ToolWindowImpl)toolWindow).getContentUI();
        contentUi.revalidate();
        contentUi.repaint();
      }
    });
  }

  private void addToolWindowContent(Content dashboardContent) {
    if (myToolWindowContentManager == null) return;

    Content toolWindowContent =
      ContentFactory.SERVICE.getInstance().createContent(myDashboardContent, dashboardContent.getDisplayName(), false);
    toolWindowContent.setIcon(dashboardContent.getIcon());
    PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        final String property = evt.getPropertyName();
        if (Content.PROP_DISPLAY_NAME.equals(property)) {
          toolWindowContent.setDisplayName(dashboardContent.getDisplayName());
        }
        else if (Content.PROP_ICON.equals(property)) {
          toolWindowContent.setIcon(dashboardContent.getIcon());
        }
      }
    };
    Disposer.register(toolWindowContent, () -> dashboardContent.removePropertyChangeListener(propertyChangeListener));
    dashboardContent.addPropertyChangeListener(propertyChangeListener);
    toolWindowContent.setShouldDisposeContent(false);
    toolWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    toolWindowContent.setHelpId(getToolWindowContextHelpId());
    myToolWindowContentManager.addContent(toolWindowContent);
    myDashboardToToolWindowContents.put(dashboardContent, toolWindowContent);
  }

  private void updateToolWindowContentTabHeader(@Nullable Content content) {
    if (content != null) {
      myToolWindowContent.setDisplayName(content.getDisplayName());
      myToolWindowContent.setIcon(content.getIcon());
      myToolWindowContent.setCloseable(true);
    }
    else {
      myToolWindowContent.setDisplayName(null);
      myToolWindowContent.setIcon(null);
      myToolWindowContent.setCloseable(false);
    }
  }

  private void syncConfigurations() {
    List<RunnerAndConfigurationSettings> settingsList = ContainerUtil
      .filter(RunManager.getInstance(myProject).getAllSettings(),
              settings -> isShowInDashboard(settings.getConfiguration()));
    List<List<RunDashboardServiceImpl>> result = new ArrayList<>();
    myServiceLock.writeLock().lock();
    try {
      for (RunnerAndConfigurationSettings settings : settingsList) {
        List<RunDashboardServiceImpl> syncedServices = getServices(settings);
        if (syncedServices == null) {
          syncedServices = ContainerUtil.newSmartList(new RunDashboardServiceImpl(settings));
        }
        result.add(syncedServices);
      }
      for (List<RunDashboardServiceImpl> settingServices : myServices) {
        RunDashboardService service = settingServices.get(0);
        if (service.getContent() != null && !settingsList.contains(service.getSettings())) {
          result.add(settingServices);
        }
      }
      myServices = result;
    }
    finally {
      myServiceLock.writeLock().unlock();
    }
  }

  private void addServiceContent(@NotNull Content content) {
    RunnerAndConfigurationSettings settings = findSettings(content);
    if (settings == null) return;

    myServiceLock.writeLock().lock();
    try {
      doAddServiceContent(settings, content);
    }
    finally {
      myServiceLock.writeLock().unlock();
    }
  }

  private void removeServiceContent(@NotNull Content content) {
    myServiceLock.writeLock().lock();
    try {
      RunDashboardServiceImpl service = findService(content);
      if (service == null) return;

      doRemoveServiceContent(service);
    }
    finally {
      myServiceLock.writeLock().unlock();
      updateDashboard(true);
    }
  }

  private void updateServiceContent(@NotNull Content content) {
    RunnerAndConfigurationSettings settings = findSettings(content);
    if (settings == null) return;

    myServiceLock.writeLock().lock();
    try {
      RunDashboardServiceImpl service = findService(content);
      if (service == null || service.getSettings().equals(settings)) return;

      doAddServiceContent(settings, content);
      doRemoveServiceContent(service);
    }
    finally {
      myServiceLock.writeLock().unlock();
    }
  }

  private void doAddServiceContent(@NotNull RunnerAndConfigurationSettings settings, @NotNull Content content) {
    List<RunDashboardServiceImpl> settingsServices = getServices(settings);
    if (settingsServices == null) {
      settingsServices = ContainerUtil.newSmartList(new RunDashboardServiceImpl(settings));
      myServices.add(settingsServices);
    }

    RunDashboardServiceImpl service = settingsServices.get(0);
    RunDashboardServiceImpl newService;
    if (service.getContent() == null) {
      newService = service;
    }
    else {
      newService = new RunDashboardServiceImpl(settings);
      settingsServices.add(newService);
    }
    newService.setContent(content);
  }

  private void doRemoveServiceContent(@NotNull RunDashboardServiceImpl service) {
    service.setContent(null);
    RunnerAndConfigurationSettings contentSettings = service.getSettings();
    List<RunDashboardServiceImpl> services = getServices(contentSettings);
    if (services == null) return;

    if (services.size() > 1) {
      services.remove(service);
    }
    else if (!isShowInDashboard(contentSettings.getConfiguration()) ||
             !RunManager.getInstance(myProject).getAllSettings().contains(contentSettings)) {
      myServices.remove(services);
    }
  }

  @Nullable
  private RunDashboardServiceImpl findService(@NotNull Content content) {
    myServiceLock.readLock().lock();
    try {
      for (List<RunDashboardServiceImpl> services : myServices) {
        for (RunDashboardServiceImpl service : services) {
          if (content.equals(service.getContent())) {
            return service;
          }
        }
      }
    }
    finally {
      myServiceLock.readLock().unlock();
      updateDashboard(true);
    }
    return null;
  }

  @Nullable
  private RunnerAndConfigurationSettings findSettings(@NotNull Content content) {
    RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
    Set<RunnerAndConfigurationSettings> settingsSet = ExecutionManagerImpl.getInstance(myProject).getConfigurations(descriptor);
    RunnerAndConfigurationSettings result = ContainerUtil.getFirstItem(settingsSet);
    if (result != null) return result;

    ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();
    return processHandler == null ? null : processHandler.getUserData(RunContentManagerImpl.TEMPORARY_CONFIGURATION_KEY);
  }

  @Nullable
  private List<RunDashboardServiceImpl> getServices(@NotNull RunnerAndConfigurationSettings settings) {
    for (List<RunDashboardServiceImpl> services : myServices) {
      if (services.get(0).getSettings().equals(settings)) {
        return services;
      }
    }
    return null;
  }

  private static void updateContentToolbar(Content content, boolean visible) {
    RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
    RunnerLayoutUiImpl ui = getRunnerLayoutUi(descriptor);
    if (ui != null) {
      ui.setLeftToolbarVisible(visible);
      ui.setContentToolbarBefore(visible);
    }
    else {
      ActionToolbar toolbar = findActionToolbar(descriptor);
      if (toolbar != null) {
        toolbar.getComponent().setVisible(visible);
      }
    }
  }

  void setSelectedContent(@NotNull Content content) {
    ContentManager contentManager = content.getManager();
    if (contentManager == null || content == contentManager.getSelectedContent()) return;

    if (contentManager != myContentManager) {
      contentManager.setSelectedContent(content);
      return;
    }

    myContentManager.removeContentManagerListener(myServiceContentManagerListener);
    myContentManager.setSelectedContent(content);
    updateContentToolbar(content, false);
    myContentManager.addContentManagerListener(myServiceContentManagerListener);
  }

  void removeFromSelection(@NotNull Content content) {
    ContentManager contentManager = content.getManager();
    if (contentManager == null || content != contentManager.getSelectedContent()) return;

    if (contentManager != myContentManager) {
      contentManager.removeFromSelection(content);
      return;
    }

    myContentManager.removeContentManagerListener(myServiceContentManagerListener);
    myContentManager.removeFromSelection(content);
    myContentManager.addContentManagerListener(myServiceContentManagerListener);
  }

  @NotNull
  public RunDashboardStatusFilter getStatusFilter() {
    return myStatusFilter;
  }

  @Nullable
  static RunnerLayoutUiImpl getRunnerLayoutUi(@Nullable RunContentDescriptor descriptor) {
    if (descriptor == null) return null;

    RunnerLayoutUi layoutUi = descriptor.getRunnerLayoutUi();
    return layoutUi instanceof RunnerLayoutUiImpl ? (RunnerLayoutUiImpl)layoutUi : null;
  }

  @Nullable
  static ActionToolbar findActionToolbar(@Nullable RunContentDescriptor descriptor) {
    if (descriptor == null) return null;

    for (Component component : descriptor.getComponent().getComponents()) {
      if (component instanceof ActionToolbar) {
        return ((ActionToolbar)component);
      }
    }
    return null;
  }

  private Set<String> getEnableByDefaultTypes() {
    Set<String> result = new THashSet<>();
    for (RunDashboardDefaultTypesProvider provider : DEFAULT_TYPES_PROVIDER_EP_NAME.getExtensionList()) {
      result.addAll(provider.getDefaultTypeIds(myProject));
    }
    return result;
  }

  @Nullable
  @Override
  public State getState() {
    List<RuleState> ruleStates = myState.ruleStates;
    ruleStates.clear();
    for (RunDashboardGrouper grouper : myGroupers) {
      if (!grouper.getRule().isAlwaysEnabled()) {
        ruleStates.add(new RuleState(grouper.getRule().getName(), grouper.isEnabled()));
      }
    }
    if (myDashboardContent != null) {
      myState.contentProportion = myDashboardContent.getContentProportion();
    }
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState = state;
    myTypes.clear();
    myTypes.addAll(myState.configurationTypes);
    Set<String> enableByDefaultTypes = getEnableByDefaultTypes();
    enableByDefaultTypes.removeAll(myState.excludedTypes);
    myTypes.addAll(enableByDefaultTypes);
    if (!myTypes.isEmpty()) {
      initToolWindowContentListeners();
      syncConfigurations();
    }
    for (RuleState ruleState : state.ruleStates) {
      for (RunDashboardGrouper grouper : myGroupers) {
        if (grouper.getRule().getName().equals(ruleState.name) && !grouper.getRule().isAlwaysEnabled()) {
          grouper.setEnabled(ruleState.enabled);
          break;
        }
      }
    }
  }

  @Override
  public void noStateLoaded() {
    myTypes.clear();
    myTypes.addAll(getEnableByDefaultTypes());
    if (!myTypes.isEmpty()) {
      initToolWindowContentListeners();
      syncConfigurations();
    }
  }

  static class State {
    public final Set<String> configurationTypes = new THashSet<>();
    public final Set<String> excludedTypes = new THashSet<>();
    public final List<RuleState> ruleStates = new ArrayList<>();
    public float contentProportion = DEFAULT_CONTENT_PROPORTION;
  }

  private static class RuleState {
    public String name;
    public boolean enabled = true;

    @SuppressWarnings("UnusedDeclaration")
    RuleState() {
    }

    RuleState(String name, boolean enabled) {
      this.name = name;
      this.enabled = enabled;
    }
  }

  private static class RunDashboardServiceImpl implements RunDashboardService {
    private final RunnerAndConfigurationSettings mySettings;
    private volatile Content myContent;

    RunDashboardServiceImpl(@NotNull RunnerAndConfigurationSettings settings) {
      mySettings = settings;
    }

    @NotNull
    @Override
    public RunnerAndConfigurationSettings getSettings() {
      return mySettings;
    }

    @Nullable
    @Override
    public RunContentDescriptor getDescriptor() {
      Content content = myContent;
      return content == null ? null : RunContentManagerImpl.getRunContentDescriptorByContent(content);
    }

    @Nullable
    @Override
    public Content getContent() {
      return myContent;
    }

    void setContent(@Nullable Content content) {
      myContent = content;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      RunDashboardServiceImpl service = (RunDashboardServiceImpl)o;
      return mySettings.equals(service.mySettings) && Comparing.equal(myContent, service.myContent);
    }

    @Override
    public int hashCode() {
      int result = mySettings.hashCode();
      result = 31 * result + (myContent != null ? myContent.hashCode() : 0);
      return result;
    }
  }

  private class ServiceContentManagerListener extends ContentManagerAdapter {
    @Override
    public void selectionChanged(@NotNull ContentManagerEvent event) {
      boolean onAdd = event.getOperation() == ContentManagerEvent.ContentOperation.add;
      Content content = event.getContent();
      if (onAdd) {
        updateContentToolbar(content, false);
        updateServiceContent(content);
      }

      updateToolWindowContent();
      updateDashboard(true);

      if (Registry.is("ide.service.view") && onAdd) {
        RunnerAndConfigurationSettings settings = findSettings(content);
        if (settings != null) {
          RunDashboardServiceImpl service = new RunDashboardServiceImpl(settings);
          service.setContent(content);
          RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
          RunConfigurationNode node = new RunConfigurationNode(myProject, service, getCustomizers(settings, descriptor));
          ServiceViewManager.getInstance(myProject).select(node, RunConfigurationsServiceViewContributor.class, true, false);
        }
      }
    }

    @Override
    public void contentAdded(@NotNull ContentManagerEvent event) {
      addServiceContent(event.getContent());
    }

    @Override
    public void contentRemoved(@NotNull ContentManagerEvent event) {
      if (myContentManager.getContentCount() == 0 && !isShowConfigurations()) {
        setShowConfigurations(true);
      }
      removeServiceContent(event.getContent());
    }
  }

  private class DashboardContentManagerListener extends ContentManagerAdapter {
    @Override
    public void contentAdded(@NotNull ContentManagerEvent event) {
      if (myShowConfigurations || myToolWindowContentManager == null) return;

      Content toolWindowContent = myDashboardToToolWindowContents.get(event.getContent());
      if (toolWindowContent == null) {
        addToolWindowContent(event.getContent());
      }
      else {
        if (!myToolWindowContentManager.isSelected(toolWindowContent)) {
          myToolWindowContentManager.setSelectedContent(toolWindowContent);
        }
      }
    }

    @Override
    public void contentRemoved(@NotNull ContentManagerEvent event) {
      if (myShowConfigurations || myToolWindowContentManager == null) return;

      Content toolWindowContent = myDashboardToToolWindowContents.remove(event.getContent());
      if (toolWindowContent != null && toolWindowContent.getManager() != null) {
        myToolWindowContentManager.removeContentManagerListener(myToolWindowContentManagerListener);
        myToolWindowContentManager.removeContent(toolWindowContent, true);
        myToolWindowContentManager.addContentManagerListener(myToolWindowContentManagerListener);
      }
    }

    @Override
    public void selectionChanged(@NotNull ContentManagerEvent event) {
      if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
        contentAdded(event);
      }

      if (myToolWindowContentManager == null || myToolWindowContent == null || !myShowConfigurations) return;

      Content content = event.getOperation() == ContentManagerEvent.ContentOperation.add ? event.getContent() : null;
      updateToolWindowContentTabHeader(content);
    }
  }

  private class ToolWindowContentManagerListener extends ContentManagerAdapter {
    @Override
    public void contentRemoveQuery(@NotNull ContentManagerEvent event) {
      if (event.getContent().equals(myToolWindowContent)) {
        Content content = myContentManager.getSelectedContent();
        if (content != null) {
          myContentManager.removeContent(content, true);
        }
        event.consume();
        return;
      }

      Content dashboardContent = getDashboardContent(event.getContent());
      if (dashboardContent == null || dashboardContent.getManager() == null) return;

      myDashboardToToolWindowContents.remove(dashboardContent);
      if (!myContentManager.removeContent(dashboardContent, true)) {
        event.consume();
        myDashboardToToolWindowContents.put(dashboardContent, event.getContent());
      }
    }

    @Override
    public void selectionChanged(@NotNull ContentManagerEvent event) {
      if (event.getContent().equals(myToolWindowContent)) return;

      if (event.getOperation() != ContentManagerEvent.ContentOperation.add) return;

      Content dashboardContent = getDashboardContent(event.getContent());
      if (dashboardContent == null || dashboardContent.getManager() == null || myContentManager.isSelected(dashboardContent)) return;

      myContentManager.removeContentManagerListener(myDashboardContentManagerListener);
      myContentManager.setSelectedContent(dashboardContent);
      myContentManager.addContentManagerListener(myDashboardContentManagerListener);
    }

    private Content getDashboardContent(Content content) {
      for (Map.Entry<Content, Content> entry : myDashboardToToolWindowContents.entrySet()) {
        if (entry.getValue().equals(content)) {
          return entry.getKey();
        }
      }
      return null;
    }
  }
}
