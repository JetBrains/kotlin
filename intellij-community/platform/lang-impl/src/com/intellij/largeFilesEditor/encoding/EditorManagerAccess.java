// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.encoding;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

public interface EditorManagerAccess {

  @NotNull
  VirtualFile getVirtualFile();

  @NotNull
  Editor getEditor();

  /**
   * @return true - if success, false - otherwise
   */
  boolean tryChangeEncoding(@NotNull Charset charset);

  String getCharsetName();
}
