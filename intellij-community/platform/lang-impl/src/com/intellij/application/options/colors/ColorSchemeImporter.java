/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.colors;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.SchemeImportUtil;
import com.intellij.openapi.options.SchemeImporter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports Intellij IDEA color scheme (.icls) to application configuration
 */
public class ColorSchemeImporter implements SchemeImporter<EditorColorsScheme> {

  private final static String[] FILE_EXTENSIONS =
    new String[] {EditorColorsManager.COLOR_SCHEME_FILE_EXTENSION.substring(1), "jar"};

  @NotNull
  @Override
  public String[] getSourceExtensions() {
    return FILE_EXTENSIONS;
  }

  @Nullable
  @Override
  public EditorColorsScheme importScheme(@NotNull Project project,
                                         @NotNull VirtualFile selectedFile,
                                         @NotNull EditorColorsScheme currentScheme,
                                         @NotNull SchemeFactory<EditorColorsScheme> schemeFactory) throws SchemeImportException {
    Element root = SchemeImportUtil.loadSchemeDom(selectedFile);
    String name = getSchemeName(root);
    EditorColorsScheme scheme = schemeFactory.createNewScheme(name);
    String preferredName = scheme.getName();
    scheme.readExternal(root);
    scheme.setName(preferredName);
    try {
      EditorColorsManager.getInstance().resolveSchemeParent(scheme);
    }
    catch (InvalidDataException e) {
      throw new SchemeImportException("Required " + e.getMessage() + " base scheme is missing or is not a bundled (read-only) scheme.");
    }
    return scheme;
  }

  static String getSchemeName(@NotNull Element root) throws SchemeImportException {
    String name = root.getAttributeValue("name");
    if (name == null) throw new SchemeImportException("Scheme 'name' attribute is missing.");
    return name;
  }
}
