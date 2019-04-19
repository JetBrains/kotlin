// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor;

import com.intellij.ide.ui.ConfigurableOptionsTopHitProvider;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public abstract class EditorOptionsTopHitProviderBase extends ConfigurableOptionsTopHitProvider {
  @NotNull
  @Override
  public String getId() {
    return "editor";
  }

  public static abstract class NoPrefix extends EditorOptionsTopHitProviderBase {
    @Override
    protected String getName(Configurable configurable) {
      return null;
    }
  }
}
