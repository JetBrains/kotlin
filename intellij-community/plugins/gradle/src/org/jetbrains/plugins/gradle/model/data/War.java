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
package org.jetbrains.plugins.gradle.model.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public class War extends Jar {
  private static final long serialVersionUID = 1L;

  @NotNull
  private final String myWebAppDirName;
  @NotNull
  private final File myWebAppDir;
  @Nullable
  private File myWebXml;
  @NotNull
  private List<WebResource> myWebResources;
  @NotNull
  private Set<File> myClasspath;


  public War(@NotNull String name, @NotNull String webAppDirName, @NotNull File webAppDir) {
    super(name);
    myWebAppDirName = webAppDirName;
    myWebAppDir = webAppDir;
    myWebResources = Collections.emptyList();
    myClasspath = Collections.emptySet();
  }

  @NotNull
  public String getWebAppDirName() {
    return myWebAppDirName;
  }

  @NotNull
  public File getWebAppDir() {
    return myWebAppDir;
  }

  public void setWebXml(@Nullable File webXml) {
    myWebXml = webXml;
  }

  @Nullable
  public File getWebXml() {
    return myWebXml;
  }

  public void setWebResources(@Nullable List<WebResource> webResources) {
    myWebResources = webResources == null ? Collections.emptyList() : webResources;
  }

  @NotNull
  public List<WebResource> getWebResources() {
    return myWebResources;
  }

  public void setClasspath(@Nullable Set<File> classpath) {
    myClasspath = classpath == null ? Collections.emptySet() : classpath;
  }

  @NotNull
  public Set<File> getClasspath() {
    return myClasspath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof War)) return false;
    if (!super.equals(o)) return false;

    War war = (War)o;

    if (!myWebAppDirName.equals(war.myWebAppDirName)) return false;
    if (!myWebResources.equals(war.myWebResources)) return false;
    if (!myClasspath.equals(war.myClasspath)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myWebAppDirName.hashCode();
    result = 31 * result + myWebResources.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "War{" +
           "name='" + getName() + '\'' +
           ", webAppDirName='" + myWebAppDirName + '\'' +
           ", webAppDir=" + myWebAppDir +
           ", webXml=" + myWebXml +
           ", webResources=" + myWebResources +
           '}';
  }
}
