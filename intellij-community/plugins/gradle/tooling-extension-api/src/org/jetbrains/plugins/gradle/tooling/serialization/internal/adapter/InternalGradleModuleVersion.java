// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.GradleModuleVersion;

public class InternalGradleModuleVersion implements GradleModuleVersion {
  private String myGroup;
  private String myName;
  private String myVersion;

  @Override
  public String getGroup() {
    return myGroup;
  }

  public void setGroup(String group) {
    myGroup = group;
  }

  @Override
  public String getName() {
    return myName;
  }

  public void setName(String name) {
    myName = name;
  }

  @Override
  public String getVersion() {
    return myVersion;
  }

  public void setVersion(String version) {
    myVersion = version;
  }
}
