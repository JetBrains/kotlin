// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.fileTypes.*;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class FileBasedIndexFileTypeListener implements FileTypeListener {
  @Override
  public void fileTypesChanged(@NotNull final FileTypeEvent event) {
    Set<ID<?, ?>> indexesToRebuild = new THashSet<>();
    for (FileBasedIndexExtension<?, ?> extension : FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList()) {
      if (IndexingStamp.versionDiffers(extension.getName(), extension.getVersion())) {
        indexesToRebuild.add(extension.getName());
      }
    }

    FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
    fileBasedIndex.scheduleFullIndexesRescan(indexesToRebuild);
  }
}
