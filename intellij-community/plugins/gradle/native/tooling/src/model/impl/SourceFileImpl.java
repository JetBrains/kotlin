// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.SourceFile;

import java.io.File;

public class SourceFileImpl implements SourceFile {
  private File mySourceFile;
  private File myObjectFile;

  public SourceFileImpl() {
  }

  public SourceFileImpl(File sourceFile, File objectFile) {
    mySourceFile = sourceFile;
    myObjectFile = objectFile;
  }

  public SourceFileImpl(SourceFile source) {
    this(source.getSourceFile(), source.getObjectFile());
  }

  @Override
  public File getSourceFile() {
    return mySourceFile;
  }

  public void setSourceFile(File sourceFile) {
    mySourceFile = sourceFile;
  }

  @Override
  public File getObjectFile() {
    return myObjectFile;
  }

  public void setObjectFile(File objectFile) {
    myObjectFile = objectFile;
  }
}
