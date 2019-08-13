// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchTask;

import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.largeFilesEditor.search.SearchResult;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;

public class FrameSearcherTest {

  private static char ELLIPSIS = '\u2026';

  @Test
  public void findAllMatchesAtFrame() {
    String TEXT_MAIN = "XXXX string str";
    String TEXT_TAIL = "ing XXXX";
    String STR_TO_FIND = "string";

    SearchTaskOptions options;
    FrameSearcher frameSearcher;

    options = new SearchTaskOptions();
    options.setStringToFind(STR_TO_FIND);
    options.setContextOneSideLength(0);

    frameSearcher = new FrameSearcher(options, new MockSmartStringSearcher());

    frameSearcher.setFrame(100, ' ', TEXT_MAIN, TEXT_TAIL, ' ');

    ArrayList<SearchResult> allMatchesAtFrame = frameSearcher.findAllMatchesAtFrame();
    assertArrayEquals(
      new Object[]{
        new SearchResult(
          100, 5,
          100, 11,
          ELLIPSIS + "", STR_TO_FIND, "" + ELLIPSIS),
        new SearchResult(
          100, 12,
          101, 3,
          ELLIPSIS + "", STR_TO_FIND, "" + ELLIPSIS),
      }, allMatchesAtFrame.toArray());
  }


  private class MockSmartStringSearcher implements FrameSearcher.SmartStringSearcher {

    @Override
    public FindResult findString(String frameText, int offset, FindModel ijFindModel) {
      String where = frameText.substring(offset);
      String what = ijFindModel.getStringToFind();

      int relativeIndex = where.indexOf(what);

      if (relativeIndex < 0) {
        return new FindResult(0, 0) {
          @Override
          public boolean isStringFound() {
            return false;
          }
        };
      }
      else {
        return new FindResult(offset + relativeIndex, offset + relativeIndex + what.length()) {
          @Override
          public boolean isStringFound() {
            return true;
          }
        };
      }
    }
  }
}