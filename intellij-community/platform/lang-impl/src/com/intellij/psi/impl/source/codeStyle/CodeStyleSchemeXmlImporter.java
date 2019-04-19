/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.application.options.ImportSchemeChooserDialog;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.SchemeImportUtil;
import com.intellij.openapi.options.SchemeImporter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports Intellij IDEA code style scheme in XML format.
 * 
 * @author Rustam Vishnyakov
 */
public class CodeStyleSchemeXmlImporter extends CodeStyleSettingsLoader implements SchemeImporter<CodeStyleScheme> {
  @NotNull
  @Override
  public String[] getSourceExtensions() {
    return new String[]{"xml"};
  }

  @Nullable
  @Override
  public CodeStyleScheme importScheme(@NotNull Project project,
                                      @NotNull VirtualFile selectedFile,
                                      @NotNull CodeStyleScheme currentScheme,
                                      @NotNull SchemeFactory<CodeStyleScheme> schemeFactory) throws SchemeImportException {
    Element rootElement = SchemeImportUtil.loadSchemeDom(selectedFile);
    Element schemeRoot = findSchemeRoot(rootElement);
    final Pair<String, CodeStyleScheme> importPair =
      !ApplicationManager.getApplication().isUnitTestMode() ?
      ImportSchemeChooserDialog.selectOrCreateTargetScheme(project, currentScheme, schemeFactory, getSchemeName(schemeRoot)) :
      Pair.create(currentScheme.getName(), currentScheme);
    if (importPair != null) {
      return readSchemeFromDom(schemeRoot, importPair.second);
    }
    return null;
  }
  
  @NotNull
  private static String getSchemeName(@NotNull Element rootElement) throws SchemeImportException {
    String rootName = rootElement.getName();
    if ("value".equals(rootElement.getName())) return "Project";
    if (!"code_scheme".equals(rootName)) {
      throw new SchemeImportException(ApplicationBundle.message("settings.code.style.import.xml.error.invalid.file", rootName));
    }
    Attribute schemeNameAttr = rootElement.getAttribute("name");
    if (schemeNameAttr == null) {
      throw new SchemeImportException(ApplicationBundle.message("settings.code.style.import.xml.error.missing.scheme.name"));
    }
    return schemeNameAttr.getValue();
  }


  private static CodeStyleScheme readSchemeFromDom(@NotNull Element rootElement, @NotNull CodeStyleScheme scheme)
    throws SchemeImportException {
    CodeStyleSettings newSettings = new CodeStyleSettings();
    loadSettings(rootElement, newSettings);
    newSettings.resetDeprecatedFields(); // Clean up if imported from legacy settings
    ((CodeStyleSchemeImpl)scheme).setCodeStyleSettings(newSettings);
    return scheme;
  }
}
