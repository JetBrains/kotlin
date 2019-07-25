// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchTask;

import com.intellij.find.FindManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;


public abstract class SearchTaskBase implements Runnable {

  protected final FileDataProviderForSearch fileDataProviderForSearch;
  protected final SearchTaskCallback callback;
  protected final SearchTaskOptions options;
  protected final Project project;

  private volatile boolean shouldStop = false;

  private boolean isFinished = false;

  public SearchTaskBase(SearchTaskOptions options,
                        Project project,
                        FileDataProviderForSearch fileDataProviderForSearch,
                        SearchTaskCallback callback) {
    this.project = project;
    this.callback = callback;
    this.options = options;
    this.fileDataProviderForSearch = fileDataProviderForSearch;
  }

  public SearchTaskOptions getOptions() {
    return options;
  }

  @Override
  public final void run() {
    doRun();
    isFinished = true;
  }

  public final void shouldStop() {
    shouldStop = true;
  }

  public final boolean isShouldStop() {
    return shouldStop;
  }

  public final boolean isFinished() {
    return isFinished;
  }

  protected abstract void doRun();

  static long getPageNumberForBeginning(long pagesAmount, SearchTaskOptions options) {
    if (options.searchForwardDirection) {
      if (options.leftBoundPageNumber == -1) {
        return 0;
      }
      else {
        return options.leftBoundPageNumber;
      }
    }
    else {
      if (options.rightBoundPageNumber == -1) {
        return pagesAmount - 1;
      }
      else {
        return options.rightBoundPageNumber;
      }
    }
  }

  static boolean isTheEndOfSearchingCycle(long curPageNumber, long pagesAmount, SearchTaskOptions options) {
    return curPageNumber < 0
           || curPageNumber >= pagesAmount
           || options.rightBoundPageNumber != -1 && curPageNumber > options.rightBoundPageNumber
           || options.leftBoundPageNumber != -1 && curPageNumber < options.leftBoundPageNumber;
  }

  static String getTailFromPage(String nextPageText, int tailLength) {
    return tailLength < nextPageText.length() ?
           nextPageText.substring(0, tailLength) : nextPageText;
  }

  static char getPostfixSymbol(String nextPageText, int tailLength) {
    return tailLength + 1 < nextPageText.length() ?
           nextPageText.charAt(tailLength + 1) : FrameSearcher.NOT_EXISTING_BORDERING_SYMBOL;
  }

  static char getPrefixSymbol(String prevPageText) {
    if (!StringUtil.isEmpty(prevPageText)) {
      return prevPageText.charAt(prevPageText.length() - 1);
    }
    else {
      return FrameSearcher.NOT_EXISTING_BORDERING_SYMBOL;
    }
  }

  static long getPreviousPageNumber(long curPageNumber, SearchTaskOptions options) {
    if (options.searchForwardDirection) {
      return curPageNumber - 1;
    }
    else {
      return curPageNumber + 1;
    }
  }

  protected FrameSearcher createFrameSearcher(SearchTaskOptions options, Project project) {
    return new FrameSearcher(options,
                             (frameText, offset, ijFindModel) ->
                               FindManager.getInstance(project).findString(frameText, offset, ijFindModel));
  }
}
