/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.framework.detection.impl.exclude.old;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author nik
 */
@Tag("module")
public class DisabledAutodetectionInModuleElement {
  public static final Comparator<DisabledAutodetectionInModuleElement> COMPARATOR =
    (o1, o2) -> StringUtil.compare(o1.getModuleName(), o2.getModuleName(), true);
  private String myModuleName;
  private Set<String> myFiles = new LinkedHashSet<>();
  private Set<String> myDirectories = new LinkedHashSet<>();

  public DisabledAutodetectionInModuleElement() {
  }

  public DisabledAutodetectionInModuleElement(final String moduleName) {
    myModuleName = moduleName;
  }

  public DisabledAutodetectionInModuleElement(final String moduleName, final String url, final boolean recursively) {
    myModuleName = moduleName;
    if (recursively) {
      myDirectories.add(url);
    }
    else {
      myFiles.add(url);
    }
  }

  @Attribute("name")
  public String getModuleName() {
    return myModuleName;
  }

  @XCollection(propertyElementName = "files", elementName = "file", valueAttributeName = "url")
  public Set<String> getFiles() {
    return myFiles;
  }

  @XCollection(propertyElementName = "directories", elementName = "directory", valueAttributeName = "url")
  public Set<String> getDirectories() {
    return myDirectories;
  }

  public void setModuleName(final String moduleName) {
    myModuleName = moduleName;
  }

  public void setFiles(final Set<String> files) {
    myFiles = files;
  }

  public void setDirectories(final Set<String> directories) {
    myDirectories = directories;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final DisabledAutodetectionInModuleElement that = (DisabledAutodetectionInModuleElement)o;
    return myDirectories.equals(that.myDirectories) && myFiles.equals(that.myFiles) && myModuleName.equals(that.myModuleName);

  }

  @Override
  public int hashCode() {
    return 31 * (31 * myModuleName.hashCode() + myFiles.hashCode()) + myDirectories.hashCode();
  }

  public boolean isDisableInWholeModule() {
    return myFiles.isEmpty() && myDirectories.isEmpty();
  }
}
