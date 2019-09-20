// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.snapshot;

import com.intellij.util.indexing.impl.InputData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class HashedInputData<Key, Value> extends InputData<Key, Value> {
  private final int myHashId;

  protected HashedInputData(@NotNull Map<Key, Value> values, int hashId) {
    super(values);
    myHashId = hashId;
  }

  public int getHashId() {
    return myHashId;
  }
}
