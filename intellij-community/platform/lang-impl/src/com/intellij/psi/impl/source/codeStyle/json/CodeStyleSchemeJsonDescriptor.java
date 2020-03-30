// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.codeStyle.json;

import com.intellij.application.options.codeStyle.properties.AbstractCodeStylePropertyMapper;
import com.intellij.application.options.codeStyle.properties.CodeStylePropertiesUtil;
import com.intellij.application.options.codeStyle.properties.GeneralCodeStylePropertyMapper;
import com.intellij.application.options.codeStyle.properties.LanguageCodeStylePropertyMapper;
import com.intellij.lang.Language;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.DisplayPriority;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CodeStyleSchemeJsonDescriptor {

  public final static String VERSION = "1.0";

  private transient final CodeStyleScheme myScheme;
  private transient final List<String> myLangDomainIds;

  public final String schemeName;
  @SuppressWarnings("FieldMayBeStatic")
  public final String version = VERSION;
  public PropertyListHolder codeStyle;

  CodeStyleSchemeJsonDescriptor(CodeStyleScheme scheme, List<String> ids) {
    myScheme = scheme;
    schemeName = scheme.getName();
    myLangDomainIds = ids;
    this.codeStyle = getPropertyListHolder();
  }

  private PropertyListHolder getPropertyListHolder() {
    PropertyListHolder holder = new PropertyListHolder();
    CodeStylePropertiesUtil.collectMappers(myScheme.getCodeStyleSettings(), mapper -> {
      if (myLangDomainIds == null || myLangDomainIds.contains(mapper.getLanguageDomainId())) {
        holder.add(mapper);
      }
    });
    holder.sort((m1, m2) -> {
      int result = Comparing.compare(getPriority(m1), getPriority(m2));
      if (result == 0) {
        return Comparing.compare(m1.getLanguageDomainId(), m2.getLanguageDomainId());
      }
      return result;
    });
    return holder;
  }

  private static DisplayPriority getPriority(@NotNull AbstractCodeStylePropertyMapper mapper) {
    if (mapper instanceof GeneralCodeStylePropertyMapper) {
      return DisplayPriority.GENERAL_SETTINGS;
    }
    else if (mapper instanceof LanguageCodeStylePropertyMapper) {
      Language language = ((LanguageCodeStylePropertyMapper)mapper).getLanguage();
      LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(language);
      if (provider != null) {
        return provider.getDisplayPriority();
      }
    }
    return DisplayPriority.OTHER_SETTINGS;
  }

  static class PropertyListHolder extends ArrayList<AbstractCodeStylePropertyMapper> {}
}
