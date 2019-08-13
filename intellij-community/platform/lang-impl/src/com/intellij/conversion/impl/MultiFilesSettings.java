/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
class MultiFilesSettings {
  private SettingsXmlFile myProjectFile;
  private final List<SettingsXmlFile> mySettingsFiles;

  protected MultiFilesSettings(@Nullable File projectFile, @Nullable File[] settingsFiles, @NotNull ConversionContextImpl context)
    throws CannotConvertException {
    if (projectFile == null && settingsFiles == null) {
      throw new IllegalArgumentException("Either project file or settings files should be not null");
    }

    if (projectFile != null && projectFile.exists()) {
      myProjectFile = context.getOrCreateFile(projectFile);
    }
    mySettingsFiles = new ArrayList<>();

    if (settingsFiles != null) {
      for (File file : settingsFiles) {
        mySettingsFiles.add(context.getOrCreateFile(file));
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

  public Collection<File> getAffectedFiles() {
    final List<File> files = new ArrayList<>();

    if (myProjectFile != null) {
      files.add(myProjectFile.getFile());
    }
    for (SettingsXmlFile file : mySettingsFiles) {
      files.add(file.getFile());
    }
    return files;
  }
}
