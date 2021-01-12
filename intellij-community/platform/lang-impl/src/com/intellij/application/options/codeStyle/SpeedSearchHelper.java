// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle;

import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.speedSearch.SpeedSearchSupply;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class SpeedSearchHelper {
  private final @Nullable SpeedSearchSupply mySpeedSearch;
  private @Nullable       String            mySearchString;

  SpeedSearchHelper() {
    this(null);
  }

  public SpeedSearchHelper(@Nullable SpeedSearchSupply search) {
    mySpeedSearch = search;
  }

  private String getSearchString() {
    String speedSearch = mySpeedSearch != null ? mySpeedSearch.getEnteredPrefix() : null;
    if (StringUtil.isNotEmpty(speedSearch)) return speedSearch;
    return ObjectUtils.notNull(mySearchString, "");
  }

  public void find(@NotNull String searchString) {
    mySearchString = searchString;
    if (mySpeedSearch != null) {
      mySpeedSearch.findAndSelectElement(searchString);
    }
  }

  public void setLabelText(@NotNull SimpleColoredComponent label,
                    @NotNull String text,
                    int style,
                    @Nullable Color foreground,
                    @Nullable Color background) {
    label.clear();
    SearchUtil.appendFragments(getSearchString(), text, style, foreground, background, label);
  }
}
