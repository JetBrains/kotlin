// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeEditor.printing;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@State(name = "PrintSettings", storages = @Storage("print.xml"))
public class PrintSettings implements PersistentStateComponent<PrintSettings> {
  @NonNls public String PAPER_SIZE = "A4";

  public boolean COLOR_PRINTING = false;
  public boolean SYNTAX_PRINTING = true;
  public boolean PRINT_AS_GRAPHICS = true;

  public boolean PORTRAIT_LAYOUT = true;

  @NonNls public String FONT_NAME = EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName();
  public int FONT_SIZE = EditorColorsManager.getInstance().getGlobalScheme().getEditorFontSize();

  public boolean PRINT_LINE_NUMBERS = true;

  public boolean WRAP = true;

  public float TOP_MARGIN = 0.5f;
  public float BOTTOM_MARGIN = 1.0f;
  public float LEFT_MARGIN = 1.0f;
  public float RIGHT_MARGIN = 1.0f;

  public boolean DRAW_BORDER = true;

  public boolean EVEN_NUMBER_OF_PAGES = false;

  public String FOOTER_HEADER_TEXT1 = CodeEditorBundle.message("print.header.default.line.1");
  public String FOOTER_HEADER_PLACEMENT1 = HEADER;
  public String FOOTER_HEADER_ALIGNMENT1 = LEFT;
  public String FOOTER_HEADER_TEXT2 = CodeEditorBundle.message("print.header.default.line.2");
  public String FOOTER_HEADER_PLACEMENT2 = FOOTER;
  public String FOOTER_HEADER_ALIGNMENT2 = CENTER;
  public int FOOTER_HEADER_FONT_SIZE = 8;
  @NonNls public String FOOTER_HEADER_FONT_NAME = "Arial";

  public static final int PRINT_FILE = 1;
  public static final int PRINT_SELECTED_TEXT = 2;
  public static final int PRINT_DIRECTORY = 4;
  private int myPrintScope;
  private boolean myIncludeSubdirectories;

  @NonNls public static final String HEADER = "Header";
  @NonNls public static final String FOOTER = "Footer";

  @NonNls public static final String LEFT = "Left";
  @NonNls public static final String CENTER = "Center";
  @NonNls public static final String RIGHT = "Right";

  public static PrintSettings getInstance() {
    return ServiceManager.getService(PrintSettings.class);
  }

  public int getPrintScope() {
    return myPrintScope;
  }

  public void setPrintScope(int printScope) {
    myPrintScope = printScope;
  }

  public boolean isIncludeSubdirectories() {
    return myIncludeSubdirectories;
  }

  public void setIncludeSubdirectories(boolean includeSubdirectories) {
    myIncludeSubdirectories = includeSubdirectories;
  }

  @Override
  public PrintSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull final PrintSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
