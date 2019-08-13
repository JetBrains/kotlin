// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchTask;

import com.intellij.find.FindModel;

import java.lang.reflect.Field;

public class SearchTaskOptions implements Cloneable {

  public static final int DEFAULT_CRITICAL_AMOUNT_OF_SEARCH_RESULTS = 1000;
  public static final int NO_LIMIT = -1;

  public String stringToFind;

  public boolean onlyOnePageSearch = false;
  public boolean loopedPhase = false;

  public long leftBoundPageNumber = NO_LIMIT;
  public int leftBoundCaretPageOffset = NO_LIMIT;
  public long rightBoundPageNumber = NO_LIMIT;
  public int rightBoundCaretPageOffset = NO_LIMIT;

  public boolean searchForwardDirection = true;

  public boolean caseSensitive = false;
  public boolean wholeWords = false;

  public int contextOneSideLength = 0;
  public int criticalAmountOfSearchResults = DEFAULT_CRITICAL_AMOUNT_OF_SEARCH_RESULTS;


  public SearchTaskOptions setStringToFind(String stringToFind) {
    this.stringToFind = stringToFind;
    return this;
  }

  public SearchTaskOptions setOnlyOnePageSearch(boolean onlyOnePageSearch) {
    this.onlyOnePageSearch = onlyOnePageSearch;
    return this;
  }

  public SearchTaskOptions setSearchBounds(long leftBoundPageNumber, int leftBoundCaretPageOffset,
                                           long rightBoundPageNumber, int rightBoundCaretPageOffset) {
    this.leftBoundPageNumber = leftBoundPageNumber;
    this.leftBoundCaretPageOffset = leftBoundCaretPageOffset;
    this.rightBoundPageNumber = rightBoundPageNumber;
    this.rightBoundCaretPageOffset = rightBoundCaretPageOffset;
    return this;
  }

  public SearchTaskOptions setCaseSensetive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  public SearchTaskOptions setWholeWords(boolean wholeWords) {
    this.wholeWords = wholeWords;
    return this;
  }

  public SearchTaskOptions setSearchDirectionForward(boolean forward) {
    this.searchForwardDirection = forward;
    return this;
  }

  public SearchTaskOptions setContextOneSideLength(int contextOneSideLength) {
    this.contextOneSideLength = contextOneSideLength;
    return this;
  }

  public SearchTaskOptions setCriticalAmountOfSearchResults(int criticalAmountOfSearchResults) {
    this.criticalAmountOfSearchResults = criticalAmountOfSearchResults;
    return this;
  }

  @Override
  public SearchTaskOptions clone() throws CloneNotSupportedException {
    return (SearchTaskOptions)super.clone();
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    Field[] fields = this.getClass().getDeclaredFields();

    stringBuilder.append(getClass().getName());
    stringBuilder.append(": {");

    for (Field field : fields) {
      stringBuilder.append(field.getName());
      stringBuilder.append("=");
      try {
        stringBuilder.append(field.get(this));
      }
      catch (IllegalAccessException e) {
        stringBuilder.append("<Illegal access>");
      }
      stringBuilder.append(", ");
    }


    stringBuilder.append("}");

    return stringBuilder.toString();
  }

  public FindModel generateFindModel() {
    FindModel findModel = new FindModel();
    findModel.setStringToFind(stringToFind);
    findModel.setCaseSensitive(caseSensitive);
    findModel.setWholeWordsOnly(wholeWords);
    return findModel;
  }
}
