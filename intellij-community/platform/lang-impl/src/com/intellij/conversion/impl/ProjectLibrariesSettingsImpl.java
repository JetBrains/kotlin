// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ProjectLibrariesSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.library.JpsLibraryTableSerializer;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

public class ProjectLibrariesSettingsImpl extends MultiFilesSettings implements ProjectLibrariesSettings {

  public ProjectLibrariesSettingsImpl(@Nullable Path projectFile, File @Nullable [] librariesFiles,
                                      ConversionContextImpl context) throws CannotConvertException {
    super(projectFile, librariesFiles, context);
  }

  @Override
  @NotNull
  public Collection<? extends Element> getProjectLibraries() {
    return getSettings("libraryTable", JpsLibraryTableSerializer.LIBRARY_TAG);
  }
}
