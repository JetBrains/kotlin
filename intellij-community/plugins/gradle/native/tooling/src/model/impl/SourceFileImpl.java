// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.SourceFile;

import java.io.File;

public class SourceFileImpl implements SourceFile {
  private File sourceFile;
  private File objectFile;

  public SourceFileImpl() {
  }

  public SourceFileImpl(File sourceFile, File objectFile) {
    this.sourceFile = sourceFile;
    this.objectFile = objectFile;
  }

  public SourceFileImpl(SourceFile source) {
    this(source.getSourceFile(), source.getObjectFile());
  }

  @Override
  public File getSourceFile() {
    return sourceFile;
  }

  public void setSourceFile(File sourceFile) {
    this.sourceFile = sourceFile;
  }

  @Override
  public File getObjectFile() {
    return objectFile;
  }

  public void setObjectFile(File objectFile) {
    this.objectFile = objectFile;
  }
}
