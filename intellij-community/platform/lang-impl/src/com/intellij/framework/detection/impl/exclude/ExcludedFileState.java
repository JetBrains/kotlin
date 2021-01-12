// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.framework.detection.impl.exclude;

import com.intellij.openapi.util.Comparing;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import java.util.Objects;

@Tag("file")
public class ExcludedFileState {
  private String myUrl;
  private String myFrameworkType;

  public ExcludedFileState() {
  }

  public ExcludedFileState(String url, String frameworkType) {
    myUrl = url;
    myFrameworkType = frameworkType;
  }

  @Attribute("url")
  public String getUrl() {
    return myUrl;
  }

  public void setUrl(String url) {
    myUrl = url;
  }

  @Attribute("type")
  public String getFrameworkType() {
    return myFrameworkType;
  }

  public void setFrameworkType(String frameworkType) {
    myFrameworkType = frameworkType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExcludedFileState state = (ExcludedFileState)o;
    return Objects.equals(myFrameworkType, state.myFrameworkType) && Objects.equals(myUrl, state.myUrl);
  }

  @Override
  public int hashCode() {
    return Comparing.hashcode(myUrl, myFrameworkType);
  }
}
