/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.tooling.internal.web;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.web.WebConfiguration;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public class WarModelImpl implements WebConfiguration.WarModel {
  @NotNull
  private final String warName;
  private final String myWebAppDirName;
  private final File myWebAppDir;
  private File myArchivePath;
  private File myWebXml;
  private List<WebConfiguration.WebResource> myWebResources;
  private Set<File> myClasspath;
  private String myManifestContent;

  public WarModelImpl(@NotNull String name, String webAppDirName, File webAppDir) {
    warName = name;
    myWebAppDirName = webAppDirName;
    myWebAppDir = webAppDir;
  }

  @NotNull
  @Override
  public String getWarName() {
    return warName;
  }

  @Override
  public File getArchivePath() {
    return myArchivePath;
  }

  public void setArchivePath(File artifactFile) {
    myArchivePath = artifactFile;
  }

  @Override
  public String getWebAppDirName() {
    return myWebAppDirName;
  }

  @Override
  public File getWebAppDir() {
    return myWebAppDir;
  }

  public void setWebXml(File webXml) {
    myWebXml = webXml;
  }

  @Override
  public File getWebXml() {
    return myWebXml;
  }

  @Override
  public List<WebConfiguration.WebResource> getWebResources() {
    return myWebResources;
  }

  public void setWebResources(List<WebConfiguration.WebResource> webResources) {
    myWebResources = webResources;
  }

  public void setClasspath(Set<File> classpath) {
    myClasspath = classpath;
  }

  @Override
  public Set<File> getClasspath() {
    return myClasspath;
  }

  public void setManifestContent(String manifestContent) {
    myManifestContent = manifestContent;
  }

  @Override
  public String getManifestContent() {
    return myManifestContent;
  }
}
