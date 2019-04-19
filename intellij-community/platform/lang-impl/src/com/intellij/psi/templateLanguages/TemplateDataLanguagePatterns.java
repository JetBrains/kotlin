// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.templateLanguages;

import com.intellij.lang.Language;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.impl.FileTypeAssocTable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author peter
 */
@State(
    name = "TemplateDataLanguagePatterns",
    storages = @Storage("templateLanguages.xml") )
public class TemplateDataLanguagePatterns implements PersistentStateComponent<Element> {
  private FileTypeAssocTable<Language> myAssocTable = new FileTypeAssocTable<>();
  @NonNls private static final String SEPARATOR = ";";

  public static TemplateDataLanguagePatterns getInstance() {
    return ServiceManager.getService(TemplateDataLanguagePatterns.class);
  }

  public FileTypeAssocTable<Language> getAssocTable() {
    return myAssocTable.copy();
  }

  @Nullable
  public Language getTemplateDataLanguageByFileName(VirtualFile file) {
    return myAssocTable.findAssociatedFileType(file.getName());
  }

  public void setAssocTable(FileTypeAssocTable<Language> assocTable) {
    myAssocTable = assocTable.copy();
  }

  @Override
  public void loadState(@NotNull Element state) {
    myAssocTable = new FileTypeAssocTable<>();

    final THashMap<String, Language> dialectMap = new THashMap<>();
    for (Language dialect : TemplateDataLanguageMappings.getTemplateableLanguages()) {
      dialectMap.put(dialect.getID(), dialect);
    }
    final List<Element> files = state.getChildren("pattern");
    for (Element fileElement : files) {
      final String patterns = fileElement.getAttributeValue("value");
      final String langId = fileElement.getAttributeValue("lang");
      final Language dialect = dialectMap.get(langId);
      if (dialect == null || StringUtil.isEmpty(patterns)) continue;

      for (String pattern : patterns.split(SEPARATOR)) {
        myAssocTable.addAssociation(FileTypeManager.parseFromString(pattern), dialect);
      }

    }
  }

  @Override
  public Element getState() {
    Element state = new Element("x");
    for (final Language language : TemplateDataLanguageMappings.getTemplateableLanguages()) {
      final List<FileNameMatcher> matchers = myAssocTable.getAssociations(language);
      if (!matchers.isEmpty()) {
        final Element child = new Element("pattern");
        state.addContent(child);
        child.setAttribute("value", StringUtil.join(matchers, fileNameMatcher -> fileNameMatcher.getPresentableString(), SEPARATOR));
        child.setAttribute("lang", language.getID());
      }
    }
    return state;
  }

}