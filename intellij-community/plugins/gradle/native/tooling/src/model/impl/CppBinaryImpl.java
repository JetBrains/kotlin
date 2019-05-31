// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CompilationDetails;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppBinary;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.LinkageDetails;

/**
 * @author Vladislav.Soroka
 */
public class CppBinaryImpl implements CppBinary {
  private final String name;
  private final String baseName;
  private final String variantName;
  private CompilationDetailsImpl compilationDetails;
  private LinkageDetailsImpl linkageDetails;

  @PropertyMapping({"name", "baseName", "variantName", "compilationDetails", "linkageDetails"})
  protected CppBinaryImpl(String name,
                          String baseName,
                          String variantName,
                          CompilationDetails compilationDetails,
                          LinkageDetails linkageDetails) {
    this(name, baseName, variantName);
    this.compilationDetails = new CompilationDetailsImpl(compilationDetails);
    this.linkageDetails = new LinkageDetailsImpl(linkageDetails);
  }

  public CppBinaryImpl(String name, String baseName, String variantName) {
    this.name = name;
    this.baseName = baseName;
    this.variantName = variantName;
    compilationDetails = new CompilationDetailsImpl();
    linkageDetails = new LinkageDetailsImpl();
  }

  public CppBinaryImpl(CppBinary binary) {
    this(binary.getName(), binary.getBaseName(), binary.getVariantName(),
         binary.getCompilationDetails(), binary.getLinkageDetails());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getVariantName() {
    return variantName;
  }

  @Override
  public String getBaseName() {
    return baseName;
  }

  @Override
  public CompilationDetailsImpl getCompilationDetails() {
    return compilationDetails;
  }

  public void setCompilationDetails(@NotNull CompilationDetailsImpl compilationDetails) {
    this.compilationDetails = compilationDetails;
  }

  @Override
  public LinkageDetailsImpl getLinkageDetails() {
    return linkageDetails;
  }

  public void setLinkageDetails(@NotNull LinkageDetailsImpl linkageDetails) {
    this.linkageDetails = linkageDetails;
  }
}
