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
package org.jetbrains.plugins.gradle.model.data;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class Ear extends Jar {
  private static final long serialVersionUID = 1L;

  @NotNull
  private final String myAppDirName;
  @NotNull
  private final String myLibDirName;
  private String myDeploymentDescriptor;
  @NotNull
  private List<EarResource> myResources = Collections.emptyList();

  public Ear(@NotNull String name, @NotNull String appDirName, @NotNull String libDirName) {
    super(name);
    myAppDirName = appDirName;
    myLibDirName = libDirName;
  }

  @NotNull
  public String getAppDirName() {
    return myAppDirName;
  }

  @NotNull
  public String getLibDirName() {
    return myLibDirName;
  }

  public void setDeploymentDescriptor(String deploymentDescriptor) {
    myDeploymentDescriptor = deploymentDescriptor;
  }

  public String getDeploymentDescriptor() {
    return myDeploymentDescriptor;
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
    if (!myAppDirName.equals(that.myAppDirName)) return false;
    if (!myLibDirName.equals(that.myLibDirName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myAppDirName.hashCode();
    result = 31 * result + myLibDirName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Ear{" +
           "appDirName='" + myAppDirName + '\'' +
           ", libDirName='" + myLibDirName + '\'' +
           '}';
  }
}
