/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.framework.detection.impl.exclude;

import com.intellij.openapi.util.Comparing;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;

/**
 * @author nik
 */
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
    return Comparing.equal(myFrameworkType, state.myFrameworkType) && Comparing.equal(myUrl, state.myUrl);
  }

  @Override
  public int hashCode() {
    return Comparing.hashcode(myUrl, myFrameworkType);
  }
}
