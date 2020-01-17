// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.libraries;

import org.jetbrains.annotations.NotNull;

public class DummyLibraryProperties extends LibraryProperties<Object> {
  public static final DummyLibraryProperties INSTANCE = new DummyLibraryProperties();

  @Override
  public Object getState() {
    return null;
  }

  @Override
  public void loadState(@NotNull Object state) {
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof DummyLibraryProperties;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
