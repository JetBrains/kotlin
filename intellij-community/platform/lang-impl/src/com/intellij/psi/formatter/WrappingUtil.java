// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.formatter;

import com.intellij.formatting.Wrap;
import com.intellij.formatting.WrapType;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;

/**
 * Misc. code wrapping functions
 * @author rvishnyakov
 */
public final class WrappingUtil {

  private WrappingUtil() {
  }

  public static boolean shouldWrap(int setting) {
    return setting != CommonCodeStyleSettings.DO_NOT_WRAP;
  }

  public static Wrap createWrap(int setting) {
    return Wrap.createWrap(getWrapType(setting), true);
  }

  public static WrapType getWrapType(int setting) {
    switch (setting) {
      case CommonCodeStyleSettings.WRAP_ALWAYS:
        return WrapType.ALWAYS;
      case CommonCodeStyleSettings.WRAP_AS_NEEDED:
        return WrapType.NORMAL;
      case CommonCodeStyleSettings.DO_NOT_WRAP:
        return WrapType.NONE;
      default:
        return WrapType.CHOP_DOWN_IF_LONG;
    }
  }

}
