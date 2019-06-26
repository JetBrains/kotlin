/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package com.intellij.ide.navigationToolbar.ui;

import com.intellij.util.ui.UIUtil;
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
    if (UIUtil.isUnderDarcula()) return DARCULA;
    return COMMON;
  }
}
