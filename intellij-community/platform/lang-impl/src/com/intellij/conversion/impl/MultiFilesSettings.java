// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
class MultiFilesSettings {
  private SettingsXmlFile myProjectFile;
  private final List<SettingsXmlFile> mySettingsFiles;

  protected MultiFilesSettings(@Nullable Path projectFile, @Nullable File[] settingsFiles, @NotNull ConversionContextImpl context)
    throws CannotConvertException {
    if (projectFile == null && settingsFiles == null) {
      throw new IllegalArgumentException("Either project file or settings files should be not null");
    }

    if (projectFile != null && Files.exists(projectFile)) {
      myProjectFile = context.getOrCreateFile(projectFile);
    }
    mySettingsFiles = new ArrayList<>();

    if (settingsFiles != null) {
      for (File file : settingsFiles) {
        mySettingsFiles.add(context.getOrCreateFile(file.toPath()));
      }
    }
  }

  @NotNull
  protected Collection<? extends Element> getSettings(@NotNull String componentName, @NotNull String tagName) {
    final List<Element> result = new ArrayList<>();
    if (myProjectFile != null) {
      result.addAll(JDOMUtil.getChildren(myProjectFile.findComponent(componentName), tagName));
    }

    for (SettingsXmlFile file : mySettingsFiles) {
      result.addAll(JDOMUtil.getChildren(file.getRootElement(), tagName));
    }

    return result;
  }

  public Collection<Path> getAffectedFiles() {
    final List<Path> files = new ArrayList<>();

    if (myProjectFile != null) {
      files.add(myProjectFile.getFile());
    }
    for (SettingsXmlFile file : mySettingsFiles) {
      files.add(file.getFile());
    }
    return files;
  }
}
