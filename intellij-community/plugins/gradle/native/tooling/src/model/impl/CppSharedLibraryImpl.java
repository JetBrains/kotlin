// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import com.intellij.serialization.PropertyMapping;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CompilationDetails;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppSharedLibrary;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.LinkageDetails;

public class CppSharedLibraryImpl extends CppBinaryImpl implements CppSharedLibrary {
  public CppSharedLibraryImpl(String name, String baseName, String variantName) {
    super(name, baseName, variantName);
  }

  public CppSharedLibraryImpl(CppSharedLibrary binary) {
    super(binary);
  }

  @PropertyMapping({"name", "baseName", "variantName", "compilationDetails", "linkageDetails"})
  private CppSharedLibraryImpl(String name,
                               String baseName,
                               String variantName,
                               CompilationDetails compilationDetails,
                               LinkageDetails linkageDetails) {
    super(name, baseName, variantName, compilationDetails, linkageDetails);
  }
}
