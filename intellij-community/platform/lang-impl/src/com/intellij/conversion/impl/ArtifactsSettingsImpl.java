// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.conversion.impl;

import com.intellij.conversion.ArtifactsSettings;
import com.intellij.conversion.CannotConvertException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

/**
 * @author Eugene.Kudelevsky
 */
public class ArtifactsSettingsImpl extends MultiFilesSettings implements ArtifactsSettings {
  protected ArtifactsSettingsImpl(@Nullable Path projectFile,
                                  @Nullable File[] settingsFiles,
                                  @NotNull ConversionContextImpl context) throws CannotConvertException {
    super(projectFile, settingsFiles, context);
  }

  @NotNull
  @Override
  public Collection<? extends Element> getArtifacts() {
    return getSettings("ArtifactManager", "artifact");
  }
}
