/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.application.options.colors;

import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptorWithPath;
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme;
import com.intellij.openapi.editor.colors.impl.ReadOnlyColorsScheme;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.colors.AbstractKeyDescriptor;
import com.intellij.openapi.options.colors.ColorAndFontDescriptorsProvider;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public abstract class ColorAndFontDescription extends TextAttributes implements EditorSchemeAttributeDescriptorWithPath {
  private final String myName;
  private final String myGroup;
  private final String myType;
  private final Icon myIcon;
  private final String myToolTip;
  private final EditorColorsScheme myScheme;
  private boolean isForegroundChecked;
  private boolean isBackgroundChecked;
  private boolean isEffectsColorChecked;
  private boolean isErrorStripeChecked;
  private boolean isInherited;

  public ColorAndFontDescription(@NotNull String name,
                                 @Nullable String group,
                                 @Nullable String type,
                                 @Nullable EditorColorsScheme scheme,
                                 @Nullable Icon icon,
                                 @Nullable String toolTip) {
    myName = name;
    myGroup = group;
    myType = type;
    myScheme = scheme;
    myIcon = icon;
    myToolTip = toolTip;
  }

  public String toString() {
    return myName;
  }

  @Override
  public String getGroup() {
    return myGroup;
  }

  @Override
  public String getType() {
    return myType;
  }

  @Override
  public EditorColorsScheme getScheme() {
    return myScheme;
  }

  public Icon getIcon() {
    return myIcon;
  }

  public String getToolTip() {
    return myToolTip;
  }

  protected void initCheckedStatus() {
    isForegroundChecked = getExternalForeground() != null;
    isBackgroundChecked = getExternalBackground() != null;
    isErrorStripeChecked = getExternalErrorStripe() != null;
    isEffectsColorChecked = getExternalEffectColor() != null;
    super.setForegroundColor(getExternalForeground());
    super.setBackgroundColor(getExternalBackground());
    super.setEffectColor(getExternalEffectColor());
    super.setEffectType(getExternalEffectType());
    super.setErrorStripeColor(getExternalErrorStripe());
  }

  public abstract Color getExternalForeground();

  public abstract Color getExternalBackground();

  public abstract Color getExternalErrorStripe();

  public abstract Color getExternalEffectColor();

  public abstract EffectType getExternalEffectType();

  public abstract void setExternalForeground(Color col);

  public abstract void setExternalBackground(Color col);

  public abstract void setExternalErrorStripe(Color col);

  public abstract void setExternalEffectColor(Color color);

  public abstract void setExternalEffectType(EffectType type);

  @Override
  public final void setForegroundColor(Color col) {
    super.setForegroundColor(col);
    if (isForegroundChecked) {
      setExternalForeground(col);
    } else {
      setExternalForeground(null);
    }
  }

  @Override
  public final void setBackgroundColor(Color col) {
    super.setBackgroundColor(col);
    if (isBackgroundChecked) {
      setExternalBackground(col);
    } else {
      setExternalBackground(null);
    }
  }

  @Override
  public void setErrorStripeColor(Color color) {
    super.setErrorStripeColor(color);
    if (isErrorStripeChecked) {
      setExternalErrorStripe(color);
    }
    else {
      setExternalErrorStripe(null);
    }
  }

  @Override
  public final void setEffectColor(Color col) {
    super.setEffectColor(col);
    if (isEffectsColorChecked) {
      setExternalEffectColor(col);
    } else {
      setExternalEffectColor(null);
    }
  }

  @Override
  public final void setEffectType(EffectType effectType) {
    super.setEffectType(effectType);
    setExternalEffectType(effectType);
  }

  public boolean isForegroundChecked() {
    return isForegroundChecked;
  }

  public boolean isBackgroundChecked() {
    return isBackgroundChecked;
  }

  public boolean isErrorStripeChecked() {
    return isErrorStripeChecked;
  }

  public boolean isEffectsColorChecked() {
    return isEffectsColorChecked;
  }

  public final void setForegroundChecked(boolean val) {
    isForegroundChecked = val;
    setForegroundColor(getForegroundColor());
  }

  public final void setBackgroundChecked(boolean val) {
    isBackgroundChecked = val;
    setBackgroundColor(getBackgroundColor());
  }

  public final void setErrorStripeChecked(boolean val) {
    isErrorStripeChecked = val;
    setErrorStripeColor(getErrorStripeColor());
  }

  public final void setEffectsColorChecked(boolean val) {
    isEffectsColorChecked = val;
    setEffectColor(getEffectColor());
    setEffectType(getEffectType());
  }

  @Override
  public abstract int getFontType();

  @Override
  public abstract void setFontType(int type);

  public boolean isFontEnabled() {
    return true;
  }

  public boolean isForegroundEnabled() {
    return true;
  }

  public boolean isBackgroundEnabled() {
    return true;
  }

  public boolean isErrorStripeEnabled() {
    return false;
  }

  public boolean isEffectsColorEnabled() {
    return true;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  public final boolean isInherited() {
    return isInherited;
  }

  public void setInherited(boolean isInherited) {
    this.isInherited = isInherited;
  }

  public boolean isEditable() {
    return !(myScheme instanceof ReadOnlyColorsScheme ||
             myScheme instanceof AbstractColorsScheme && ((AbstractColorsScheme)myScheme).isReadOnly());
  }

  @Nullable
  public TextAttributes getBaseAttributes() {
    return null;
  }

  @Nullable
  public Pair<ColorAndFontDescriptorsProvider, ? extends AbstractKeyDescriptor> getFallbackKeyDescriptor() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }
}