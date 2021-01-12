// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.options.colors.pages;

import com.intellij.application.options.colors.ColorSettingsUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.options.colors.*;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

final class ColorSettingsPagesImpl extends ColorSettingsPages implements Disposable {
  private final Map<Object, Pair<ColorAndFontDescriptorsProvider, ? extends AbstractKeyDescriptor<?>>> myCache =
    ConcurrentFactoryMap.createMap(this::getDescriptorImpl);

  ColorSettingsPagesImpl() {
    ColorAndFontDescriptorsProvider.EP_NAME.addChangeListener(myCache::clear, this);
    ColorSettingsPage.EP_NAME.addChangeListener(myCache::clear, this);
  }

  @Override
  public void registerPage(ColorSettingsPage page) {
    ColorSettingsPage.EP_NAME.getPoint().registerExtension(page);
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
  private Pair<ColorAndFontDescriptorsProvider, ? extends AbstractKeyDescriptor<?>> getDescriptorImpl(Object key) {
    JBIterable<ColorAndFontDescriptorsProvider> providers = JBIterable.empty();
    for (ColorAndFontDescriptorsProvider page : providers.append(getRegisteredPages()).append(ColorAndFontDescriptorsProvider.EP_NAME.getExtensionList())) {
      Iterable<? extends AbstractKeyDescriptor<?>> descriptors;
      if (key instanceof TextAttributesKey) {
        descriptors = ColorSettingsUtil.getAllAttributeDescriptors(page);
      }
      else {
        descriptors = key instanceof ColorKey ? JBIterable.of(page.getColorDescriptors()) : Collections.emptyList();
      }

      for (AbstractKeyDescriptor<?> descriptor : descriptors) {
        if (descriptor.getKey() == key) {
          return new Pair<>(page, descriptor);
        }
      }
    }
    return null;
  }

  @Override
  public void dispose() {
  }
}
