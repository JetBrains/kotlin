// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options;

import com.intellij.psi.codeStyle.PredefinedCodeStyle;

import java.util.EventListener;

public interface PredefinedCodeStyleListener extends EventListener {
  void styleApplied(PredefinedCodeStyle style);
}
