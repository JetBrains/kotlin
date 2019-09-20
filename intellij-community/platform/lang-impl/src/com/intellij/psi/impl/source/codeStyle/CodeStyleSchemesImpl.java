// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.application.options.schemes.SchemeNameGenerator;
import com.intellij.configurationStore.LazySchemeProcessor;
import com.intellij.configurationStore.SchemeDataHolder;
import com.intellij.openapi.options.SchemeManager;
import com.intellij.openapi.options.SchemeManagerFactory;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public abstract class CodeStyleSchemesImpl extends CodeStyleSchemes {
  @NonNls
  static final String CODE_STYLES_DIR_PATH = "codestyles";

  protected final SchemeManager<CodeStyleScheme> mySchemeManager;

  public CodeStyleSchemesImpl(@NotNull SchemeManagerFactory schemeManagerFactory) {
    mySchemeManager = schemeManagerFactory.create(CODE_STYLES_DIR_PATH, new LazySchemeProcessor<CodeStyleScheme, CodeStyleSchemeImpl>() {
      @NotNull
      @Override
      public CodeStyleSchemeImpl createScheme(@NotNull SchemeDataHolder<? super CodeStyleSchemeImpl> dataHolder,
                                              @NotNull String name,
                                              @NotNull Function<? super String, String> attributeProvider,
                                              boolean isBundled) {
        return new CodeStyleSchemeImpl(attributeProvider.apply("name"), attributeProvider.apply("parent"), dataHolder);
      }
    });

    mySchemeManager.loadSchemes();
    setCurrentScheme(getDefaultScheme());
  }

  @Override
  @Transient
  public CodeStyleScheme getCurrentScheme() {
    return mySchemeManager.getActiveScheme();
  }

  @Override
  public void setCurrentScheme(CodeStyleScheme scheme) {
    mySchemeManager.setCurrent(scheme);
  }

  @Override
  public CodeStyleScheme createNewScheme(String preferredName, CodeStyleScheme parentScheme) {
    return new CodeStyleSchemeImpl(
      SchemeNameGenerator.getUniqueName(preferredName, parentScheme, name -> mySchemeManager.findSchemeByName(name) != null),
      false,
      parentScheme);
  }

  @Override
  public void deleteScheme(@NotNull CodeStyleScheme scheme) {
    if (scheme.isDefault()) {
      throw new IllegalArgumentException("Unable to delete default scheme!");
    }

    CodeStyleSchemeImpl currentScheme = (CodeStyleSchemeImpl)getCurrentScheme();
    if (currentScheme == scheme) {
      CodeStyleScheme newCurrentScheme = getDefaultScheme();
      if (newCurrentScheme == null) {
        throw new IllegalStateException("Unable to load default scheme!");
      }
      setCurrentScheme(newCurrentScheme);
    }
    mySchemeManager.removeScheme(scheme);
  }

  @Override
  public CodeStyleScheme getDefaultScheme() {
    CodeStyleScheme defaultScheme = mySchemeManager.findSchemeByName(CodeStyleScheme.DEFAULT_SCHEME_NAME);
    if (defaultScheme == null) {
      defaultScheme = new CodeStyleSchemeImpl(CodeStyleScheme.DEFAULT_SCHEME_NAME, true, null);
      addScheme(defaultScheme);
    }
    return defaultScheme;
  }

  @Nullable
  @Override
  public CodeStyleScheme findSchemeByName(@NotNull String name) {
    return mySchemeManager.findSchemeByName(name);
  }

  @Override
  public void addScheme(@NotNull CodeStyleScheme scheme) {
    mySchemeManager.addScheme(scheme);
  }

  @NotNull
  public static SchemeManager<CodeStyleScheme> getSchemeManager() {
    return ((CodeStyleSchemesImpl)CodeStyleSchemes.getInstance()).mySchemeManager;
  }
}
