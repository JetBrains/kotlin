// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template;

import com.intellij.codeInsight.template.impl.TemplateState;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

public interface TemplateManagerListener extends EventListener {
  void templateStarted(@NotNull TemplateState state);
}
