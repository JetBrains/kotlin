// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.openapi.project.ExternalStorageConfigurationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.config.DelegatingGradleSettingsListenerAdapter;
import org.jetbrains.plugins.gradle.service.settings.GradleSettingsService;

import java.util.Collection;
import java.util.Set;

/**
 * Holds shared project-level gradle-related settings (should be kept at the '*.ipr' or under '.idea').
 *
 * @author peter
 */
@State(name = "GradleSettings", storages = @Storage("gradle.xml"))
public class GradleSettings extends AbstractExternalSystemSettings<GradleSettings, GradleProjectSettings, GradleSettingsListener>
  implements PersistentStateComponent<GradleSettings.MyState> {

  private final GradleSystemSettings mySystemSettings;

  public GradleSettings(@NotNull Project project) {
    super(GradleSettingsListener.TOPIC, project);
    mySystemSettings = GradleSystemSettings.getInstance();
  }

  @NotNull
  public static GradleSettings getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, GradleSettings.class);
  }

  @Override
  public void subscribe(@NotNull ExternalSystemSettingsListener<GradleProjectSettings> listener) {
    getProject().getMessageBus().connect(getProject()).subscribe(GradleSettingsListener.TOPIC,
                                                                 new DelegatingGradleSettingsListenerAdapter(listener));
  }

  @Override
  protected void copyExtraSettingsFrom(@NotNull GradleSettings settings) {
  }

  @Nullable
  @Override
  public GradleSettings.MyState getState() {
    MyState state = new MyState();
    fillState(state);
    return state;
  }

  @Override
  public void loadState(@NotNull MyState state) {
    super.loadState(state);
  }

  /**
   * @return service directory path (if defined). 'Service directory' is a directory which is used internally by gradle during
   * calls to the tooling api. E.g. it holds downloaded binaries (dependency jars). We allow to define it because there
   * is a possible situation when a user wants to configure particular directory to be excluded from anti-virus protection
   * in order to increase performance
   */
  @Nullable
  public String getServiceDirectoryPath() {
    return mySystemSettings.getServiceDirectoryPath();
  }

  public void setServiceDirectoryPath(@Nullable String newPath) {
    String myServiceDirectoryPath = mySystemSettings.getServiceDirectoryPath();
    if (!Comparing.equal(myServiceDirectoryPath, newPath)) {
      mySystemSettings.setServiceDirectoryPath(newPath);
      getPublisher().onServiceDirectoryPathChange(myServiceDirectoryPath, newPath);
    }
  }

  @Nullable
  public String getGradleVmOptions() {
    return mySystemSettings.getGradleVmOptions();
  }

  public void setGradleVmOptions(@Nullable String gradleVmOptions) {
    String myGradleVmOptions = mySystemSettings.getGradleVmOptions();
    if (!Comparing.equal(myGradleVmOptions, gradleVmOptions)) {
      mySystemSettings.setGradleVmOptions(gradleVmOptions);
      getPublisher().onGradleVmOptionsChange(myGradleVmOptions, gradleVmOptions);
    }
  }

  public boolean isOfflineWork() {
    return mySystemSettings.isOfflineWork();
  }

  public void setOfflineWork(boolean isOfflineWork) {
    mySystemSettings.setOfflineWork(isOfflineWork);
  }

  @Override
  protected void checkSettings(@NotNull GradleProjectSettings old, @NotNull GradleProjectSettings current) {
    if (!Comparing.equal(old.getGradleHome(), current.getGradleHome())) {
      getPublisher().onGradleHomeChange(old.getGradleHome(), current.getGradleHome(), current.getExternalProjectPath());
    }
    if (old.getDistributionType() != current.getDistributionType()) {
      getPublisher().onGradleDistributionTypeChange(current.getDistributionType(), current.getExternalProjectPath());
    }
    if (old.isResolveModulePerSourceSet() != current.isResolveModulePerSourceSet()) {
      ExternalProjectsManager.getInstance(getProject()).getExternalProjectsWatcher().markDirty(current.getExternalProjectPath());
    }
    ThreeState storeProjectFilesExternally = current.getStoreProjectFilesExternally();
    if (old.getStoreProjectFilesExternally() != storeProjectFilesExternally) {
      ExternalProjectsManagerImpl.getInstance(getProject()).setStoreExternally(storeProjectFilesExternally != ThreeState.NO);
    }
    if (!Comparing.equal(old.getDelegatedBuild(), current.getDelegatedBuild())) {
      boolean delegatedBuild = GradleSettingsService.getInstance(getProject()).isDelegatedBuildEnabled(current.getExternalProjectPath());
      getPublisher().onBuildDelegationChange(delegatedBuild, current.getExternalProjectPath());
    }
    if (!Comparing.equal(old.getTestRunner(), current.getTestRunner())) {
      TestRunner testRunner = GradleSettingsService.getInstance(getProject()).getTestRunner(current.getExternalProjectPath());
      getPublisher().onTestRunnerChange(testRunner, current.getExternalProjectPath());
    }
  }

  @NotNull
  @Override
  public Collection<GradleProjectSettings> getLinkedProjectsSettings() {
    Collection<GradleProjectSettings> settings = super.getLinkedProjectsSettings();
    boolean isStoredExternally = ExternalStorageConfigurationManager.getInstance(getProject()).isEnabled();
    // GradleProjectSettings has transient field isStoredExternally - used when no project yet,
    // but when project created, isStoredExternally stored in the ExternalProjectsManagerImpl and we need to transfer it
    for (GradleProjectSettings setting : settings) {
      setting.setStoreProjectFilesExternally(ThreeState.fromBoolean(isStoredExternally));
    }
    return settings;
  }

  public static class MyState implements State<GradleProjectSettings> {
    private final Set<GradleProjectSettings> myProjectSettings = ContainerUtilRt.newTreeSet();

    @Override
    @XCollection(elementTypes = {GradleProjectSettings.class})
    public Set<GradleProjectSettings> getLinkedExternalProjectsSettings() {
      return myProjectSettings;
    }

    @Override
    public void setLinkedExternalProjectsSettings(Set<GradleProjectSettings> settings) {
      if (settings != null) {
        myProjectSettings.addAll(settings);
      }
    }
  }
}