// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.injected.editor;

import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link VirtualFileWindow} instead. To be removed in IDEA 2018.1
 */
@Deprecated
public abstract class VirtualFileWindowImpl extends LightVirtualFile implements VirtualFileWindow {
  public VirtualFileWindowImpl(@NotNull String name,
                               Language language,
                               @NotNull CharSequence text) {
    super(name, language, text);
  }

  /**
   * @deprecated Use {@link VirtualFileWindow#getDelegate()} instead. to be removed in IDEA 2018.1
   */
  @Deprecated
  @NotNull
  @Override
  public VirtualFile getDelegate() {
    throw new IllegalStateException();
  }
}
