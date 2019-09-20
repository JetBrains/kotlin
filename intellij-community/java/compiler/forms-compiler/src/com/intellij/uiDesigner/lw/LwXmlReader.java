/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.uiDesigner.lw;

import com.intellij.uiDesigner.UIFormXmlConstants;
import org.jdom.Attribute;
import org.jdom.Element;

import java.awt.*;
import java.lang.reflect.Method;

public final class LwXmlReader {
  private LwXmlReader() {
  }

  /**
   * @return can be {@code null}.
   */
  public static Element getChild(final Element element, final String childName) {
    return element.getChild(childName, element.getNamespace());
  }

  /**
   * @return never {@code null}.
   */
  public static Element getRequiredChild(final Element element, final String childName) {
    final Element child = getChild(element, childName);
    if(child == null){
      throw new IllegalArgumentException("subtag '" + childName + "' is required: "+element);
    }
    return child;
  }

  /**
   * @return {@code null} or trimmed attribute value.
   */
  public static String getString(final Element element, final String attributeName){
    final String value = element.getAttributeValue(attributeName);
    return value != null ? value.trim() : null;
  }

  /**
   * @return never {@code null} trimmed attribute value.
   */
  public static String getRequiredString(final Element element, final String attributeName) {
    final String value = getString(element, attributeName);
    if(value != null){
      return value;
    }
    else{
      throw new IllegalArgumentException("attribute '" + attributeName + "' is required: "+element);
    }
  }

  public static String getOptionalString(final Element element, final String attributeName, final String defaultValue) {
    final String value = element.getAttributeValue(attributeName);
    return value != null ? value.trim() : defaultValue;
  }

  public static int getRequiredInt(final Element element, final String attributeName) {
    final String str = getRequiredString(element, attributeName);
    try {
      return Integer.parseInt(str);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("attribute '" + attributeName + "' is not a proper integer: " + str);
    }
  }

  public static int getOptionalInt(final Element element, final String attributeName, final int defaultValue) {
    final String str = element.getAttributeValue(attributeName);
    if (str == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(str);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("attribute '" + attributeName + "' is not a proper integer: " + str);
    }
  }

  public static boolean getOptionalBoolean(final Element element, final String attributeName, final boolean defaultValue) {
    final String str = element.getAttributeValue(attributeName);
    if (str == null) {
      return defaultValue;
    }
    return Boolean.valueOf(str).booleanValue();
  }

  public static double getRequiredDouble(final Element element, final String attributeName) {
    final String str = getRequiredString(element, attributeName);
    try {
      return Double.parseDouble(str);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("attribute '" + attributeName + "' is not a proper double: " + str);
    }
  }

  public static double getOptionalDouble(final Element element, final String attributeName, double defaultValue) {
    final String str = element.getAttributeValue(attributeName);
    if (str == null) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(str);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("attribute '" + attributeName + "' is not a proper double: " + str);
    }
  }

  public static float getRequiredFloat(final Element element, final String attributeName) {
    final String str = getRequiredString(element, attributeName);
    try {
      return Float.parseFloat(str);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("attribute '" + attributeName + "' is not a proper float: " + str);
    }
  }

  public static Object getRequiredPrimitiveTypeValue(final Element element, final String attributeName, final Class valueClass) {
    final String str = getRequiredString(element, attributeName);
    try {
      final Method method = valueClass.getMethod("valueOf", String.class);
      return method.invoke(null, str);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("attribute '" + attributeName + "' is not a proper float: " + str);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static StringDescriptor getStringDescriptor(final Element element, final String valueAttr,
                                                     final String bundleAttr, final String keyAttr) {
    final String title = element.getAttributeValue(valueAttr);
    if (title != null) {
      StringDescriptor descriptor = StringDescriptor.create(title);
      descriptor.setNoI18n(getOptionalBoolean(element, UIFormXmlConstants.ATTRIBUTE_NOI18N, false));
      return descriptor;
    }
    else {
      final String bundle = element.getAttributeValue(bundleAttr);
      if (bundle != null) {
        final String key = getRequiredString(element, keyAttr);
        return new StringDescriptor(bundle, key);
      }
    }

    return null;
  }

  public static FontDescriptor getFontDescriptor(final Element element) {
    String swingFont = element.getAttributeValue(UIFormXmlConstants.ATTRIBUTE_SWING_FONT);
    if (swingFont != null) {
      return FontDescriptor.fromSwingFont(swingFont);
    }

    String fontName = element.getAttributeValue(UIFormXmlConstants.ATTRIBUTE_NAME);
    int fontStyle = getOptionalInt(element, UIFormXmlConstants.ATTRIBUTE_STYLE, -1);
    int fontSize = getOptionalInt(element, UIFormXmlConstants.ATTRIBUTE_SIZE, -1);
    return new FontDescriptor(fontName, fontStyle, fontSize);
  }

  public static ColorDescriptor getColorDescriptor(final Element element) throws Exception {
    Attribute attr = element.getAttribute(UIFormXmlConstants.ATTRIBUTE_COLOR);
    if (attr != null) {
      return new ColorDescriptor(new Color(attr.getIntValue()));
    }
    String swingColor = element.getAttributeValue(UIFormXmlConstants.ATTRIBUTE_SWING_COLOR);
    if (swingColor != null) {
      return ColorDescriptor.fromSwingColor(swingColor);
    }
    String systemColor = element.getAttributeValue(UIFormXmlConstants.ATTRIBUTE_SYSTEM_COLOR);
    if (systemColor != null) {
      return ColorDescriptor.fromSystemColor(systemColor);
    }
    String awtColor = element.getAttributeValue(UIFormXmlConstants.ATTRIBUTE_AWT_COLOR);
    if (awtColor != null) {
      return ColorDescriptor.fromAWTColor(awtColor);
    }
    return new ColorDescriptor(null);
  }

  public static ColorDescriptor getOptionalColorDescriptor(final Element element) {
    if (element == null) return null;
    try {
      return getColorDescriptor(element);
    }
    catch(Exception ex) {
      return null;
    }
  }

  public static Insets readInsets(final Element element) {
    final int top = getRequiredInt(element, UIFormXmlConstants.ATTRIBUTE_TOP);
    final int left = getRequiredInt(element, UIFormXmlConstants.ATTRIBUTE_LEFT);
    final int bottom = getRequiredInt(element, UIFormXmlConstants.ATTRIBUTE_BOTTOM);
    final int right = getRequiredInt(element, UIFormXmlConstants.ATTRIBUTE_RIGHT);
    return new Insets(top, left, bottom, right);
  }
}
