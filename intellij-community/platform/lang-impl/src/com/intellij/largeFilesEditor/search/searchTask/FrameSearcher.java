// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchTask;


import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.largeFilesEditor.search.Position;
import com.intellij.largeFilesEditor.search.SearchResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Class, that finds all occurrences in certain frame.
 * Frame is consisted of two strings, that can be set for this class.
 */
class FrameSearcher {
  /**
   * Use this field if bordering symbol (beforeFrameStartSymbol, afterFrameEndSymbol) doesn't exist
   */
  static char NOT_EXISTING_BORDERING_SYMBOL = '\n';
  static char ELLIPSIS = '\u2026';

  private final SmartStringSearcher smartStringSearcher;

  private final SearchTaskOptions options;

  private final FindModel ijFindModel;
  private final Position resultStartPos;
  private final Position resultEndPos;

  private long curPageNumber;
  private String curPageText;
  private String tailText;
  private char beforeFrameStartSymbol;
  private char afterFrameEndSymbol;


  FrameSearcher(SearchTaskOptions options, SmartStringSearcher smartStringSearcher) {
    this.smartStringSearcher = smartStringSearcher;
    this.options = options;

    ijFindModel = options.generateFindModel();

    resultStartPos = new Position();
    resultEndPos = new Position();
  }

  void setFrame(long curPageNumber, char beforeFrameStartSymbol,
                String curPageText, String tailText, char afterFrameEndSymbol) {
    this.curPageNumber = curPageNumber;
    this.beforeFrameStartSymbol = beforeFrameStartSymbol;
    this.curPageText = curPageText;
    this.tailText = tailText;
    this.afterFrameEndSymbol = afterFrameEndSymbol;
  }

  @NotNull
  ArrayList<SearchResult> findAllMatchesAtFrame() {
    String frameText;
    FindResult ijFindResult;
    int offset;
    String contextPrefix;
    String foundString;
    String contextPostfix;
    ArrayList<SearchResult> resultsList;

    frameText = beforeFrameStartSymbol + curPageText + tailText + afterFrameEndSymbol;
    offset = 1;  // the prefix symbol can't be a part of any search result
    resultsList = new ArrayList<>();

    while (true) {
      ijFindResult = smartStringSearcher.findString(frameText, offset, ijFindModel);

      if (!ijFindResult.isStringFound()) {
        return resultsList;
      }

      calculateAndResetStartResultPosition(resultStartPos, ijFindResult);
      calculateAndResetEndResultPosition(resultEndPos, ijFindResult);
      contextPrefix = calculateContextPrefix(frameText, ijFindResult, options);
      foundString = calculateFoundString(frameText, ijFindResult);
      contextPostfix = calculateContextPostfix(frameText, ijFindResult, options);

      if (ijFindResult.getEndOffset() != frameText.length()) { // the postfix symbol can't be a part of any search result
        resultsList.add(new SearchResult(
          resultStartPos.pageNumber,
          resultStartPos.symbolOffsetInPage,
          resultEndPos.pageNumber,
          resultEndPos.symbolOffsetInPage,
          contextPrefix,
          foundString,
          contextPostfix));
      }

      offset = ijFindResult.getEndOffset();
    }
  }

  private static String calculateFoundString(String frameText, FindResult ijFindResult) {
    return frameText.substring(ijFindResult.getStartOffset(), ijFindResult.getEndOffset());
  }

  private static String calculateContextPrefix(String frameText, FindResult ijFindResult, SearchTaskOptions options) {
    int contextStartOffset = ijFindResult.getStartOffset();
    int minAllowedContextStartOffset = ijFindResult.getStartOffset() - options.contextOneSideLength;
    if (minAllowedContextStartOffset < 0) {
      minAllowedContextStartOffset = 0;
    }

    char _char;
    while (contextStartOffset > minAllowedContextStartOffset) {
      _char = frameText.charAt(contextStartOffset - 1);
      if (_char != '\r' && _char != '\n') {
        contextStartOffset--;
      }
      else {
        break;
      }
    }

    boolean ellipsis = false;
    if (contextStartOffset > 0) {
      _char = frameText.charAt(contextStartOffset - 1);
      if (_char != '\n' && _char != '\r') {
        ellipsis = true;
      }
    }

    String str = frameText.substring(contextStartOffset, ijFindResult.getStartOffset());
    str = str.replace('\t', ' ');
    if (ellipsis) {
      str = ELLIPSIS + str;
    }
    return str;
  }

  private static String calculateContextPostfix(String frameText, FindResult ijFindResult, SearchTaskOptions options) {
    int contextEndOffset = ijFindResult.getEndOffset();
    int maxAllowedContextEndOffset = ijFindResult.getEndOffset() + options.contextOneSideLength;
    if (maxAllowedContextEndOffset > frameText.length()) {
      maxAllowedContextEndOffset = frameText.length();
    }

    char _char;
    while (contextEndOffset < maxAllowedContextEndOffset) {
      _char = frameText.charAt(contextEndOffset);
      if (_char != '\r' && _char != '\n') {
        contextEndOffset++;
      }
      else {
        break;
      }
    }

    boolean ellipsis = false;
    if (contextEndOffset < frameText.length()) {
      _char = frameText.charAt(contextEndOffset);
      if (_char != '\n' && _char != '\r') {
        ellipsis = true;
      }
    }

    String str = frameText.substring(ijFindResult.getEndOffset(), contextEndOffset);
    str = str.replace('\t', ' ');
    if (ellipsis) {
      str += ELLIPSIS;
    }
    return str;
  }


  private void calculateAndResetStartResultPosition(Position resultStartPos, FindResult ijFindResult) {
    if (ijFindResult.getStartOffset() - 1 < curPageText.length()) { // "-1" because of beforeFrameStartSymbol in frame
      resultStartPos.pageNumber = curPageNumber;
      resultStartPos.symbolOffsetInPage = ijFindResult.getStartOffset() - 1; // "-1" because of beforeFrameStartSymbol in frame
    }
    else {
      resultStartPos.pageNumber = curPageNumber + 1;
      resultStartPos.symbolOffsetInPage =
        ijFindResult.getStartOffset() - curPageText.length() - 1; // "-1" because of beforeFrameStartSymbol in frame
    }
  }

  private void calculateAndResetEndResultPosition(Position resultEndPos, FindResult ijFindResult) {
    if (ijFindResult.getEndOffset() - 1 <= curPageText.length()) { // "-1" because of beforeFrameStartSymbol in frame
      resultEndPos.pageNumber = curPageNumber;
      resultEndPos.symbolOffsetInPage = ijFindResult.getEndOffset() - 1; // "-1" because of beforeFrameStartSymbol in frame
    }
    else {
      resultEndPos.pageNumber = curPageNumber + 1;
      resultEndPos.symbolOffsetInPage =
        ijFindResult.getEndOffset() - curPageText.length() - 1; // "-1" because of beforeFrameStartSymbol in frame
    }
  }

  public interface SmartStringSearcher {

    FindResult findString(String frameText, int offset, FindModel ijFindModel);
  }
}
