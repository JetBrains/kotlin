// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.cache.impl.id;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.indexing.CustomInputsIndexFileBasedIndexExtension;
import com.intellij.util.indexing.InvertedIndex;
import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public class IdIndexImpl extends IdIndex implements CustomInputsIndexFileBasedIndexExtension<IdIndexEntry> {
  @Override
  public int getVersion() {
    int version = super.getVersion();

    if (!InvertedIndex.ARE_COMPOSITE_INDEXERS_ENABLED) {
      FileType[] types = FileTypeRegistry.getInstance().getRegisteredFileTypes();
      Arrays.sort(types, (o1, o2) -> Comparing.compare(o1.getName(), o2.getName()));
      for(FileType fileType:types) {
        if (!isIndexable(fileType)) continue;
        IdIndexer indexer = IdTableBuilding.getFileTypeIndexer(fileType);
        if (indexer == null) continue;
        version = version * 31 + (indexer.getVersion() ^ indexer.getClass().getName().hashCode());
      }
    }

    return version;
  }

  @NotNull
  @Override
  public DataExternalizer<Collection<IdIndexEntry>> createExternalizer() {
    return new IdIndexEntriesExternalizer();
  }
}
