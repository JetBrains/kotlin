// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.java.impl;

import com.intellij.openapi.util.Comparing;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsUrlList;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.impl.JpsUrlListRole;
import org.jetbrains.jps.model.java.JpsJavaModuleExtension;
import org.jetbrains.jps.model.java.LanguageLevel;

public class JpsJavaModuleExtensionImpl extends JpsCompositeElementBase<JpsJavaModuleExtensionImpl> implements JpsJavaModuleExtension {
  private static final JpsUrlListRole JAVADOC_ROOTS_ROLE = new JpsUrlListRole("javadoc roots");
  private static final JpsUrlListRole ANNOTATIONS_ROOTS_ROLE = new JpsUrlListRole("annotation roots");
  private String myOutputUrl;
  private String myTestOutputUrl;
  private boolean myInheritOutput;
  private boolean myExcludeOutput;
  private LanguageLevel myLanguageLevel;

  public JpsJavaModuleExtensionImpl() {
    myContainer.setChild(JAVADOC_ROOTS_ROLE);
    myContainer.setChild(ANNOTATIONS_ROOTS_ROLE);
  }

  private JpsJavaModuleExtensionImpl(JpsJavaModuleExtensionImpl original) {
    super(original);
    myOutputUrl = original.myOutputUrl;
    myTestOutputUrl = original.myTestOutputUrl;
    myLanguageLevel = original.myLanguageLevel;
  }

  @NotNull
  @Override
  public JpsJavaModuleExtensionImpl createCopy() {
    return new JpsJavaModuleExtensionImpl(this);
  }

  @NotNull
  @Override
  public JpsUrlList getAnnotationRoots() {
    return myContainer.getChild(ANNOTATIONS_ROOTS_ROLE);
  }

  @NotNull
  @Override
  public JpsUrlList getJavadocRoots() {
    return myContainer.getChild(JAVADOC_ROOTS_ROLE);
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
  public String getTestOutputUrl() {
    return myTestOutputUrl;
  }

  @Override
  public void setTestOutputUrl(String testOutputUrl) {
    if (!Objects.equals(myTestOutputUrl, testOutputUrl)) {
      myTestOutputUrl = testOutputUrl;
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
  public void applyChanges(@NotNull JpsJavaModuleExtensionImpl modified) {
    setLanguageLevel(modified.myLanguageLevel);
    setInheritOutput(modified.myInheritOutput);
    setExcludeOutput(modified.myExcludeOutput);
    setOutputUrl(modified.myOutputUrl);
    setTestOutputUrl(modified.myTestOutputUrl);
  }

  @Override
  public boolean isInheritOutput() {
    return myInheritOutput;
  }

  @Override
  public void setInheritOutput(boolean inheritOutput) {
    if (myInheritOutput != inheritOutput) {
      myInheritOutput = inheritOutput;
      fireElementChanged();
    }
  }

  @Override
  public boolean isExcludeOutput() {
    return myExcludeOutput;
  }

  @Override
  public void setExcludeOutput(boolean excludeOutput) {
    if (myExcludeOutput != excludeOutput) {
      myExcludeOutput = excludeOutput;
      fireElementChanged();
    }
  }
}
