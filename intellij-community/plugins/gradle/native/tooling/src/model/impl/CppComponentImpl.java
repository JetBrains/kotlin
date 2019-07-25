// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import com.intellij.serialization.PropertyMapping;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.*;

import java.util.LinkedHashSet;
import java.util.Set;

public class CppComponentImpl implements CppComponent {
  private final String name;
  private final String baseName;
  private final Set<CppBinary> binaries;

  @PropertyMapping({"name", "baseName", "binaries"})
  protected CppComponentImpl(String name, String baseName, Set<? extends CppBinary> binaries) {
    this(name, baseName);
    for (CppBinary binary : binaries) {
      this.binaries.add(newCopy(binary));
    }
  }

  public CppComponentImpl(String name, String baseName) {
    this.name = name;
    this.baseName = baseName;
    this.binaries = new LinkedHashSet<CppBinary>();
  }

  public CppComponentImpl(CppComponent component) {
    this(component.getName(), component.getBaseName(),
         component.getBinaries());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getBaseName() {
    return baseName;
  }

  @Override
  public Set<? extends CppBinary> getBinaries() {
    return binaries;
  }

  public void addBinary(CppBinary binary) {
    binaries.add(binary);
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
