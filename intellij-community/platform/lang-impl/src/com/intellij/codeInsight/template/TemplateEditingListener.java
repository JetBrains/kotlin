// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.template;

import com.intellij.codeInsight.template.impl.TemplateState;
import org.jetbrains.annotations.NotNull;

public interface TemplateEditingListener {
  void beforeTemplateFinished(@NotNull TemplateState state, Template template);

  default void beforeTemplateFinished(@NotNull TemplateState state, Template template, boolean brokenOff) {
    beforeTemplateFinished(state, template);
  }

  void templateFinished(@NotNull Template template, boolean brokenOff);
  void templateCancelled(Template template);
  void currentVariableChanged(@NotNull TemplateState templateState, Template template, int oldIndex, int newIndex);
  void waitingForInput(Template template);
}
