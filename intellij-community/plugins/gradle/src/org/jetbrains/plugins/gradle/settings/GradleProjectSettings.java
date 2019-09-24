// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import com.intellij.util.ThreeState;
import com.intellij.util.xmlb.annotations.*;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.BuildParticipant;
import org.jetbrains.plugins.gradle.service.GradleInstallationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link GradleProjectSettings} holds settings for the linked gradle project.
 *
 * @author Denis Zhdanov
 */
public class GradleProjectSettings extends ExternalProjectSettings {
  private static final Logger LOG = Logger.getInstance(GradleProjectSettings.class);

  public static final boolean DEFAULT_DELEGATE = true;
  public static final TestRunner DEFAULT_TEST_RUNNER = TestRunner.GRADLE;

  @Nullable private String myGradleHome;
  @Nullable private String myGradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK;
  @Nullable private DistributionType distributionType;
  private boolean disableWrapperSourceDistributionNotification;
  private boolean resolveModulePerSourceSet = true;
  private boolean resolveExternalAnnotations = true;
  @Nullable private CompositeBuild myCompositeBuild;

  @Nullable
  private Boolean delegatedBuild;
  @Nullable
  private TestRunner testRunner;

  @Nullable
  public String getGradleHome() {
    return myGradleHome;
  }

  public void setGradleHome(@Nullable String gradleHome) {
    myGradleHome = gradleHome;
  }

  @Nullable
  public String getGradleJvm() {
    return myGradleJvm;
  }

  public void setGradleJvm(@Nullable String gradleJvm) {
    myGradleJvm = gradleJvm;
  }

  @Nullable
  public DistributionType getDistributionType() {
    return distributionType;
  }

  public void setDistributionType(@Nullable DistributionType distributionType) {
    this.distributionType = distributionType;
  }

  public boolean isDisableWrapperSourceDistributionNotification() {
    return disableWrapperSourceDistributionNotification;
  }

  public void setDisableWrapperSourceDistributionNotification(boolean disableWrapperSourceDistributionNotification) {
    this.disableWrapperSourceDistributionNotification = disableWrapperSourceDistributionNotification;
  }

  public boolean isResolveModulePerSourceSet() {
    return resolveModulePerSourceSet;
  }

  public void setResolveModulePerSourceSet(boolean useIdeModulePerSourceSet) {
    this.resolveModulePerSourceSet = useIdeModulePerSourceSet;
  }

  public boolean isResolveExternalAnnotations() {
    return resolveExternalAnnotations;
  }

  public void setResolveExternalAnnotations(boolean resolveExternalAnnotations) {
    this.resolveExternalAnnotations = resolveExternalAnnotations;
  }

  @OptionTag(tag = "compositeConfiguration", nameAttribute = "")
  @Nullable
  public CompositeBuild getCompositeBuild() {
    return myCompositeBuild;
  }

  public void setCompositeBuild(@Nullable CompositeBuild compositeBuild) {
    myCompositeBuild = compositeBuild;
  }

  @NotNull
  @Override
  public GradleProjectSettings clone() {
    GradleProjectSettings result = new GradleProjectSettings();
    copyTo(result);
    result.myGradleHome = myGradleHome;
    result.myGradleJvm = myGradleJvm;
    result.distributionType = distributionType;
    result.disableWrapperSourceDistributionNotification = disableWrapperSourceDistributionNotification;
    result.resolveModulePerSourceSet = resolveModulePerSourceSet;
    result.resolveExternalAnnotations = resolveExternalAnnotations;
    result.myCompositeBuild = myCompositeBuild != null ? myCompositeBuild.copy() : null;

    result.delegatedBuild = delegatedBuild;
    result.testRunner = testRunner;
    return result;
  }

  /**
   * @deprecated use {@link GradleSettings#getStoreProjectFilesExternally}
   */
  @SuppressWarnings("unused")
  @Deprecated
  public ThreeState getStoreProjectFilesExternally() {
    return ThreeState.UNSURE;
  }

  /**
   * @deprecated use {@link GradleSettings#setStoreProjectFilesExternally(boolean)}
   */
  @SuppressWarnings("unused")
  @Deprecated
  public void setStoreProjectFilesExternally(@NotNull ThreeState value) {
  }

  /**
   * @return Build/run mode for the gradle project
   */
  @Transient()
  public boolean getDelegatedBuild() {
    return ObjectUtils.notNull(delegatedBuild, DEFAULT_DELEGATE);
  }

  public void setDelegatedBuild(@NotNull Boolean state) {
    this.delegatedBuild = state;
  }

