/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.find.impl;

import com.intellij.find.FindModel;
import com.intellij.find.FindSettings;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

public interface FindPopupScopeUI {
  Pair<ScopeType, JComponent> @NotNull [] getComponents();

  @NotNull
  ScopeType initByModel(@NotNull FindModel findModel);
  void applyTo(@NotNull FindSettings findSettings, @NotNull FindPopupScopeUI.ScopeType selectedScope);
  void applyTo(@NotNull FindModel findModel, @NotNull FindPopupScopeUI.ScopeType selectedScope);

  @Nullable("null means OK")
  default ValidationInfo validate(@NotNull FindModel model, FindPopupScopeUI.ScopeType selectedScope) {
    return null;
  }

  /**
   * @return true if something was hidden
   */
  boolean hideAllPopups();

  class ScopeType {
    public final String name;
    public Supplier<String> textComputable;
    @Deprecated
    public final String text;
    public final Icon icon;

    public ScopeType(String name, Supplier<String> textComputable, Icon icon) {
      this.name = name;
      this.textComputable = textComputable;
      this.icon = icon;
      this.text = textComputable.get();
    }

    /**
     * @deprecated Use {@link #ScopeType(String, Supplier, Icon)}
     */
    @Deprecated
    public ScopeType(String name, String text, Icon icon) {
      this.name = name;
      this.textComputable = () -> text;
      this.icon = icon;
      this.text = text;
    }
  }
}
