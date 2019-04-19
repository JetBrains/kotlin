// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppBinary;

/**
 * @author Vladislav.Soroka
 */
public class CppBinaryImpl implements CppBinary {
  private final String myName;
  private final String myBaseName;
  private final String myVariantName;
  private CompilationDetailsImpl myCompilationDetails;
  private LinkageDetailsImpl myLinkageDetails;

  public CppBinaryImpl(String name, String baseName, String variantName) {
    myName = name;
    myBaseName = baseName;
    myVariantName = variantName;
    myCompilationDetails = new CompilationDetailsImpl();
    myLinkageDetails = new LinkageDetailsImpl();
  }

  public CppBinaryImpl(CppBinary binary) {
    this(binary.getName(), binary.getBaseName(), binary.getVariantName());
    myCompilationDetails = new CompilationDetailsImpl(binary.getCompilationDetails());
    myLinkageDetails = new LinkageDetailsImpl(binary.getLinkageDetails());
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getVariantName() {
    return myVariantName;
  }

  @Override
  public String getBaseName() {
    return myBaseName;
  }

  @Override
  public CompilationDetailsImpl getCompilationDetails() {
    return myCompilationDetails;
  }

  public void setCompilationDetails(@NotNull CompilationDetailsImpl compilationDetails) {
    myCompilationDetails = compilationDetails;
  }

  @Override
  public LinkageDetailsImpl getLinkageDetails() {
    return myLinkageDetails;
  }

  public void setLinkageDetails(@NotNull LinkageDetailsImpl linkageDetails) {
    myLinkageDetails = linkageDetails;
  }
}
