// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.RequiredElement;
import com.intellij.util.xmlb.annotations.Attribute;

/**
 * Provide default settings for folding lines in console output.
 * <p>
 * Augments user configurable entries in "Editor | General | Console".
 * <p>
 * Register in {@code plugin.xml}:<p>
 * {@code <stacktrace.fold substring="at com.intellij.ide.IdeEventQueue"/>}
 *
 * @author peter
 */
public class CustomizableConsoleFoldingBean extends AbstractExtensionPointBean {

  public static final ExtensionPointName<CustomizableConsoleFoldingBean> EP_NAME =
    ExtensionPointName.create("com.intellij.stacktrace.fold");

  /**
   * Fold lines that contain this text.
   */
  @RequiredElement
  @Attribute("substring")
  public String substring;

  /**
   * If {@code true} suppresses folding.
   */
  @Attribute("negate")
  public boolean negate = false;
}
