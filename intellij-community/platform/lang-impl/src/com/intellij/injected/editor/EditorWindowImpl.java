// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.injected.editor;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.UserDataHolderBase;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link EditorWindow} instead. To be removed in IDEA 2018.1
 */
@Deprecated
public abstract class EditorWindowImpl extends UserDataHolderBase implements EditorWindow {
  /**
   * @deprecated Use {@link EditorWindow#getDelegate()} instead. To be removed in IDEA 2018.1
   */
  @Deprecated
  @NotNull
  @Override
  public Editor getDelegate() {
    throw new IllegalStateException();
  }
}
