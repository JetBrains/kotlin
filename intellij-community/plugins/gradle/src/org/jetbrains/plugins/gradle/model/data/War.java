// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.serialization.PropertyMapping;
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
  private final String webAppDirName;
  @NotNull
  private final File webAppDir;
  @Nullable
  private File webXml;
  @NotNull
  private List<WebResource> webResources;
  @NotNull
  private Set<File> classpath;

  @PropertyMapping({"name", "webAppDirName", "webAppDir"})
  public War(@NotNull String name, @NotNull String webAppDirName, @NotNull File webAppDir) {
    super(name);
    this.webAppDirName = webAppDirName;
    this.webAppDir = webAppDir;
    webResources = Collections.emptyList();
    classpath = Collections.emptySet();
  }

  @NotNull
  public String getWebAppDirName() {
    return webAppDirName;
  }

  @NotNull
  public File getWebAppDir() {
    return webAppDir;
  }

  public void setWebXml(@Nullable File webXml) {
    this.webXml = webXml;
  }

  @Nullable
  public File getWebXml() {
    return webXml;
  }

  public void setWebResources(@Nullable List<WebResource> webResources) {
    this.webResources = webResources == null ? Collections.emptyList() : webResources;
  }

  @NotNull
  public List<WebResource> getWebResources() {
    return webResources;
  }

  public void setClasspath(@Nullable Set<File> classpath) {
    this.classpath = classpath == null ? Collections.emptySet() : classpath;
  }

  @NotNull
  public Set<File> getClasspath() {
    return classpath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof War)) return false;
    if (!super.equals(o)) return false;

    War war = (War)o;

    if (!webAppDirName.equals(war.webAppDirName)) return false;
    if (!webResources.equals(war.webResources)) return false;
    if (!classpath.equals(war.classpath)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + webAppDirName.hashCode();
    result = 31 * result + webResources.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "War{" +
           "name='" + getName() + '\'' +
           ", webAppDirName='" + webAppDirName + '\'' +
           ", webAppDir=" + webAppDir +
           ", webXml=" + webXml +
           ", webResources=" + webResources +
           '}';
  }
}
