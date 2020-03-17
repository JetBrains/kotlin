// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar.ui;

import com.intellij.util.ui.StartupUiUtil;

/**
 * @author Konstantin Bulenkov
 */
public class NavBarUIManager {
  public static final NavBarUI COMMON = new CommonNavBarUI();
  public static final NavBarUI DARCULA = new DarculaNavBarUI();

  public static NavBarUI getUI() {
    if (StartupUiUtil.isUnderDarcula()) return DARCULA;
    return COMMON;
  }
}
