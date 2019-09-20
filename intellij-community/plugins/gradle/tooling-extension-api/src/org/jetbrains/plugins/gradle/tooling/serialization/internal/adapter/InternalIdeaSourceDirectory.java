// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.idea.IdeaSourceDirectory;

import java.io.File;

public class InternalIdeaSourceDirectory implements IdeaSourceDirectory {
  private File directory;
  private boolean generated;

  @Override
  public File getDirectory() {
    return this.directory;
  }

  @Override
  public boolean isGenerated() {
    return this.generated;
  }

  public void setDirectory(File directory) {
    this.directory = directory;
  }

  public void setGenerated(boolean generated) {
    this.generated = generated;
  }

  public String toString() {
    return "IdeaSourceDirectory{directory=" + this.directory + ", generated=" + this.generated + '}';
  }
}
