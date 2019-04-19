package com.intellij.execution.filters;

import org.jetbrains.annotations.NotNull;

/**
 * @deprecated use {@link FileHyperlinkRawData} instead
 */
@Deprecated
public class FileHyperlinkParsedData {

  private final String myFilePath;
  private final int myDocumentLine;
  private final int myDocumentColumn;
  private final int myHyperlinkStartOffset;
  private final int myHyperlinkEndOffset;

  public FileHyperlinkParsedData(@NotNull String filePath,
                                 int documentLine,
                                 int documentColumn,
                                 int hyperlinkStartOffset,
                                 int hyperlinkEndOffset) {
    myFilePath = filePath;
    myDocumentLine = documentLine;
    myDocumentColumn = documentColumn;
    myHyperlinkStartOffset = hyperlinkStartOffset;
    myHyperlinkEndOffset = hyperlinkEndOffset;
  }

  @NotNull
  public String getFilePath() {
    return myFilePath;
  }

  public int getDocumentLine() {
    return myDocumentLine;
  }

  public int getDocumentColumn() {
    return myDocumentColumn;
  }

  public int getHyperlinkStartOffset() {
    return myHyperlinkStartOffset;
  }

  public int getHyperlinkEndOffset() {
    return myHyperlinkEndOffset;
  }
}
