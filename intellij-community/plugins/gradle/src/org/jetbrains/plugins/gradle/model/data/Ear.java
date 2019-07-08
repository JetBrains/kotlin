// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class Ear extends Jar {
  private static final long serialVersionUID = 1L;

  @NotNull
  private final String appDirName;
  @NotNull
  private final String libDirName;
  private String deploymentDescriptor;
  @NotNull
  private List<EarResource> myResources = Collections.emptyList();

  @PropertyMapping({"name", "appDirName", "libDirName"})
  public Ear(@NotNull String name, @NotNull String appDirName, @NotNull String libDirName) {
    super(name);
    this.appDirName = appDirName;
    this.libDirName = libDirName;
  }

  @NotNull
  public String getAppDirName() {
    return appDirName;
  }

  @NotNull
  public String getLibDirName() {
    return libDirName;
  }

  public void setDeploymentDescriptor(String deploymentDescriptor) {
    this.deploymentDescriptor = deploymentDescriptor;
  }

  public String getDeploymentDescriptor() {
    return deploymentDescriptor;
  }

  @NotNull
  public List<EarResource> getResources() {
    return myResources;
  }

  public void setResources(@NotNull List<EarResource> resources) {
    myResources = resources;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Ear)) return false;
    if (!super.equals(o)) return false;

    Ear that = (Ear)o;
    if (!appDirName.equals(that.appDirName)) return false;
    if (!libDirName.equals(that.libDirName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + appDirName.hashCode();
    result = 31 * result + libDirName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Ear{" +
           "appDirName='" + appDirName + '\'' +
           ", libDirName='" + libDirName + '\'' +
           '}';
  }
}
