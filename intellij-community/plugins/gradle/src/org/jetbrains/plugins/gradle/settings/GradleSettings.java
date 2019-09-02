// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.openapi.project.ExternalStorageConfigurationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.config.DelegatingGradleSettingsListenerAdapter;

import java.util.Set;
import java.util.TreeSet;

/**
 * Holds shared project-level gradle-related settings (should be kept at the '*.ipr' or under '.idea').
 *
 * @author peter
 */
@State(name = "GradleSettings", storages = @Storage("gradle.xml"))
public class GradleSettings extends AbstractExternalSystemSettings<GradleSettings, GradleProjectSettings, GradleSettingsListener>
  implements PersistentStateComponent<GradleSettings.MyState> {
  public GradleSettings(@NotNull Project project) {
    super(GradleSettingsListener.TOPIC, project);
  }

  @NotNull
  public static GradleSettings getInstance(@NotNull Project project) {
    return project.getService(GradleSettings.class);
  }

  @Override
  public void subscribe(@NotNull ExternalSystemSettingsListener<GradleProjectSettings> listener) {
    doSubscribe(new DelegatingGradleSettingsListenerAdapter(listener), getProject());
  }

  @Override
  public void subscribe(@NotNull ExternalSystemSettingsListener<GradleProjectSettings> listener, @NotNull Disposable parentDisposable) {
    doSubscribe(new DelegatingGradleSettingsListenerAdapter(listener), parentDisposable);
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

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    GradleSettingsMigration migration = getProject().getService(GradleSettingsMigration.class);

    // When we are opening pre 2019.2 project, we need to import project defaults from the workspace
    // The migration flag is saved to a separate component to preserve backward and forward compatibility.
    if (migration.getMigrationVersion() < 1) {
      migration.setMigrationVersion(1);

      GradleSettingsMigration.LegacyDefaultGradleProjectSettings.MyState legacyProjectDefaults
        = getProject().getService(GradleSettingsMigration.LegacyDefaultGradleProjectSettings.class).getState();
      if (legacyProjectDefaults != null) {
        for (GradleProjectSettings each : getLinkedProjectsSettings()) {
          if (each.getDirectDelegatedBuild() == null) each.setDelegatedBuild(legacyProjectDefaults.delegatedBuild);
          if (each.getDirectTestRunner() == null) each.setTestRunner(legacyProjectDefaults.testRunner);
        }
      }
    }
  }

  /**
   * @return service directory path (if defined). 'Service directory' is a directory which is used internally by gradle during
   * calls to the tooling api. E.g. it holds downloaded binaries (dependency jars). We allow to define it because there
   * is a possible situation when a user wants to configure particular directory to be excluded from anti-virus protection
   * in order to increase performance
   */
  @Nullable
  public String getServiceDirectoryPath() {
    return GradleSystemSettings.getInstance().getServiceDirectoryPath();
  }

  public void setServiceDirectoryPath(@Nullable String newPath) {
    String myServiceDirectoryPath = GradleSystemSettings.getInstance().getServiceDirectoryPath();
    if (!Comparing.equal(myServiceDirectoryPath, newPath)) {
      GradleSystemSettings.getInstance().setServiceDirectoryPath(newPath);
      getPublisher().onServiceDirectoryPathChange(myServiceDirectoryPath, newPath);
    }
  }

  @Nullable
  public String getGradleVmOptions() {
    return GradleSystemSettings.getInstance().getGradleVmOptions();
  }

  public void setGradleVmOptions(@Nullable String gradleVmOptions) {
    String myGradleVmOptions = GradleSystemSettings.getInstance().getGradleVmOptions();
    if (!Comparing.equal(myGradleVmOptions, gradleVmOptions)) {
      GradleSystemSettings.getInstance().setGradleVmOptions(gradleVmOptions);
      getPublisher().onGradleVmOptionsChange(myGradleVmOptions, gradleVmOptions);
    }
  }

  public boolean isOfflineWork() {
    return GradleSystemSettings.getInstance().isOfflineWork();
  }

  public void setOfflineWork(boolean isOfflineWork) {
    GradleSystemSettings.getInstance().setOfflineWork(isOfflineWork);
  }

  public boolean getStoreProjectFilesExternally() {
    return ExternalStorageConfigurationManager.getInstance(getProject()).isEnabled();
  }

  public void setStoreProjectFilesExternally(boolean value) {
    ExternalProjectsManagerImpl.getInstance(getProject()).setStoreExternally(value);
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
    if (!Comparing.equal(old.getDelegatedBuild(), current.getDelegatedBuild())) {
      boolean delegatedBuild = GradleProjectSettings.isDelegatedBuildEnabled(getProject(), current.getExternalProjectPath());
      getPublisher().onBuildDelegationChange(delegatedBuild, current.getExternalProjectPath());
    }
    if (!Comparing.equal(old.getTestRunner(), current.getTestRunner())) {
      TestRunner testRunner = GradleProjectSettings.getTestRunner(getProject(), current.getExternalProjectPath());
      getPublisher().onTestRunnerChange(testRunner, current.getExternalProjectPath());
    }
  }

  public static class MyState implements State<GradleProjectSettings> {
    private final Set<GradleProjectSettings> myProjectSettings = new TreeSet<>();

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