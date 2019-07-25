// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import gnu.trove.THashMap;

import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

class ListenerWrapperMap<T extends EventListener> {
  private final Map<T,T> myListener2WrapperMap = new THashMap<>();

  void registerWrapper(T listener, T wrapper) {
    myListener2WrapperMap.put(listener, wrapper);
  }
  T removeWrapper(T listener) {
    return myListener2WrapperMap.remove(listener);
  }

  public Collection<T> wrappers() {
    return myListener2WrapperMap.values();
  }

  public String toString() {
    return new HashMap<>(myListener2WrapperMap).toString();
  }

  public void clear() {
    myListener2WrapperMap.clear();
  }
}
