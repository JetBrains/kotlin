/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverExtension;

import java.util.List;

/**
 * @author Denis Zhdanov
 */
public class GradleExecutionSettings extends ExternalSystemExecutionSettings {

  private static final boolean USE_VERBOSE_GRADLE_API_BY_DEFAULT = Boolean.parseBoolean(System.getProperty("gradle.api.verbose"));

  private static final long serialVersionUID = 1L;

  @NotNull private final GradleExecutionWorkspace myExecutionWorkspace = new GradleExecutionWorkspace();

  @NotNull private final List<ClassHolder<? extends GradleProjectResolverExtension>> myResolverExtensions = ContainerUtilRt.newArrayList();
  @Nullable private final String myGradleHome;

  @Nullable private final String myServiceDirectory;
  private final boolean myIsOfflineWork;

  @NotNull private final DistributionType myDistributionType;
  @Nullable private String wrapperPropertyFile;

  @Nullable private String myJavaHome;
  @Nullable
  private String myIdeProjectPath;
  private boolean resolveModulePerSourceSet = true;
  private boolean useQualifiedModuleNames = false;
  private boolean delegatedBuild = true;

  public GradleExecutionSettings(@Nullable String gradleHome,
                                 @Nullable String serviceDirectory,
                                 @NotNull DistributionType distributionType,
                                 boolean isOfflineWork) {
    myGradleHome = gradleHome;
    myServiceDirectory = serviceDirectory;
    myDistributionType = distributionType;
    myIsOfflineWork = isOfflineWork;
    setVerboseProcessing(USE_VERBOSE_GRADLE_API_BY_DEFAULT);
  }

  public GradleExecutionSettings(@Nullable String gradleHome,
                                 @Nullable String serviceDirectory,
                                 @NotNull DistributionType distributionType,
                                 @Nullable String daemonVmOptions,
                                 boolean isOfflineWork) {
    myGradleHome = gradleHome;
    myServiceDirectory = serviceDirectory;
    myDistributionType = distributionType;
    if (daemonVmOptions != null) {
      withVmOptions(ParametersListUtil.parse(daemonVmOptions));
    }
    myIsOfflineWork = isOfflineWork;
    setVerboseProcessing(USE_VERBOSE_GRADLE_API_BY_DEFAULT);
  }

  public void setIdeProjectPath(@Nullable String ideProjectPath) {
    myIdeProjectPath = ideProjectPath;
  }

  @Nullable
  public String getIdeProjectPath() {
    return myIdeProjectPath;
  }

  @Nullable
  public String getGradleHome() {
    return myGradleHome;
  }

  @Nullable
  public String getServiceDirectory() {
    return myServiceDirectory;
  }

  @Nullable
  public String getJavaHome() {
    return myJavaHome;
  }

  public void setJavaHome(@Nullable String javaHome) {
    myJavaHome = javaHome;
  }

  public boolean isOfflineWork() {
    return myIsOfflineWork;
  }

  public boolean isResolveModulePerSourceSet() {
    return resolveModulePerSourceSet;
  }

  public void setResolveModulePerSourceSet(boolean resolveModulePerSourceSet) {
    this.resolveModulePerSourceSet = resolveModulePerSourceSet;
  }

  public boolean isUseQualifiedModuleNames() {
    return useQualifiedModuleNames;
  }

  public void setUseQualifiedModuleNames(boolean useQualifiedModuleNames) {
    this.useQualifiedModuleNames = useQualifiedModuleNames;
  }

  public boolean isDelegatedBuild() {
    return delegatedBuild;
  }

  public void setDelegatedBuild(boolean delegatedBuild) {
    this.delegatedBuild = delegatedBuild;
  }

  @NotNull
  public List<ClassHolder<? extends GradleProjectResolverExtension>> getResolverExtensions() {
    return myResolverExtensions;
  }

  public void addResolverExtensionClass(@NotNull ClassHolder<? extends GradleProjectResolverExtension> holder) {
    myResolverExtensions.add(holder);
  }

  /**
   * @return VM options to use for the gradle daemon process (if any)
   * @deprecated use {@link #getJvmArguments()}
   */
  @Deprecated
  @Nullable
  public String getDaemonVmOptions() {
    return ParametersListUtil.join(getJvmArguments());
  }

  @Nullable
  public String getWrapperPropertyFile() {
    return wrapperPropertyFile;
  }

  public void setWrapperPropertyFile(@Nullable String wrapperPropertyFile) {
    this.wrapperPropertyFile = wrapperPropertyFile;
  }

  @NotNull
  public DistributionType getDistributionType() {
    return myDistributionType;
  }

  @NotNull
  public GradleExecutionWorkspace getExecutionWorkspace() {
    return myExecutionWorkspace;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (myGradleHome != null ? myGradleHome.hashCode() : 0);
    result = 31 * result + (myServiceDirectory != null ? myServiceDirectory.hashCode() : 0);
    result = 31 * result + myDistributionType.hashCode();
    result = 31 * result + (myJavaHome != null ? myJavaHome.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;

    GradleExecutionSettings that = (GradleExecutionSettings)o;

    if (myDistributionType != that.myDistributionType) return false;
    if (myGradleHome != null ? !myGradleHome.equals(that.myGradleHome) : that.myGradleHome != null) return false;
    if (myJavaHome != null ? !myJavaHome.equals(that.myJavaHome) : that.myJavaHome != null) return false;
    if (myServiceDirectory != null ? !myServiceDirectory.equals(that.myServiceDirectory) : that.myServiceDirectory != null) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return "home: " + myGradleHome + ", distributionType: " + myDistributionType;
  }
}
