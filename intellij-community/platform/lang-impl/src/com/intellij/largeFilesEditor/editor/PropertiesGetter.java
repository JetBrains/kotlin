// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.openapi.diagnostic.Logger;

public class PropertiesGetter {
  private static final Logger logger = Logger.getInstance(PropertiesGetter.class);

  private static final String[] PAGE_SIZE_PROPERTY_NAMES = {"lfe.pageSize", "lfe.ps"};
  private static final String[] MAX_PAGE_BORDER_SHIFT_PROPERTY_NAMES = {"lfe.maxPageBorderShift", "lfe.mpbs"};

  private static final int DEFAULT_PAGE_SIZE_BYTES = 100_000;
  private static final int DEFAULT_MAX_PAGE_BORDER_SHIFT_BYTES = 1_000;

  public static int getPageSize() {
    return getPropertyInt(PAGE_SIZE_PROPERTY_NAMES, DEFAULT_PAGE_SIZE_BYTES);
  }

  public static int getMaxPageBorderShiftBytes() {
    return getPropertyInt(MAX_PAGE_BORDER_SHIFT_PROPERTY_NAMES, DEFAULT_MAX_PAGE_BORDER_SHIFT_BYTES);
  }

  private static int getPropertyInt(String[] propertyNames, int defaultValue) {
    String strValue;
    for (String propertyName : propertyNames) {
      strValue = System.getProperty(propertyName);
      if (strValue != null) {
        try {
          return Integer.parseInt(strValue);
        }
        catch (NumberFormatException e) {
          logger.warn("NumberFormatException: can't parse to int [propertyName="
                      + propertyName + " stringValue=" + strValue + " defaultValue=" + defaultValue + ")");
          return defaultValue;
        }
      }
    }
    return defaultValue;
  }
}
