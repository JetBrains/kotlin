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

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

/**
 * @author yole
 */
public class ColorDescriptor {
  private final Color myColor;
  private String mySwingColor;
  private String mySystemColor;
  private String myAWTColor;

  public ColorDescriptor(final Color color) {
    myColor = color;
  }

  public static ColorDescriptor fromSwingColor(final String swingColor) {
    ColorDescriptor result = new ColorDescriptor(null);
    result.mySwingColor = swingColor;
    return result;
  }

  public static ColorDescriptor fromSystemColor(final String systemColor) {
    ColorDescriptor result = new ColorDescriptor(null);
    result.mySystemColor = systemColor;
    return result;
  }

  public static ColorDescriptor fromAWTColor(final String awtColor) {
    ColorDescriptor result = new ColorDescriptor(null);
    result.myAWTColor = awtColor;
    return result;
  }

  private static Color getColorField(final Class aClass, final String fieldName) {
    try {
      final Field field = aClass.getDeclaredField(fieldName);
      return (Color)field.get(null);
    }
    catch (NoSuchFieldException e) {
      return Color.black;
    }
    catch (IllegalAccessException e) {
      return Color.black;
    }
  }

  public Color getResolvedColor() {
    if (myColor != null) {
      return myColor;
    }
    if (mySwingColor != null) {
      return UIManager.getColor(mySwingColor);
    }
    if (mySystemColor != null) {
      return getColorField(SystemColor.class, mySystemColor);
    }
    if (myAWTColor != null) {
      return getColorField(Color.class, myAWTColor);
    }
    return null;
  }

  public Color getColor() {
    return myColor;
  }

  public String getSwingColor() {
    return mySwingColor;
  }

  public String getSystemColor() {
    return mySystemColor;
  }

  public String getAWTColor() {
    return myAWTColor;
  }

  public String toString() {
    if (mySwingColor != null) {
      return mySwingColor;
    }
    if (mySystemColor != null) {
      return mySystemColor;
    }
    if (myAWTColor != null) {
      return myAWTColor;
    }
    if (myColor != null) {
      return "[" + myColor.getRed() + "," + myColor.getGreen() + "," + myColor.getBlue() + "]";
    }
    return "null";
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof ColorDescriptor)) {
      return false;
    }
    ColorDescriptor rhs = (ColorDescriptor) obj;
    if (myColor != null) {
      return myColor.equals(rhs.myColor);
    }
    if (mySwingColor != null) {
      return mySwingColor.equals(rhs.mySwingColor);
    }
    if (mySystemColor != null) {
      return mySystemColor.equals(rhs.mySystemColor);
    }
    if (myAWTColor != null) {
      return myAWTColor.equals(rhs.myAWTColor);
    }
    return false;
  }

  public boolean isColorSet() {
    return myColor != null || mySwingColor != null || mySystemColor != null || myAWTColor != null;
  }
}
