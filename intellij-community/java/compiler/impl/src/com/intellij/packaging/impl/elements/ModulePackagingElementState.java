// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NonNls;

public class ModulePackagingElementState {
  @NonNls public static final String MODULE_NAME_ATTRIBUTE = "name";

  private String myModuleName;

  @Attribute(MODULE_NAME_ATTRIBUTE)
  public String getModuleName() {
    return myModuleName;
  }

  public void setModuleName(String moduleName) {
    myModuleName = moduleName;
  }
}
