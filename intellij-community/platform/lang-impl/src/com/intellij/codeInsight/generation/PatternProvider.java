package com.intellij.codeInsight.generation;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;

/**
 * @author Dmitry Avdeev
 */
public interface PatternProvider {

  ExtensionPointName<PatternProvider> EXTENSION_POINT_NAME = ExtensionPointName.create("com.intellij.patternProvider");

  PatternDescriptor[] getDescriptors();

  boolean isAvailable(DataContext context);
}
