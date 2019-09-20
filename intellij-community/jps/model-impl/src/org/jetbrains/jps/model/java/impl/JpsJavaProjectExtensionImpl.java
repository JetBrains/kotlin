/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jps.model.java.impl;

import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.java.JpsJavaProjectExtension;
import org.jetbrains.jps.model.java.LanguageLevel;

/**
 * @author nik
 */
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
    if (!Comparing.equal(myOutputUrl, outputUrl)) {
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
