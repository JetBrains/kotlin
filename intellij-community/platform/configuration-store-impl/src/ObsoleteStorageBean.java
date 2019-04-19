// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore;

import com.intellij.util.SmartList;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.XCollection;

import java.util.List;

final class ObsoleteStorageBean {
  @Attribute
  public String file;

  @Attribute
  public boolean isProjectLevel;

  @XCollection(propertyElementName = "components", style = XCollection.Style.v2, elementName = "component", valueAttributeName = "")
  public final List<String> components = new SmartList<>();
}
