/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.tooling.internal.ear;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.ear.EarConfiguration;

import java.io.File;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class EarModelImpl implements EarConfiguration.EarModel {
  @NotNull
  private final String myEarName;
  private final String myAppDirName;
  private final String myLibDirName;
  private File myArchivePath;
  private List<EarConfiguration.EarResource> myEarResources;
  private String myManifestContent;
  private String myDeploymentDescriptor;

  public EarModelImpl(@NotNull String name, @NotNull String appDirName, String libDirName) {
    myEarName = name;
    myAppDirName = appDirName;
    myLibDirName = libDirName;
  }

  @NotNull
  @Override
  public String getEarName() {
    return myEarName;
  }

  @Override
  public File getArchivePath() {
    return myArchivePath;
  }

  public void setArchivePath(File artifactFile) {
    myArchivePath = artifactFile;
  }

  @NotNull
  @Override
  public String getAppDirName() {
    return myAppDirName;
  }

  @Override
  public String getLibDirName() {
    return myLibDirName;
  }

  @Override
  public List<EarConfiguration.EarResource> getResources() {
    return myEarResources;
  }

  public void setResources(List<EarConfiguration.EarResource> earResources) {
    myEarResources = earResources;
  }

  public void setManifestContent(String manifestContent) {
    myManifestContent = manifestContent;
  }

  @Override
  public String getManifestContent() {
    return myManifestContent;
  }

  @Override
  public String getDeploymentDescriptor() {
    return myDeploymentDescriptor;
  }

  public void setDeploymentDescriptor(String deploymentDescriptor) {
    myDeploymentDescriptor = deploymentDescriptor;
  }
}
