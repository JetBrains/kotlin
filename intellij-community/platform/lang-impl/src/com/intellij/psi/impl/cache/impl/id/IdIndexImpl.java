// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.cache.impl.id;

import com.intellij.util.indexing.CustomInputsIndexFileBasedIndexExtension;
import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class IdIndexImpl extends IdIndex implements CustomInputsIndexFileBasedIndexExtension<IdIndexEntry> {
  @NotNull
  @Override
  public DataExternalizer<Collection<IdIndexEntry>> createExternalizer() {
    return new IdIndexEntriesExternalizer();
  }
}
