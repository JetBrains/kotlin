// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.macro;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

/**
 * This filter allows to hide some macro irrelevant or invalid in certain IDE-related context
 * (e.g. "JDKPath" macro is useless when IDE doesn't support Java at all)
 */
public abstract class MacroFilter {
  public static final ExtensionPointName<MacroFilter> EP_NAME = ExtensionPointName.create("com.intellij.macroFilter");

  public abstract boolean accept(@NotNull Macro macro);

  public static final MacroFilter GLOBAL = new MacroFilter() {
    @Override
    public boolean accept(@NotNull Macro macro) {
      for (MacroFilter filter : EP_NAME.getExtensionList()) {
        if (!filter.accept(macro)) return false;
      }
      return true;
    }
  };
}
