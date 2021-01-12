// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public enum FileIndexingState {
  NOT_INDEXED,
  OUT_DATED,
  UP_TO_DATE;

  public boolean updateRequired() {
    return this != UP_TO_DATE;
  }
}
