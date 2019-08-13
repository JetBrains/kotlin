// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.template.macro;

import com.intellij.codeInsight.template.Macro;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NonNls;

import java.util.Collection;
import java.util.List;

public class MacroFactory {
  private static final MultiMap<String, Macro> myMacroTable = init();

  public static Macro createMacro(@NonNls String name) {
    return ContainerUtil.getFirstItem(myMacroTable.get(name));
  }

  public static List<Macro> getMacros(@NonNls String name) {
    return (List<Macro>)myMacroTable.get(name);
  }

  public static Macro[] getMacros() {
    final Collection<? extends Macro> values = myMacroTable.values();
    return values.toArray(new Macro[0]);
  }

  private static MultiMap<String, Macro> init() {
    MultiMap<String, Macro> result = MultiMap.create();
    for(Macro macro: Macro.EP_NAME.getExtensionList()) {
      result.putValue(macro.getName(), macro);
    }
    return result;
  }

}

