// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.java.impl;

import com.intellij.openapi.util.Comparing;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.java.JpsJavaProjectExtension;
import org.jetbrains.jps.model.java.LanguageLevel;

public class JpsJavaProjectExtensionImpl extends JpsElementBase<JpsJavaProjectExtensionImpl> implements JpsJavaProjectExtension {
  private String myOutputUrl;
  private LanguageLevel myLanguageLevel;

  public JpsJavaProjectExtensionImpl() {
  }

  private JpsJavaProjectExtensionImpl(JpsJavaProjectExtensionImpl original) {
    myOutputUrl = original.myOutputUrl;
    myLanguageLevel = original.myLanguageLevel;
  }

  @NotNull
  @Override
  public JpsJavaProjectExtensionImpl createCopy() {
    return new JpsJavaProjectExtensionImpl(this);
  }

  @Override
  public String getOutputUrl() {
    return myOutputUrl;
  }

  @Override
  public void setOutputUrl(String outputUrl) {
    if (!Objects.equals(myOutputUrl, outputUrl)) {
      myOutputUrl = outputUrl;
      fireElementChanged();
    }
  }

  @Override
  public LanguageLevel getLanguageLevel() {
    return myLanguageLevel;
  }

  @Override
  public void setLanguageLevel(LanguageLevel languageLevel) {
    if (!Comparing.equal(myLanguageLevel, languageLevel)) {
      myLanguageLevel = languageLevel;
      fireElementChanged();
    }
  }

  @Override
  public void applyChanges(@NotNull JpsJavaProjectExtensionImpl modified) {
    setLanguageLevel(modified.myLanguageLevel);
    setOutputUrl(modified.myOutputUrl);
  }
}