  // For backward compatibility
  @Nullable
  @OptionTag(value = "delegatedBuild")
  public Boolean getDirectDelegatedBuild() {
    return delegatedBuild;
  }

  @SuppressWarnings("unused")
  public void setDirectDelegatedBuild(@Nullable Boolean state) {
    this.delegatedBuild = state;
  }

  public static boolean isDelegatedBuildEnabled(@NotNull Project project, @Nullable String gradleProjectPath) {
    GradleProjectSettings projectSettings = gradleProjectPath == null
                                            ? null : GradleSettings.getInstance(project).getLinkedProjectSettings(gradleProjectPath);
    if (projectSettings == null) return false;

    return projectSettings.getDelegatedBuild();
  }

  public static boolean isDelegatedBuildEnabled(@NotNull Module module) {
    return isDelegatedBuildEnabled(module.getProject(), ExternalSystemApiUtil.getExternalRootProjectPath(module));
  }

  /**
   * @return test runner option.
   */
  @NotNull
  @Transient()
  public TestRunner getTestRunner() {
    return ObjectUtils.notNull(testRunner, DEFAULT_TEST_RUNNER);
  }

  public void setTestRunner(@NotNull TestRunner testRunner) {
    if (LOG.isDebugEnabled()) {
      if (testRunner != TestRunner.GRADLE) {
        LOG.debug(String.format("Gradle test runner sets to %s", testRunner), new Throwable());
      }
    }
    this.testRunner = testRunner;
  }

  // For backward compatibility
  @Nullable
  @OptionTag(value = "testRunner")
  public TestRunner getDirectTestRunner() {
    return testRunner;
  }

  @SuppressWarnings("unused")
  public void setDirectTestRunner(@Nullable TestRunner testRunner) {
    this.testRunner = testRunner;
  }

  @NotNull
  public static TestRunner getTestRunner(@NotNull Project project, @Nullable String gradleProjectPath) {
    GradleProjectSettings projectSettings = gradleProjectPath == null
                                            ? null : GradleSettings.getInstance(project).getLinkedProjectSettings(gradleProjectPath);
    TestRunner testRunner = projectSettings == null ? TestRunner.PLATFORM : projectSettings.getTestRunner();
    if (LOG.isDebugEnabled()) {
      if (testRunner != TestRunner.GRADLE) {
        String settingsPresentation = projectSettings == null ? String.format("<null: %s>", gradleProjectPath) : gradleProjectPath;
        LOG.debug(String.format("Get non gradle test runner %s at '%s'", testRunner, settingsPresentation), new Throwable());
      }
    }
    return testRunner;
  }

  @NotNull
  public static TestRunner getTestRunner(@NotNull Module module) {
    return getTestRunner(module.getProject(), ExternalSystemApiUtil.getExternalRootProjectPath(module));
  }

  @NotNull
  public GradleVersion resolveGradleVersion() {
    GradleVersion version = GradleInstallationManager.getGradleVersion(this);
    return Optional.ofNullable(version).orElseGet(GradleVersion::current);
  }

  public GradleProjectSettings withQualifiedModuleNames() {
    setUseQualifiedModuleNames(true);
    return this;
  }

  @Tag("compositeBuild")
  public static class CompositeBuild {
    @Nullable private CompositeDefinitionSource myCompositeDefinitionSource;
    private List<BuildParticipant> myCompositeParticipants = new SmartList<>();

    @Attribute
    @Nullable
    public CompositeDefinitionSource getCompositeDefinitionSource() {
      return myCompositeDefinitionSource;
    }

    public void setCompositeDefinitionSource(@Nullable CompositeDefinitionSource compositeDefinitionSource) {
      myCompositeDefinitionSource = compositeDefinitionSource;
    }

    @XCollection(propertyElementName = "builds", elementName = "build")
    @NotNull
    public List<BuildParticipant> getCompositeParticipants() {
      return myCompositeParticipants;
    }

    public void setCompositeParticipants(List<? extends BuildParticipant> compositeParticipants) {
      myCompositeParticipants = compositeParticipants == null ? new SmartList<>() : new ArrayList<>(compositeParticipants);
    }

    @NotNull
    public CompositeBuild copy() {
      CompositeBuild result = new CompositeBuild();
      result.myCompositeParticipants = new ArrayList<>();
      for (BuildParticipant participant : myCompositeParticipants) {
        result.myCompositeParticipants.add(participant.copy());
      }
      result.myCompositeDefinitionSource = myCompositeDefinitionSource;
      return result;
    }
  }
}
