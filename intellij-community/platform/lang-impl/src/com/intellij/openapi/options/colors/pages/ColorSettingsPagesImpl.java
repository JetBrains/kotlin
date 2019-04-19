// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.options.colors.pages;

import com.intellij.application.options.colors.ColorSettingsUtil;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.options.colors.*;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public class ColorSettingsPagesImpl extends ColorSettingsPages {
  private final Map<Object, Pair<ColorAndFontDescriptorsProvider, ? extends AbstractKeyDescriptor>> myCache =
    ConcurrentFactoryMap.createMap(this::getDescriptorImpl);

  @Override
  public void registerPage(ColorSettingsPage page) {
    ColorSettingsPage.EP_NAME.getPoint(null).registerExtension(page);
  }

  @Override
  public ColorSettingsPage[] getRegisteredPages() {
    return ColorSettingsPage.EP_NAME.getExtensions();
  }

  @Nullable
  @Override
  public Pair<ColorAndFontDescriptorsProvider, AttributesDescriptor> getAttributeDescriptor(TextAttributesKey key) {
    //noinspection unchecked
    return (Pair<ColorAndFontDescriptorsProvider, AttributesDescriptor>)myCache.get(key);
  }

  @Nullable
  @Override
  public Pair<ColorAndFontDescriptorsProvider, ColorDescriptor> getColorDescriptor(ColorKey key) {
    //noinspection unchecked
    return (Pair<ColorAndFontDescriptorsProvider, ColorDescriptor>)myCache.get(key);
  }

  @Nullable
  private Pair<ColorAndFontDescriptorsProvider, ? extends AbstractKeyDescriptor> getDescriptorImpl(Object key) {
    JBIterable<ColorAndFontDescriptorsProvider> providers = JBIterable.empty();
    for (ColorAndFontDescriptorsProvider page : providers.append(getRegisteredPages()).append(ColorAndFontDescriptorsProvider.EP_NAME.getExtensionList())) {
      Iterable<? extends AbstractKeyDescriptor> descriptors =
        key instanceof TextAttributesKey ? ColorSettingsUtil.getAllAttributeDescriptors(page) :
        key instanceof ColorKey ? JBIterable.of(page.getColorDescriptors()) :
        Collections.emptyList();
      for (AbstractKeyDescriptor descriptor : descriptors) {
        if (descriptor.getKey() == key) {
          return Pair.create(page, descriptor);
        }
      }
    }
    return null;
  }
}
