// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.api.JavaVersion;
import org.gradle.tooling.model.java.InstalledJdk;

import java.io.File;

public class InternalInstalledJdk implements InstalledJdk {
  private final File javaHome;
  private final JavaVersion javaVersion;

  public InternalInstalledJdk(File javaHome, JavaVersion javaVersion) {
    this.javaHome = javaHome;
    this.javaVersion = javaVersion;
  }

  @Override
  public JavaVersion getJavaVersion() {
    return this.javaVersion;
  }

  @Override
  public File getJavaHome() {
    return this.javaHome;
  }
}
