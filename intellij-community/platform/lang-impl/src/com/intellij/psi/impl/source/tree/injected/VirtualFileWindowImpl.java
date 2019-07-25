// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

class VirtualFileWindowImpl extends com.intellij.injected.editor.VirtualFileWindowImpl implements VirtualFileWindow {
  private final VirtualFile myDelegate;
  private final DocumentWindowImpl myDocumentWindow;

  VirtualFileWindowImpl(@NotNull String name,
                        @NotNull VirtualFile delegate,
                        @NotNull DocumentWindowImpl window,
                        @NotNull Language language,
                        @NotNull CharSequence text) {
    super(name, language, text);
    setCharset(delegate.getCharset());
    setFileType(language.getAssociatedFileType());
    if (delegate instanceof VirtualFileWindow) throw new IllegalArgumentException(delegate +" must not be injected");
    myDelegate = delegate;
    myDocumentWindow = window;
  }

  @NotNull
  @Override
  public VirtualFile getDelegate() {
    return myDelegate;
  }

  @NotNull
  @Override
  public DocumentWindowImpl getDocumentWindow() {
    return myDocumentWindow;
  }

  @Override
  public boolean isValid() {
    return myDelegate.isValid() && myDocumentWindow.isValid();
  }

  @Override
  public String toString() {
    return "VirtualFileWindow in " + myDelegate.getPresentableUrl();
  }
}