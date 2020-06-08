// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.richcopy;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Java logical font names (like 'Monospaced') don't necessarily make sense for other applications, so we try to map those fonts to
 * the corresponding physical font names.
 */
public final class FontMapper {
  private static final Logger LOG = Logger.getInstance(FontMapper.class);

  private static final String[] logicalFontsToMap = {Font.DIALOG, Font.DIALOG_INPUT, Font.MONOSPACED, Font.SERIF, Font.SANS_SERIF};
  private static final Map<String, String> logicalToPhysicalMapping = new HashMap<>();
  private static final Map<String, Boolean> monospacedMapping = new HashMap<>();

  static {
    try {
      Object fontManager = Class.forName("sun.font.FontManagerFactory").getMethod("getInstance").invoke(null);
      Method findFontMethod = Class.forName("sun.font.FontManager").getMethod("findFont2D", String.class, int.class, int.class);
      for (String logicalFont : logicalFontsToMap) {
        Object font2D = findFontMethod.invoke(fontManager, logicalFont, Font.PLAIN, 0);
        if (font2D == null) {
          continue;
        }
        String fontClassName = font2D.getClass().getName();
        String physicalFont = null;
        if ("sun.font.CompositeFont".equals(fontClassName)) { // Windows and Linux case
          Object physicalFontObject = Class.forName("sun.font.CompositeFont").getMethod("getSlotFont", int.class).invoke(font2D, 0);
          physicalFont = (String)Class.forName("sun.font.Font2D").getMethod("getFamilyName", Locale.class).invoke(physicalFontObject, Locale.getDefault());
        }
        else if ("sun.font.CFont".equals(fontClassName)) { // MacOS case
          physicalFont = ReflectionUtil.getField(Class.forName("sun.font.CFont"), font2D, String.class, "nativeFontName");
        }
        if (physicalFont != null) {
          logicalToPhysicalMapping.put(logicalFont, physicalFont);
        }
      }
    }
    catch (Throwable e) {
      LOG.warn("Failed to determine logical to physical font mappings", e);
    }
  }

  public static
  @NotNull
  String getPhysicalFontName(@NotNull String logicalFontName) {
    String mapped = logicalToPhysicalMapping.get(logicalFontName);
    return mapped == null ? logicalFontName : mapped;
  }

  public static boolean isMonospaced(@NotNull String fontName) {
    Boolean result = monospacedMapping.get(fontName);
    if (result == null) {
      FontMetrics metrics = FontInfo.getFontMetrics(new Font(fontName, Font.PLAIN, FontPreferences.DEFAULT_FONT_SIZE),
                                                    new FontRenderContext(null, false, false));
      result = metrics.charWidth('l') == metrics.charWidth('W');
      monospacedMapping.put(fontName, result);
    }
    return result;
  }
}
