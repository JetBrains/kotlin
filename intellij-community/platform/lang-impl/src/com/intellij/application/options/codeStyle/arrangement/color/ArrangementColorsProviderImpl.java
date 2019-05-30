// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.color;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementColorsAware;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Zhdanov
 */
public class ArrangementColorsProviderImpl implements ArrangementColorsProvider {

  @NotNull private final Map<ArrangementSettingsToken, TextAttributes> myNormalAttributesCache   =
    new HashMap<>();
  @NotNull private final Map<ArrangementSettingsToken, TextAttributes> mySelectedAttributesCache =
    new HashMap<>();

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
    myDefaultNormalBorderColor = JBColor.border();
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
