// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.Converter;
import com.intellij.util.xmlb.annotations.*;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.BuildParticipant;
import org.jetbrains.plugins.gradle.service.GradleInstallationManager;
import org.jetbrains.plugins.gradle.service.settings.GradleSettingsService;

import java.util.List;
import java.util.Optional;

/**
 * {@link GradleProjectSettings} holds settings for the linked gradle project.
 * These settings might have IDE project level defaults - {@link DefaultGradleProjectSettings}.
 * Consider to use effective settings with {@link GradleSettingsService}.
 *
 * @see GradleSettingsService
 * @see DefaultGradleProjectSettings
 *
 * @author Denis Zhdanov
 */
public class GradleProjectSettings extends ExternalProjectSettings {
  @Nullable private String myGradleHome;
  @Nullable private String myGradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK;
  @Nullable private DistributionType distributionType;
  private boolean disableWrapperSourceDistributionNotification;
  private boolean resolveModulePerSourceSet = ExternalSystemApiUtil.isJavaCompatibleIde();
  private boolean resolveExternalAnnotations;
  @Nullable private CompositeBuild myCompositeBuild;

  private ThreeState storeProjectFilesExternally = ThreeState.NO;

  @NotNull
  private ThreeState delegatedBuild = ThreeState.UNSURE;
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
    return result;
  }

  @Transient
  public ThreeState getStoreProjectFilesExternally() {
    return storeProjectFilesExternally;
  }

  public void setStoreProjectFilesExternally(@NotNull ThreeState value) {
    storeProjectFilesExternally = value;
  }

  /**
   * Build/run mode for the gradle project.
   * Consider to use effective settings using {@link GradleSettingsService#isDelegatedBuildEnabled(Module)}
   * @return build/run mode, {@link ThreeState#UNSURE} means using IDE project level configuration, see {@link DefaultGradleProjectSettings#isDelegatedBuild()}
   */
  @OptionTag(value = "delegatedBuild", converter = ThreeStateConverter.class)
  @NotNull
  public ThreeState getDelegatedBuild() {
    return delegatedBuild;
  }

  /**
   * @param state {@link ThreeState#UNSURE} means using IDE project level configuration, see {@link DefaultGradleProjectSettings#isDelegatedBuild()}
   */
  public void setDelegatedBuild(@NotNull ThreeState state) {
    this.delegatedBuild = state;
  }

  /**
   * Test runner option.
   * Consider to use effective settings using {@link GradleSettingsService#getTestRunner(Module)}
   * @return test runner option, "null" means using IDE project level configuration, see {@link DefaultGradleProjectSettings#getTestRunner()}
   */
  @Nullable
  public TestRunner getTestRunner() {
    return testRunner;
  }

  /**
   * @param testRunner null means using IDE project level configuration, see {@link DefaultGradleProjectSettings#getTestRunner()}
   */
  public void setTestRunner(@Nullable TestRunner testRunner) {
    this.testRunner = testRunner;
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

    public void setCompositeParticipants(List<BuildParticipant> compositeParticipants) {
      myCompositeParticipants = compositeParticipants == null ? new SmartList<>() : ContainerUtil.newArrayList(compositeParticipants);
    }

    @NotNull
    public CompositeBuild copy() {
      CompositeBuild result = new CompositeBuild();
      result.myCompositeParticipants = ContainerUtil.newArrayList();
      for (BuildParticipant participant : myCompositeParticipants) {
        result.myCompositeParticipants.add(participant.copy());
      }
      result.myCompositeDefinitionSource = myCompositeDefinitionSource;
      return result;
    }
  }

  private static final class ThreeStateConverter extends Converter<ThreeState> {
    @Nullable
    @Override
    public ThreeState fromString(@NotNull String value) {
      if (StringUtil.isEmpty(value)) return ThreeState.UNSURE;
      return ThreeState.fromBoolean(Boolean.valueOf(value));
    }

    @Nullable
    @Override
    public String toString(@NotNull ThreeState value) {
      if (value == ThreeState.UNSURE) return null;
      return String.valueOf(value.toBoolean());
    }
  }
}
