/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.framework.detection.impl.exclude;

import com.intellij.openapi.util.Comparing;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.XCollection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class ExcludesConfigurationState {
  private List<String> myFrameworkTypes = new ArrayList<>();
  private List<ExcludedFileState> myFiles = new ArrayList<>();
  private boolean myDetectionEnabled = true;

  @Property(surroundWithTag = false)
  @XCollection(elementName = "type", valueAttributeName = "id")
  public List<String> getFrameworkTypes() {
    return myFrameworkTypes;
  }

  @Property(surroundWithTag = false)
  @XCollection
  public List<ExcludedFileState> getFiles() {
    return myFiles;
  }

  @Attribute("detection-enabled")
  public boolean isDetectionEnabled() {
    return myDetectionEnabled;
  }

  public void setDetectionEnabled(boolean detectionEnabled) {
    myDetectionEnabled = detectionEnabled;
  }

  public void setFrameworkTypes(List<String> frameworkTypes) {
    myFrameworkTypes = frameworkTypes;
  }

  public void setFiles(List<ExcludedFileState> files) {
    myFiles = files;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ExcludesConfigurationState)) return false;

    ExcludesConfigurationState state = (ExcludesConfigurationState)o;
    return myDetectionEnabled == state.myDetectionEnabled && Comparing.haveEqualElements(myFiles, state.myFiles)
           && Comparing.haveEqualElements(myFrameworkTypes, state.myFrameworkTypes);
  }

  @Override
  public int hashCode() {
    return 31 * myFrameworkTypes.hashCode() + myFiles.hashCode() + (myDetectionEnabled ? 1 : 0);
  }
}
