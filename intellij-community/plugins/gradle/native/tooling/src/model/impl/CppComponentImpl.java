// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.*;

import java.util.LinkedHashSet;
import java.util.Set;

public class CppComponentImpl implements CppComponent {
  private final String myName;
  private final String myBaseName;
  private final Set<CppBinary> myBinaries;

  public CppComponentImpl(String name, String baseName) {
    myName = name;
    myBaseName = baseName;
    myBinaries = new LinkedHashSet<CppBinary>();
  }

  public CppComponentImpl(CppComponent component) {
    this(component.getName(), component.getBaseName());
    for (CppBinary binary : component.getBinaries()) {
      myBinaries.add(newCopy(binary));
    }
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getBaseName() {
    return myBaseName;
  }

  @Override
  public Set<? extends CppBinary> getBinaries() {
    return myBinaries;
  }

  public void addBinary(CppBinary binary) {
    myBinaries.add(binary);
  }


  private static CppBinary newCopy(CppBinary binary) {
    if (binary instanceof CppSharedLibrary) {
      return new CppSharedLibraryImpl((CppSharedLibrary)binary);
    }
    if (binary instanceof CppStaticLibrary) {
      return new CppStaticLibraryImpl((CppStaticLibrary)binary);
    }
    if (binary instanceof CppExecutable) {
      return new CppExecutableImpl((CppExecutable)binary);
    }
    return new CppBinaryImpl(binary);
  }
}
