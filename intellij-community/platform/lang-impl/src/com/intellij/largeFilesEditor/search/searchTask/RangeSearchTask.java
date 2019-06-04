// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchTask;

import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.util.ArrayList;

public class RangeSearchTask extends SearchTaskBase {
  private static final Logger logger = Logger.getInstance(RangeSearchTask.class);
  private ProgressIndicator myProgressIndicator;

  public RangeSearchTask(SearchTaskOptions options,
                         Project project,
                         FileDataProviderForSearch fileDataProviderForSearch,
                         SearchTaskCallback callback) {
    super(options, project, fileDataProviderForSearch, callback);
  }

  public String getTitleForBackgroundableTask() {
    final int maxStrToFindLength = 16;
    final int maxFileNameLength = 20;

    String strToFind = cutToMaxLength(
      options.stringToFind, maxStrToFindLength);
    String fileName = cutToMaxLength(
      fileDataProviderForSearch.getName(), maxFileNameLength);

    return String.format("Searching for \"%s\" in file \"%s\"", strToFind, fileName);
  }

  public void setProgressIndicator(ProgressIndicator progressIndicator) {
    myProgressIndicator = progressIndicator;
  }

  @Override
  protected void doRun() {
    FrameSearcher searcher;
    String prevPageText;
    String curPageText;
    String nextPageText;
    String tailText;
    char prefixSymbol;
    char postfixSymbol;
    long pagesAmount;
    int tailLength;
    long curPageNumber;
    //int offset;
    ArrayList<SearchResult> allMatchesAtFrame;

    searcher = createFrameSearcher(options, project);
    tailLength = options.stringToFind.length() - 1;

    try {
      /* preparing init data... */
      pagesAmount = fileDataProviderForSearch.getPagesAmount();
      curPageNumber = getPageNumberForBeginning(pagesAmount, options);

      /* checking if it is the end... */
      if (isTheEndOfSearchingCycle(curPageNumber, pagesAmount, options)) {
        callback.tellSearchIsFinished(this, curPageNumber);
        return;
      }

      prevPageText = curPageNumber > 0 ?
                     fileDataProviderForSearch.getPage_wait(curPageNumber - 1).getText() : "";
      curPageText = fileDataProviderForSearch.getPage_wait(curPageNumber).getText();
      nextPageText = curPageNumber < pagesAmount - 1 ?
                     fileDataProviderForSearch.getPage_wait(curPageNumber + 1).getText() : "";
      tailText = getTailFromPage(nextPageText, tailLength);
      prefixSymbol = getPrefixSymbol(prevPageText);
      postfixSymbol = getPostfixSymbol(nextPageText, tailLength);


      while (true) {
        callback.tellSearchProgress(this, curPageNumber, pagesAmount);

        /* searching result in current page */
        searcher.setFrame(curPageNumber, prefixSymbol, curPageText, tailText, postfixSymbol);
        allMatchesAtFrame = searcher.findAllMatchesAtFrame();
        if (allMatchesAtFrame.size() > 0) {
          callback.tellFrameSearchResultsFound(this, allMatchesAtFrame);
        }

        if (isShouldStop()) {
          if (myProgressIndicator != null) {
            myProgressIndicator.cancel();
          }
          callback.tellSearchWasStopped(this, curPageNumber);
          return;
        }
        if (myProgressIndicator != null && myProgressIndicator.isCanceled()) {
          this.shouldStop();
          callback.tellSearchWasStopped(this, curPageNumber);
          return;
        }


        /* preparing data for searching in next page if it's possible*/
        pagesAmount = fileDataProviderForSearch.getPagesAmount();
        if (options.searchForwardDirection) {
          curPageNumber++;
          prevPageText = curPageText;
          curPageText = nextPageText;
          nextPageText = curPageNumber < pagesAmount - 1 ?
                         fileDataProviderForSearch.getPage_wait(curPageNumber + 1).getText() : "";
        }
        else {
          curPageNumber--;
          nextPageText = curPageText;
          curPageText = prevPageText;
          prevPageText = curPageNumber > 0 ?
                         fileDataProviderForSearch.getPage_wait(curPageNumber - 1).getText() : "";
        }

        /* checking if it is the end... */
        if (isTheEndOfSearchingCycle(curPageNumber, pagesAmount, options)) {
          callback.tellSearchIsFinished(this, getPreviousPageNumber(curPageNumber, options));
          return;
        }
        if (isShouldStop()) {
          if (myProgressIndicator != null) {
            myProgressIndicator.cancel();
          }
          callback.tellSearchWasStopped(this, curPageNumber);
          return;
        }
        if (myProgressIndicator != null && myProgressIndicator.isCanceled()) {
          this.shouldStop();
          callback.tellSearchWasStopped(this, curPageNumber);
          return;
        }

        /* preparing addictive data */
        tailText = getTailFromPage(nextPageText, tailLength);
        prefixSymbol = getPrefixSymbol(prevPageText);
        postfixSymbol = getPostfixSymbol(nextPageText, tailLength);
      }
    }
    catch (IOException e) {
      logger.warn(e);
      callback.tellSearchWasCatchedException(this, e);
    }
  }

  private static String cutToMaxLength(String whatToCut, int maxLength) {
    if (whatToCut.length() > maxLength) {
      return whatToCut.substring(0, maxLength / 2 - 1) + "..." +
             whatToCut.substring(whatToCut.length() - 1 - maxLength / 2);
    }
    else {
      return whatToCut;
    }
  }
}
