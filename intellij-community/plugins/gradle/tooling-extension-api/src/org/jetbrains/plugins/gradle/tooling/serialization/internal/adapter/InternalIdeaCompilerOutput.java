// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.idea.IdeaCompilerOutput;

import java.io.File;

public class InternalIdeaCompilerOutput implements IdeaCompilerOutput {
  private boolean inheritOutputDirs;
  private File outputDir;
  private File testOutputDir;

  @Override
  public boolean getInheritOutputDirs() {
    return this.inheritOutputDirs;
  }

  public void setInheritOutputDirs(boolean inheritOutputDirs) {
    this.inheritOutputDirs = inheritOutputDirs;
  }

  @Override
  public File getOutputDir() {
    return this.outputDir;
  }

  public void setOutputDir(File outputDir) {
    this.outputDir = outputDir;
  }

  @Override
  public File getTestOutputDir() {
    return this.testOutputDir;
  }

  public void setTestOutputDir(File testOutputDir) {
    this.testOutputDir = testOutputDir;
  }

  public String toString() {
    return String.format("IdeaCompilerOutput{inheritOutputDirs=%s, outputDir=%s, testOutputDir=%s}",
                         this.inheritOutputDirs, this.outputDir, this.testOutputDir);
  }
}
