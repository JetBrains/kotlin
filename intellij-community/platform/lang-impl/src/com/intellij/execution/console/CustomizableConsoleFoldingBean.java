package com.intellij.execution.console;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;

/**
 * @author peter
 */
public class CustomizableConsoleFoldingBean extends AbstractExtensionPointBean {
  public static final ExtensionPointName<CustomizableConsoleFoldingBean> EP_NAME = ExtensionPointName.create("com.intellij.stacktrace.fold");

  @Attribute("substring")
  public String substring;

  @Attribute("negate")
  public boolean negate = false;

}
