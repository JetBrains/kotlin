// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.template.macro;

import com.intellij.codeInsight.template.Macro;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class MacroFactory {
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
    for (Macro macro: Macro.EP_NAME.getExtensionList()) {
      result.putValue(macro.getName(), macro);
    }
    Macro.EP_NAME.addExtensionPointListener(new ExtensionPointListener<Macro>() {
      @Override
      public void extensionAdded(@NotNull Macro extension, @NotNull PluginDescriptor pluginDescriptor) {
        myMacroTable.putValue(extension.getName(), extension);
      }

      @Override
      public void extensionRemoved(@NotNull Macro extension, @NotNull PluginDescriptor pluginDescriptor) {
        myMacroTable.remove(extension.getName(), extension);
      }
    }, ApplicationManager.getApplication());
    return result;
  }

}

