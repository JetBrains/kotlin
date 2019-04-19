// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.richcopy.view;

import com.intellij.ide.MacOSApplicationProvider;
import com.intellij.openapi.editor.richcopy.model.ColorRegistry;
import com.intellij.openapi.editor.richcopy.model.FontNameRegistry;
import com.intellij.openapi.editor.richcopy.model.MarkupHandler;
import com.intellij.openapi.editor.richcopy.model.SyntaxInfo;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.datatransfer.DataFlavor;

public class RtfTransferableData extends AbstractSyntaxAwareInputStreamTransferableData {
  public static final int PRIORITY = 100;
  @NotNull public static final DataFlavor FLAVOR = new DataFlavor("text/rtf;class=java.io.InputStream", "RTF text");

  @NotNull private static final String HEADER_PREFIX = "{\\rtf1\\ansi\\deff0";
  @NotNull private static final String HEADER_SUFFIX = "}";
  @NotNull private static final String TAB           = "\\tab\n";
  // using undocumented way to denote line break on Mac (used e.g. by TextEdit) to resolve IDEA-165337
  @NotNull private static final String NEW_LINE      = SystemInfo.isMac ? "\\\n" : "\\line\n";
  @NotNull private static final String BOLD          = "\\b";
  @NotNull private static final String ITALIC        = "\\i";

  public RtfTransferableData(@NotNull SyntaxInfo syntaxInfo) {
    super(syntaxInfo, FLAVOR);
  }

  @Override
  protected void build(@NotNull final StringBuilder holder, final int maxLength) {
    holder.append(HEADER_PREFIX);

    holder.append("{\\colortbl;");
    ColorRegistry colorRegistry = mySyntaxInfo.getColorRegistry();
    for (int id : colorRegistry.getAllIds()) {
      Color color = colorRegistry.dataById(id);
      int[] components = getAdjustedColorComponents(color);
      holder.append(String.format("\\red%d\\green%d\\blue%d;", components[0], components[1], components[2]));
    }
    holder.append("}\n");

    holder.append("{\\fonttbl");
    FontNameRegistry fontNameRegistry = mySyntaxInfo.getFontNameRegistry();
    for (int id : fontNameRegistry.getAllIds()) {
      String fontName = fontNameRegistry.dataById(id);
      holder.append(String.format("{\\f%d %s;}", id, fontName));
    }
    holder.append("}\n");

    holder.append("\n\\s0\\box")
      .append("\\cbpat").append(mySyntaxInfo.getDefaultBackground())
      .append("\\cb").append(mySyntaxInfo.getDefaultBackground())
      .append("\\cf").append(mySyntaxInfo.getDefaultForeground());
    addFontSize(holder, mySyntaxInfo.getFontSize());
    holder.append('\n');

    mySyntaxInfo.processOutputInfo(new MyVisitor(holder, myRawText, mySyntaxInfo, maxLength));

    holder.append("\\par");
    holder.append(HEADER_SUFFIX);
  }
  
  private static int[] getAdjustedColorComponents(Color color) {
    if (SystemInfo.isMac) {
      // on Mac OS color components are expected in Apple's 'Generic RGB' color space
      ColorSpace genericRgbSpace = MacOSApplicationProvider.getInstance().getGenericRgbColorSpace();
      if (genericRgbSpace != null) {
        float[] components = genericRgbSpace.fromRGB(color.getRGBColorComponents(null));
        return new int[] {
          colorComponentFloatToInt(components[0]), 
          colorComponentFloatToInt(components[1]), 
          colorComponentFloatToInt(components[2])
        };
      }
    }
    return new int[] {color.getRed(), color.getGreen(), color.getBlue()};
  }
  
  private static int colorComponentFloatToInt(float component) {
    return (int)(component * 255 + 0.5f);
  }

  @NotNull
  @Override
  protected String getCharset() {
    return "US-ASCII";
  }

  private static void addFontSize(StringBuilder buffer, float fontSize) {
    buffer.append("\\fs").append(Math.round(fontSize * 2));
  }

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  private static class MyVisitor implements MarkupHandler {

    @NotNull private final StringBuilder myBuffer;
    @NotNull private final String        myRawText;
    private final int myMaxLength;

    private final int myDefaultBackgroundId;
    private final float myFontSize;
    private int myForegroundId = -1;
    private int myFontNameId   = -1;
    private int myFontStyle    = -1;

    MyVisitor(@NotNull StringBuilder buffer, @NotNull String rawText, @NotNull SyntaxInfo syntaxInfo, int maxLength) {
      myBuffer = buffer;
      myRawText = rawText;
      myMaxLength = maxLength;

      myDefaultBackgroundId = syntaxInfo.getDefaultBackground();
      myFontSize = syntaxInfo.getFontSize();
    }

    @Override
    public void handleText(int startOffset, int endOffset) throws Exception {
      myBuffer.append("\n");
      for (int i = startOffset; i < endOffset; i++) {
        char c = myRawText.charAt(i);
        if (c > 127) {
          // Escape non-ascii symbols.
          myBuffer.append(String.format("\\u%04d?", (int)c));
          continue;
        }

        switch (c) {
          case '\t':
            myBuffer.append(TAB);
            continue;
          case '\n':
            myBuffer.append(NEW_LINE);
            continue;
          case '\\':
          case '{':
          case '}':
            myBuffer.append('\\');
        }
        myBuffer.append(c);
      }
    }

    @Override
    public void handleBackground(int backgroundId) throws Exception {
      if (backgroundId == myDefaultBackgroundId) {
        myBuffer.append("\\plain"); // we cannot use \chcbpat with default background id, as it doesn't work in MS Word,
                                    // and we cannot use \chcbpat0 as it doesn't work in OpenOffice

        addFontSize(myBuffer, myFontSize);
        if (myFontNameId >= 0) {
          handleFont(myFontNameId);
        }
        if (myForegroundId >= 0) {
          handleForeground(myForegroundId);
        }
        if (myFontStyle >= 0) {
          handleStyle(myFontStyle);
        }
      }
      else {
        myBuffer.append("\\chcbpat").append(backgroundId);
      }
      myBuffer.append("\\cb").append(backgroundId);
      myBuffer.append('\n');
    }

    @Override
    public void handleForeground(int foregroundId) throws Exception {
      myBuffer.append("\\cf").append(foregroundId).append('\n');
      myForegroundId = foregroundId;
    }

    @Override
    public void handleFont(int fontNameId) throws Exception {
      myBuffer.append("\\f").append(fontNameId).append('\n');
      myFontNameId = fontNameId;
    }

    @Override
    public void handleStyle(int style) throws Exception {
      myBuffer.append(ITALIC);
      if ((style & Font.ITALIC) == 0) {
        myBuffer.append('0');
      }
      myBuffer.append(BOLD);
      if ((style & Font.BOLD) == 0) {
        myBuffer.append('0');
      }
      myBuffer.append('\n');
      myFontStyle = style;
    }

    @Override
    public boolean canHandleMore() {
      if (myBuffer.length() > myMaxLength) {
        myBuffer.append("... truncated ...");
        return false;
      }
      return true;
    }
  }
}
