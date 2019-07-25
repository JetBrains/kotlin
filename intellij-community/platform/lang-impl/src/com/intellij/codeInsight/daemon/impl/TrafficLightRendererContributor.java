// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Allows to replace the rendering of the traffic light icon in the top right corner of the editor.
 */
public interface TrafficLightRendererContributor {
  ExtensionPointName<TrafficLightRendererContributor> EP_NAME = ExtensionPointName.create("com.intellij.trafficLightRendererContributor");

  @Nullable
  TrafficLightRenderer createRenderer(@NotNull Editor editor, @Nullable PsiFile file);
}
