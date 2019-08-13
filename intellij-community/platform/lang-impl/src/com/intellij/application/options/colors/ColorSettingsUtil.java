// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.colors;

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.SeveritiesProvider;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.options.OptionsBundle;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorAndFontDescriptorsProvider;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author lesya
 */
public class ColorSettingsUtil {
  private ColorSettingsUtil() {
  }

  public static Map<TextAttributesKey, String> keyToDisplayTextMap(final ColorSettingsPage page) {
    final List<AttributesDescriptor> attributeDescriptors = getAllAttributeDescriptors(page);
    final Map<TextAttributesKey, String> displayText = new HashMap<>();
    for (AttributesDescriptor attributeDescriptor : attributeDescriptors) {
      final TextAttributesKey key = attributeDescriptor.getKey();
      displayText.put(key, attributeDescriptor.getDisplayName());
    }
    return displayText;
  }

  public static List<AttributesDescriptor> getAllAttributeDescriptors(@NotNull ColorAndFontDescriptorsProvider provider) {
    List<AttributesDescriptor> result = new ArrayList<>();
    Collections.addAll(result, provider.getAttributeDescriptors());
    if (isInspectionColorsPage(provider)) {
      addInspectionSeverityAttributes(result);
    }
    return result;
  }

  private static boolean isInspectionColorsPage(ColorAndFontDescriptorsProvider provider) {
    // the first registered page implementing InspectionColorSettingsPage
    // gets the inspection attribute descriptors added to its list
    if (!(provider instanceof InspectionColorSettingsPage)) return false;
    for(ColorSettingsPage settingsPage: ColorSettingsPage.EP_NAME.getExtensionList()) {
      if (settingsPage == provider) break;
      if (settingsPage instanceof InspectionColorSettingsPage) return false;
    }
    return true;
  }

  private static void addInspectionSeverityAttributes(List<? super AttributesDescriptor> descriptors) {
    descriptors.add(new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.unknown.symbol"), CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES));
    descriptors.add(new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.deprecated.symbol"), CodeInsightColors.DEPRECATED_ATTRIBUTES));
    descriptors.add(new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.marked.for.removal.symbol"), CodeInsightColors.MARKED_FOR_REMOVAL_ATTRIBUTES));
    descriptors.add(new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.unused.symbol"), CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES));
    descriptors.add(new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.error"), CodeInsightColors.ERRORS_ATTRIBUTES));
    descriptors.add(new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.warning"), CodeInsightColors.WARNINGS_ATTRIBUTES));
    descriptors.add(new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.weak.warning"), CodeInsightColors.WEAK_WARNING_ATTRIBUTES));
    descriptors.add(new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.server.problems"), CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING));
    descriptors.add(new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.server.duplicate"), CodeInsightColors.DUPLICATE_FROM_SERVER));
    descriptors.add(new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.runtime"), CodeInsightColors.RUNTIME_ERROR));

    for (SeveritiesProvider provider : SeveritiesProvider.EP_NAME.getExtensionList()) {
      for (HighlightInfoType highlightInfoType : provider.getSeveritiesHighlightInfoTypes()) {
        final TextAttributesKey attributesKey = highlightInfoType.getAttributesKey();
        descriptors.add(new AttributesDescriptor(toDisplayName(attributesKey), attributesKey));
      }
    }
  }

  @NotNull
  private static String toDisplayName(@NotNull TextAttributesKey attributesKey) {
    return OptionsBundle.message(
      "options.java.attribute.descriptor.errors.group",
      StringUtil.capitalize(StringUtil.toLowerCase(attributesKey.getExternalName()).replaceAll("_", " ")));
  }
}
