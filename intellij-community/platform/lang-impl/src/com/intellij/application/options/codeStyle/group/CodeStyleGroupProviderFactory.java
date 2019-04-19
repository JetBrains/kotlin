// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.group;

import com.intellij.application.options.CodeStyleSchemesConfigurable;
import com.intellij.application.options.codeStyle.CodeStyleSchemesModel;
import com.intellij.psi.codeStyle.CodeStyleGroup;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CodeStyleGroupProviderFactory {
  private final Map<CodeStyleGroup,CodeStyleGroupProvider> myProviderMap = ContainerUtil.newHashMap();
  private final CodeStyleSchemesModel myModel;
  private final CodeStyleSchemesConfigurable mySchemesConfigurable;

  public CodeStyleGroupProviderFactory(CodeStyleSchemesModel model,
                                       CodeStyleSchemesConfigurable configurable) {
    myModel = model;
    mySchemesConfigurable = configurable;
  }

  public CodeStyleGroupProvider getGroupProvider(@NotNull CodeStyleGroup group) {
    if (!myProviderMap.containsKey(group)) {
      CodeStyleGroupProvider provider = new CodeStyleGroupProvider(group, myModel, mySchemesConfigurable);
      myProviderMap.put(group, provider);
    }
    return myProviderMap.get(group);
  }
}
