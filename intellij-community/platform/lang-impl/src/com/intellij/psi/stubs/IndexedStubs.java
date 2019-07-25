// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

class IndexedStubs {
  @NotNull
  private final byte[] myStubTreeHash;
  @NotNull
  private final Map<StubIndexKey, Map<Object, StubIdList>> myStubIndicesValueMap;

  IndexedStubs(@NotNull byte[] stubTreeHash, @NotNull Map<StubIndexKey, Map<Object, StubIdList>> map) {
    myStubTreeHash = stubTreeHash;
    myStubIndicesValueMap = map;
  }

  @NotNull
  byte[] getStubTreeHash() {
    return myStubTreeHash;
  }

  @NotNull
  Map<StubIndexKey, Map<Object, StubIdList>> getStubIndicesValueMap() {
    return myStubIndicesValueMap;
  }
}
