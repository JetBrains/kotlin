// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard;

import com.google.common.collect.Sets;
import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.tree.RunConfigurationNode;
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
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.ui.content.*;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@State(
  name = "RunDashboard",
  storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public class RunDashboardManagerImpl implements RunDashboardManager, PersistentStateComponent<RunDashboardManagerImpl.State> {
  private static final ExtensionPointName<RunDashboardCustomizer> CUSTOMIZER_EP_NAME =
    ExtensionPointName.create("com.intellij.runDashboardCustomizer");
  private static final ExtensionPointName<RunDashboardDefaultTypesProvider> DEFAULT_TYPES_PROVIDER_EP_NAME =
    ExtensionPointName.create("com.intellij.runDashboardDefaultTypesProvider");

  private final Project myProject;
  private final ContentManager myContentManager;
  private final ContentManagerListener myServiceContentManagerListener;
  private State myState = new State();
  private final Set<String> myTypes = new THashSet<>();
  private final Set<RunConfiguration> myHiddenConfigurations = new THashSet<>();
  private volatile List<List<RunDashboardServiceImpl>> myServices = Collections.emptyList();
  private final ReentrantReadWriteLock myServiceLock = new ReentrantReadWriteLock();
  private final RunDashboardStatusFilter myStatusFilter = new RunDashboardStatusFilter();
  private String myToolWindowId;
  private final Condition<Content> myReuseCondition;
  private final AtomicBoolean myListenersInitialized = new AtomicBoolean();

  public RunDashboardManagerImpl(@NotNull Project project) {
    myProject = project;
    ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    myContentManager = contentFactory.createContentManager(new PanelContentUI(), false, project);
    myServiceContentManagerListener = new ServiceContentManagerListener();
    myReuseCondition = this::canReuseContent;
  }

  private void initServiceContentListeners() {
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
        myHiddenConfigurations.remove(settings.getConfiguration());
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
        updateDashboardIfNeeded(env.getRunnerAndConfigurationSettings());
      }

      @Override
      public void processTerminated(@NotNull String executorId,
                                    @NotNull ExecutionEnvironment env,
                                    @NotNull ProcessHandler handler,
                                    int exitCode) {
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
  @NotNull
  public String getToolWindowId() {
    if (myToolWindowId == null) {
      String toolWindowId =
        ((ServiceViewManagerImpl)ServiceViewManager.getInstance(myProject))
          .getToolWindowId(RunDashboardServiceViewContributor.class);
      myToolWindowId = toolWindowId != null ? toolWindowId : ToolWindowId.SERVICES;
    }
    return myToolWindowId;
  }

  @Override
  @NotNull
  public Icon getToolWindowIcon() {
    return AllIcons.Toolwindows.ToolWindowServices;
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
  public boolean isShowInDashboard(@NotNull RunConfiguration runConfiguration) {
    return myTypes.contains(runConfiguration.getType().getId()) && !myHiddenConfigurations.contains(runConfiguration);
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
      initServiceContentListeners();
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

  public Set<RunConfiguration> getHiddenConfigurations() {
    return Collections.unmodifiableSet(myHiddenConfigurations);
  }

  public void hideConfigurations(Collection<RunConfiguration> configurations) {
    myHiddenConfigurations.addAll(configurations);
    syncConfigurations();
    updateDashboard(true);
  }

  public void restoreConfigurations(Collection<RunConfiguration> configurations) {
    myHiddenConfigurations.removeAll(configurations);
    syncConfigurations();
    updateDashboard(true);
  }

  @NotNull
  List<RunDashboardCustomizer> getCustomizers(@NotNull RunnerAndConfigurationSettings settings,
                                              @Nullable RunContentDescriptor descriptor) {
    List<RunDashboardCustomizer> customizers = new SmartList<>();
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
      ServiceEventListener.ServiceEvent.createResetEvent(RunDashboardServiceViewContributor.class));
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
          syncedServices = new SmartList<>(new RunDashboardServiceImpl(settings));
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
      settingsServices = new SmartList<>(new RunDashboardServiceImpl(settings));
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

  Set<String> getEnableByDefaultTypes() {
    Set<String> result = new THashSet<>();
    for (RunDashboardDefaultTypesProvider provider : DEFAULT_TYPES_PROVIDER_EP_NAME.getExtensionList()) {
      result.addAll(provider.getDefaultTypeIds(myProject));
    }
    return result;
  }

  @Nullable
  @Override
  public State getState() {
    myState.hiddenConfigurations.clear();
    for (RunConfiguration configuration : myHiddenConfigurations) {
      ConfigurationType type = configuration.getType();
      if (myTypes.contains(type.getId())) {
        Set<String> configurations = myState.hiddenConfigurations.get(type.getId());
        if (configurations == null) {
          configurations = new THashSet<>();
          myState.hiddenConfigurations.put(type.getId(), configurations);
        }
        configurations.add(configuration.getName());
      }
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
      loadHiddenConfigurations();
      initServiceContentListeners();
      syncConfigurations();
    }
  }

  private void loadHiddenConfigurations() {
    for (Map.Entry<String, Set<String>> entry : myState.hiddenConfigurations.entrySet()) {
      ConfigurationType type = ConfigurationTypeUtil.findConfigurationType(entry.getKey());
      if (type == null) continue;

      List<RunConfiguration> configurations = RunManager.getInstance(myProject).getConfigurationsList(type);
      for (String name : entry.getValue()) {
        for (RunConfiguration configuration : configurations) {
          if (configuration.getName().equals(name)) {
            myHiddenConfigurations.add(configuration);
          }
        }
      }
    }
  }

  @Override
  public void noStateLoaded() {
    myTypes.clear();
    myTypes.addAll(getEnableByDefaultTypes());
    if (!myTypes.isEmpty()) {
      initServiceContentListeners();
      syncConfigurations();
    }
  }

  static class State {
    public final Set<String> configurationTypes = new THashSet<>();
    public final Set<String> excludedTypes = new THashSet<>();
    public final Map<String, Set<String>> hiddenConfigurations = new THashMap<>();
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

      updateDashboard(true);

      if (onAdd) {
        RunnerAndConfigurationSettings settings = findSettings(content);
        if (settings != null) {
          RunDashboardServiceImpl service = new RunDashboardServiceImpl(settings);
          service.setContent(content);
          RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
          RunConfigurationNode node = new RunConfigurationNode(myProject, service, getCustomizers(settings, descriptor));
          ServiceViewManager.getInstance(myProject).select(node, RunDashboardServiceViewContributor.class, true, false);
        }
      }
    }

    @Override
    public void contentAdded(@NotNull ContentManagerEvent event) {
      addServiceContent(event.getContent());
    }

    @Override
    public void contentRemoved(@NotNull ContentManagerEvent event) {
      removeServiceContent(event.getContent());
    }
  }
}
