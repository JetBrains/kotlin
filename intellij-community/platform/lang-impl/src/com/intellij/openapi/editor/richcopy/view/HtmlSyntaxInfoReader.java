// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.richcopy.view;

import com.intellij.openapi.editor.richcopy.model.ColorRegistry;
import com.intellij.openapi.editor.richcopy.model.FontNameRegistry;
import com.intellij.openapi.editor.richcopy.model.MarkupHandler;
import com.intellij.openapi.editor.richcopy.model.SyntaxInfo;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ui.UIUtil;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author Denis Zhdanov
 */
public class HtmlSyntaxInfoReader extends AbstractSyntaxAwareReader implements MarkupHandler {

  private final int myTabSize;
  protected StringBuilder    myResultBuffer;
  private ColorRegistry    myColorRegistry;
  private FontNameRegistry myFontNameRegistry;
  private int myMaxLength;

  private int     myDefaultForeground;
  private int     myDefaultBackground;
  private int     myDefaultFontFamily;
  private int     myForeground;
  private int     myBackground;
  private int     myFontFamily;
  private boolean myBold;
  private boolean myItalic;
  private int     myCurrentColumn;

  private final TIntObjectHashMap<String> myColors = new TIntObjectHashMap<>();

  public HtmlSyntaxInfoReader(@NotNull SyntaxInfo syntaxInfo, int tabSize) {
    super(syntaxInfo);
    myTabSize = tabSize;
  }

  @Override
  protected void build(@NotNull StringBuilder holder, int maxLength) {
    myResultBuffer = holder;
    myColorRegistry = mySyntaxInfo.getColorRegistry();
    myFontNameRegistry = mySyntaxInfo.getFontNameRegistry();
    myDefaultForeground = myForeground = mySyntaxInfo.getDefaultForeground();
    myDefaultBackground = myBackground = mySyntaxInfo.getDefaultBackground();
    myBold = myItalic = false;
    myCurrentColumn = 0;
    myMaxLength = maxLength;
    try {
      buildColorMap();
      appendStartTags();

      mySyntaxInfo.processOutputInfo(this);

      appendCloseTags();
    }
    finally {
      myResultBuffer = null;
      myColorRegistry = null;
      myFontNameRegistry = null;
      myColors.clear();
    }
  }

  protected void appendCloseTags() {
    myResultBuffer.append("</pre></body></html>");
  }

  protected void appendStartTags() {
    myResultBuffer.append("<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body>")
                  .append("<pre style=\"background-color:");
    appendColor(myResultBuffer, myDefaultBackground);
    myResultBuffer.append(";color:");
    appendColor(myResultBuffer, myDefaultForeground);
    myResultBuffer.append(';');
    int[] fontIds = myFontNameRegistry.getAllIds();
    if (fontIds.length > 0) {
      myFontFamily = myDefaultFontFamily = fontIds[0];
      appendFontFamilyRule(myResultBuffer, myDefaultFontFamily);
    }
    else {
      myFontFamily = myDefaultFontFamily = -1;
    }
    float fontSize = mySyntaxInfo.getFontSize();
    // on Mac OS font size in points declared in HTML doesn't mean the same value as when declared e.g. in TextEdit (and in Java),
    // this is the correction factor
    if (SystemInfo.isMac) fontSize *= 0.75f;
    myResultBuffer.append(String.format("font-size:%.1fpt;\">", fontSize));
  }

  protected void appendFontFamilyRule(@NotNull StringBuilder styleBuffer, int fontFamilyId) {
    styleBuffer.append("font-family:'").append(myFontNameRegistry.dataById(fontFamilyId)).append("';");
  }

  private static void defineBold(@NotNull StringBuilder styleBuffer) {
    styleBuffer.append("font-weight:bold;");
  }

  private static void defineItalic(@NotNull StringBuilder styleBuffer) {
    styleBuffer.append("font-style:italic;");
  }

  private void defineForeground(int id, @NotNull StringBuilder styleBuffer) {
    styleBuffer.append("color:");
    appendColor(styleBuffer, id);
    styleBuffer.append(";");
  }

  protected void defineBackground(int id, @NotNull StringBuilder styleBuffer) {
    styleBuffer.append("background-color:");
    appendColor(styleBuffer, id);
    styleBuffer.append(";");
  }

  private void appendColor(StringBuilder builder, int id) {
    builder.append(myColors.get(id));
  }

  private void buildColorMap() {
    for (int id : myColorRegistry.getAllIds()) {
      StringBuilder b = new StringBuilder("#");
      UIUtil.appendColor(myColorRegistry.dataById(id), b);
      myColors.put(id, b.toString());
    }
  }

  @Override
  public void handleText(int startOffset, int endOffset) {
    boolean formattedText = myForeground != myDefaultForeground || myBackground != myDefaultBackground || myFontFamily != myDefaultFontFamily || myBold || myItalic;
    if (!formattedText) {
      escapeAndAdd(startOffset, endOffset);
      return;
    }

    myResultBuffer.append("<span style=\"");
    if (myForeground != myDefaultForeground) {
      defineForeground(myForeground, myResultBuffer);
    }
    if (myBackground != myDefaultBackground) {
      defineBackground(myBackground, myResultBuffer);
    }
    if (myBold) {
      defineBold(myResultBuffer);
    }
    if (myItalic) {
      defineItalic(myResultBuffer);
    }
    if (myFontFamily != myDefaultFontFamily) {
      appendFontFamilyRule(myResultBuffer, myFontFamily);
    }
    myResultBuffer.append("\">");
    escapeAndAdd(startOffset, endOffset);
    myResultBuffer.append("</span>");
  }

  private void escapeAndAdd(int start, int end) {
    for (int i = start; i < end; i++) {
      char c = myRawText.charAt(i);
      switch (c) {
        case '<': myResultBuffer.append("&lt;"); break;
        case '>': myResultBuffer.append("&gt;"); break;
        case '&': myResultBuffer.append("&amp;"); break;
        case ' ': myResultBuffer.append("&#32;"); break;
        case '\n': myResultBuffer.append("<br>"); myCurrentColumn = 0; break;
        case '\t':
          int newColumn = (myCurrentColumn / myTabSize + 1) * myTabSize;
          for (; myCurrentColumn < newColumn; myCurrentColumn++) myResultBuffer.append("&#32;");
          break;
        default: myResultBuffer.append(c);
      }
      myCurrentColumn++;
    }
  }

  @Override
  public void handleForeground(int foregroundId) throws Exception {
    myForeground = foregroundId;
  }

  @Override
  public void handleBackground(int backgroundId) throws Exception {
    myBackground = backgroundId;
  }

  @Override
  public void handleFont(int fontNameId) throws Exception {
    myFontFamily = fontNameId;
  }

  @Override
  public void handleStyle(int style) throws Exception {
    myBold = (Font.BOLD & style) != 0;
    myItalic = (Font.ITALIC & style) != 0;
  }

  @Override
  public boolean canHandleMore() {
    if (myResultBuffer.length() > myMaxLength) {
      myResultBuffer.append("... truncated ...");
      return false;
    }
    return true;
  }
}
