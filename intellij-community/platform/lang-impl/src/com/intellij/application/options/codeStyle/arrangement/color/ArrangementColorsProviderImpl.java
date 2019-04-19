/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.color;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementColorsAware;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Map;

/**
 * @author Denis Zhdanov
 */
public class ArrangementColorsProviderImpl implements ArrangementColorsProvider {

  @NotNull private final Map<ArrangementSettingsToken, TextAttributes> myNormalAttributesCache   = ContainerUtilRt.newHashMap();
  @NotNull private final Map<ArrangementSettingsToken, TextAttributes> mySelectedAttributesCache = ContainerUtilRt.newHashMap();

  @NotNull private final TextAttributes myDefaultNormalAttributes   = new TextAttributes();
  @NotNull private final TextAttributes myDefaultSelectedAttributes = new TextAttributes();
  @NotNull private final Color myDefaultNormalBorderColor;
  @NotNull private final Color myDefaultSelectedBorderColor;

  @Nullable private final ArrangementColorsAware myColorsAware;

  @Nullable private Color myCachedNormalBorderColor;
  @Nullable private Color myCachedSelectedBorderColor;

  public ArrangementColorsProviderImpl(@Nullable ArrangementColorsAware colorsAware) {
    myColorsAware = colorsAware;

    // Default settings.
    myDefaultNormalAttributes.setForegroundColor(UIUtil.getTreeForeground());
    myDefaultNormalAttributes.setBackgroundColor(UIUtil.getPanelBackground());
    myDefaultSelectedAttributes.setForegroundColor(UIUtil.getTreeSelectionForeground());
    myDefaultSelectedAttributes.setBackgroundColor(UIUtil.getTreeSelectionBackground());
    myDefaultNormalBorderColor = UIUtil.getBorderColor();
    Color selectionBorderColor = UIUtil.getTreeSelectionBorderColor();
    if (selectionBorderColor == null) {
      selectionBorderColor = JBColor.black;
    }
    myDefaultSelectedBorderColor = selectionBorderColor;
  }

  @NotNull
  @Override
  public Color getBorderColor(boolean selected) {
    final Color cached;
    if (selected) {
      cached = myCachedSelectedBorderColor;
    }
    else {
      cached = myCachedNormalBorderColor;
    }
    if (cached != null) {
      return cached;
    }
    
    Color result = null;
    if (myColorsAware != null) {
      result = myColorsAware.getBorderColor(EditorColorsManager.getInstance().getGlobalScheme(), selected);
    }
    if (result == null) {
      result = selected ? myDefaultSelectedBorderColor : myDefaultNormalBorderColor; 
    }
    if (selected) {
      myCachedSelectedBorderColor = result;
    }
    else {
      myCachedNormalBorderColor = result;
    }
    return result;
  }

  @NotNull
  @Override
  public TextAttributes getTextAttributes(@NotNull ArrangementSettingsToken token, boolean selected) {
    final TextAttributes cached;
    if (selected) {
      cached = mySelectedAttributesCache.get(token);
    }
    else {
      cached = myNormalAttributesCache.get(token);
    }
    if (cached != null) {
      return cached;
    }

    TextAttributes result = null;
    if (myColorsAware != null) {
      result = myColorsAware.getTextAttributes(EditorColorsManager.getInstance().getGlobalScheme(), token, selected);
    }
    if (result == null) {
      result = selected ? myDefaultSelectedAttributes : myDefaultNormalAttributes;
    }
    if (selected) {
      mySelectedAttributesCache.put(token, result);
    }
    else {
      myNormalAttributesCache.put(token, result);
    }

    return result;
  }

  /**
   * Asks the implementation to ensure that it uses the most up-to-date colors.
   * <p/>
   * I.e. this method is assumed to be called when color settings has been changed and gives a chance to reflect the changes
   * accordingly.
   */
  public void refresh() {
    if (myColorsAware != null) {
      myNormalAttributesCache.clear();
      mySelectedAttributesCache.clear();
    }
  }
}
