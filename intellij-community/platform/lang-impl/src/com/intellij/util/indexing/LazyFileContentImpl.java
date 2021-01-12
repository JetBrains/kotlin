// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.util.NotNullComputable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;


class LazyFileContentImpl extends FileContentImpl {
  @NotNull
  private final NotNullComputable<byte[]> myContentComputable;
  private boolean myContentComputed;

  LazyFileContentImpl(@NotNull VirtualFile file, @NotNull NotNullComputable<byte[]> contentComputable) {
    super(file, null, null, -1, true);
    myContentComputable = contentComputable;
  }

  @Override
  public byte @NotNull [] getContent() {
    initializeContent();
    return super.getContent();
  }

  @Override
  public @NotNull CharSequence getContentAsText() {
    initializeContent();
    return super.getContentAsText();
  }

  private void initializeContent() {
    if (!myContentComputed) {
      myContent = myContentComputable.get();
      myContentComputed = true;
      if (FileBasedIndex.ourSnapshotMappingsEnabled) {
        IndexedHashesSupport.getOrInitIndexedHash(this);
      }
    }
  }
}
