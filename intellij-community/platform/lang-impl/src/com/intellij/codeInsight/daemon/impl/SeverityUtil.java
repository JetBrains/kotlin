// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SeverityUtil {
  @NotNull
  public static Collection<SeverityRegistrar.SeverityBasedTextAttributes> getRegisteredHighlightingInfoTypes(@NotNull SeverityRegistrar registrar) {
    List<SeverityRegistrar.SeverityBasedTextAttributes> collection = new ArrayList<>(registrar.allRegisteredAttributes());
    for (HighlightInfoType type : registrar.standardSeverities()) {
      if (HighlightInfoType.INFORMATION.equals(type) || HighlightInfoType.INFO.equals(type)) continue;
      collection.add(getSeverityBasedTextAttributes(registrar, type));
    }
    collection.sort(Comparator.comparing(SeverityRegistrar.SeverityBasedTextAttributes::getSeverity, registrar.reversed()));
    return collection;
  }

  private static SeverityRegistrar.SeverityBasedTextAttributes getSeverityBasedTextAttributes(@NotNull SeverityRegistrar registrar, @NotNull HighlightInfoType type) {
    final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    final TextAttributes textAttributes = scheme.getAttributes(type.getAttributesKey());
    if (textAttributes != null) {
      return new SeverityRegistrar.SeverityBasedTextAttributes(textAttributes, (HighlightInfoType.HighlightInfoTypeImpl)type);
    }
    TextAttributes severity = registrar.getTextAttributesBySeverity(type.getSeverity(null));
    return new SeverityRegistrar.SeverityBasedTextAttributes(severity, (HighlightInfoType.HighlightInfoTypeImpl)type);
  }
}
