// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.instrumentation;

import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.Label;

import java.io.IOException;
import java.io.InputStream;

public class FailSafeClassReader extends ClassReader {
  private static final Label INVALID = new Label();

  public FailSafeClassReader(byte[] b) {
    super(b);
  }

  public FailSafeClassReader(byte[] b, int off, int len) {
    super(b, off, len);
  }

  public FailSafeClassReader(InputStream is) throws IOException {
    super(is);
  }

  public FailSafeClassReader(String name) throws IOException {
    super(name);
  }

  @Override
  protected Label readLabel(int offset, Label[] labels) {
    // attempt to workaround javac bug:
    // annotation table from original method is duplicated for synthetic bridge methods.
    // All offsets in the duplicated table is taken from original annotations table and obviously are not relevant for the bridge method
    if (offset >= 0 && offset < labels.length) {
      return super.readLabel(offset, labels);
    }
    else {
      return INVALID;
    }
  }
}