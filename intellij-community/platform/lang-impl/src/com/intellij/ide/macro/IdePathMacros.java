// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.macro;

import com.intellij.application.options.PathMacrosImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

final class IdePathMacros extends PathMacrosImpl {
  @Override
  public void removeToolMacroNames(@NotNull Set<String> result) {
    for (Macro macro : MacroManager.getInstance().getMacros()) {
      result.remove(macro.getName());
    }
  }
}
