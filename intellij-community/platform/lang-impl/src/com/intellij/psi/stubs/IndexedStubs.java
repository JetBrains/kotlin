// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

//TODO remove field myFileId and inline this class when we will advance StubUpdatingIndex version
class IndexedStubs {
  private final int myFileId;
  @NotNull
  private final Map<StubIndexKey, Map<Object, StubIdList>> myStubIndicesValueMap;

  IndexedStubs(int id,
               @NotNull
                 Map<StubIndexKey, Map<Object, StubIdList>> map) {
    myFileId = id;
    myStubIndicesValueMap = map;
  }

  int getFileId() {
    return myFileId;
  }

  @NotNull
  Map<StubIndexKey, Map<Object, StubIdList>> getStubIndicesValueMap() {
    return myStubIndicesValueMap;
  }
}
