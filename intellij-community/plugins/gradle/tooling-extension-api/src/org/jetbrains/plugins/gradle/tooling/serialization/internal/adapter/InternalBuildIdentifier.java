// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.BuildIdentifier;

import java.io.File;

public class InternalBuildIdentifier implements BuildIdentifier {
  private final File rootDir;

  public InternalBuildIdentifier(File rootDir) {
    this.rootDir = rootDir.getAbsoluteFile();
  }

  @Override
  public File getRootDir() {
    return this.rootDir;
  }

  public String toString() {
    return "build=" + this.rootDir.getPath();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InternalBuildIdentifier that = (InternalBuildIdentifier)o;
    if (rootDir == that.rootDir) return true;
    return rootDir.getPath().equals(that.rootDir.getPath());
  }

  @Override
  public int hashCode() {
    return rootDir != null ? rootDir.getPath().hashCode() : 0;
  }
}
