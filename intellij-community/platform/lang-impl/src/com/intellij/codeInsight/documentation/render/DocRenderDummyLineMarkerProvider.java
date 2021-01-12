// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.codeInsight.daemon.LineMarkerSettings;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DocRenderDummyLineMarkerProvider extends LineMarkerProviderDescriptor {
  private static final DocRenderDummyLineMarkerProvider INSTANCE = new DocRenderDummyLineMarkerProvider();

  @Override
  public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    return null; // this class does not generate line marker info, it exists to add configuration entry in settings
  }

  @Override
  public String getId() {
    return "RenderedDoc";
  }

  @Override
  public String getName() {
    return CodeInsightBundle.message("doc.render.gutter.icon.setting");
  }

  @Override
  public @Nullable Icon getIcon() {
    return AllIcons.Gutter.JavadocRead;
  }

  static boolean isGutterIconEnabled() {
    return LineMarkerSettings.getSettings().isEnabled(INSTANCE);
  }
}
