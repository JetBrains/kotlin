// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class InternalIdeaSingleEntryLibraryDependency extends InternalIdeaDependency implements IdeaSingleEntryLibraryDependency {
  @NotNull private final File myFile;
  @Nullable private File mySource;
  @Nullable private File myJavadoc;
  private InternalGradleModuleVersion myModuleVersion;

  public InternalIdeaSingleEntryLibraryDependency(@NotNull File file) {myFile = file;}

  @NotNull
  @Override
  public File getFile() {
    return myFile;
  }

  @Nullable
  @Override
  public File getSource() {
    return mySource;
  }

  public void setSource(@Nullable File source) {
    mySource = source;
  }

  @Nullable
  @Override
  public File getJavadoc() {
    return myJavadoc;
  }

  @Override
  public boolean isExported() {
    return getExported();
  }

  public void setJavadoc(@Nullable File javadoc) {
    myJavadoc = javadoc;
  }

  @Override
  public GradleModuleVersion getGradleModuleVersion() {
    return myModuleVersion;
  }

  public void setModuleVersion(InternalGradleModuleVersion moduleVersion) {
    myModuleVersion = moduleVersion;
  }

  @Override
  public String toString() {
    return "IdeaSingleEntryLibraryDependency{" +
           "myFile=" + myFile +
           ", mySource=" + mySource +
           ", myJavadoc=" + myJavadoc +
           ", myModuleVersion=" + myModuleVersion +
           "} " + super.toString();
  }
}
