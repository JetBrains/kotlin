// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar.ui;

import com.intellij.util.ui.StartupUiUtil;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Konstantin Bulenkov
 */
public class NavBarUIManager {
  public static final NavBarUI COMMON = new CommonNavBarUI();
  /**
   * @deprecated will be removed in 2020.1
   */
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public static final NavBarUI AQUA = COMMON;
  public static final NavBarUI DARCULA = new DarculaNavBarUI();


  public static NavBarUI getUI() {
    if (StartupUiUtil.isUnderDarcula()) return DARCULA;
    return COMMON;
  }
}
