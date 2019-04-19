/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.SchemeImportUtil;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class CodeStyleSettingsLoader {

  public CodeStyleSettings loadSettings(@NotNull VirtualFile file) throws SchemeImportException {
    Element rootElement = SchemeImportUtil.loadSchemeDom(file);
    CodeStyleSettings settings = new CodeStyleSettings();
    loadSettings(rootElement, settings);
    return settings;
  }

  protected static void loadSettings(@NotNull Element rootElement, @NotNull CodeStyleSettings settings) throws SchemeImportException {
    try {
      settings.readExternal(findSchemeRoot(rootElement));
    }
    catch (InvalidDataException e) {
      throw new SchemeImportException(ApplicationBundle.message("settings.code.style.import.xml.error.can.not.load", e.getMessage()));
    }
  }

  protected static Element findSchemeRoot(@NotNull Element rootElement) throws SchemeImportException {
    String rootName = rootElement.getName();
    //
    // Project code style 172.x and earlier
    //
    if ("project".equals(rootName)) {
      Element child = rootElement.getChild("component");
      if (child != null && "ProjectCodeStyleSettingsManager".equals(child.getAttributeValue("name"))) {
        child = child.getChild("option");
        if (child != null && "PER_PROJECT_SETTINGS".equals(child.getAttributeValue("name"))) {
          child = child.getChild("value");
          if (child != null) return child;
        }
      }
      throw new SchemeImportException("Invalid scheme root: " + rootName);
    }
    //
    // Project code style 173.x and later
    //
    else if ("component".equals(rootName)) {
      if ("ProjectCodeStyleConfiguration".equals(rootElement.getAttributeValue("name"))) {
        Element child = rootElement.getChild("code_scheme");
        if (child != null) {
          return child;
        }
      }
      throw new SchemeImportException("Invalid scheme root: " + rootName);
    }
    return rootElement;
  }
}
