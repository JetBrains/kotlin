// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchResultsPanel;

import com.intellij.diagnostic.ThreadDumper;
import com.intellij.largeFilesEditor.editor.Page;
import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.largeFilesEditor.search.actions.FindFurtherAction;
import com.intellij.largeFilesEditor.search.actions.StopRangeSearchAction;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskOptions;
import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.util.ui.UIUtil;
import org.junit.Assert;

import java.util.Iterator;
import java.util.List;

public class RangeSearchDuplicateResultsTest extends LightPlatformTestCase {

  private static final String FILE_NAME = "testFileName.test";
  private static final String EVERY_PAGE_TEXT = "aa";
  private static final long PAGES_AMOUNT = 10;
  private static final long PAGE_NUMBER_TO_STOP = 6;

  private FileDataProviderForSearch myFileDataProviderForSearch;
  private RangeSearch myRangeSearch;
  private StopRangeSearchAction myStopRangeSearchAction;
  private FindFurtherAction myFindFurtherAction;
  private volatile boolean myNeedToAbortSearch;
  private volatile boolean isAllDoneAsExpected;

  public void test() {
    myFileDataProviderForSearch = new MyFileDataProviderForSearch();
    myRangeSearch = new RangeSearch(new MockVirtualFile(FILE_NAME), getProject(), new MyRangeSearchCallback());

    myStopRangeSearchAction = new StopRangeSearchAction(myRangeSearch);
    myFindFurtherAction = new FindFurtherAction(true, myRangeSearch);

    SearchTaskOptions options = new SearchTaskOptions();
    options.setStringToFind("a");

    myNeedToAbortSearch = true;
    myRangeSearch.addEdtRangeSearchEventsListener(new RangeSearch.EdtRangeSearchEventsListener() {
      @Override
      public void onSearchStopped() {
        onStopped(this);
      }

      @Override
      public void onSearchFinished() {
        Assert.fail("Was called onSearchFinished(), however shouldn't be");
      }
    });

    isAllDoneAsExpected = false;
    myRangeSearch.runNewSearch(options, myFileDataProviderForSearch);
    UIUtil.dispatchAllInvocationEvents();

    /* This line shouldn't be executed before onFinished() will be done.
     * See com.intellij.openapi.progress.impl.CoreProgressManager#run,
     * where in tests "task.isHeadless()" will be true.
     * This causes "strange" order of execution of EDT-tasks.
     */
    if (!isAllDoneAsExpected) {
      Assert.fail("wrong order of tasks execution\n" + ThreadDumper.dumpThreadsToString());
    }
  }

  private void onStopped(RangeSearch.EdtRangeSearchEventsListener listener) {
    myNeedToAbortSearch = false;
    myRangeSearch.removeEdtRangeSearchEventsListener(listener);
    myRangeSearch.addEdtRangeSearchEventsListener(new RangeSearch.EdtRangeSearchEventsListener() {
      @Override
      public void onSearchStopped() {
        Assert.fail("Was called onSearchStopped(), however shouldn't be");
      }

      @Override
      public void onSearchFinished() {
        onFinished();
      }
    });
    myFindFurtherAction.actionPerformed(ActionUtil.createEmptyEvent());
  }

  private void onFinished() {
    List<SearchResult> results = myRangeSearch.getSearchResultsList();
    Iterator<SearchResult> iterator = results.iterator();
    SearchResult current = iterator.next();
    while (iterator.hasNext()) {
      SearchResult previous = current;
      current = iterator.next();
      boolean isRightOrder = previous.startPosition.pageNumber < current.startPosition.pageNumber
                             || previous.startPosition.pageNumber == current.startPosition.pageNumber
                                && previous.startPosition.symbolOffsetInPage < current.startPosition.symbolOffsetInPage;
      if (!isRightOrder) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Order check is failed at ([%s] , [%s])", previous.toString(), current.toString()));
        message.append("\nAll search results:");
        for (SearchResult result : results) {
          message.append("\n  ").append(result);
        }
        Assert.fail(message.toString());
        return;
      }
    }
    isAllDoneAsExpected = true;
  }

  private class MyRangeSearchCallback implements RangeSearchCallback {
    @Override
    public FileDataProviderForSearch getFileDataProviderForSearch(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
      LOG.info("called getFileDataProviderForSearch");
      return myFileDataProviderForSearch;
    }

    @Override
    public void showResultInEditor(SearchResult searchResult, Project project, VirtualFile virtualFile) { }
  }

  private class MyFileDataProviderForSearch implements FileDataProviderForSearch {
    @Override
    public long getPagesAmount() {
      return PAGES_AMOUNT;
    }

    @Override
    public Page getPage_wait(long pageNumber) {
      if (myNeedToAbortSearch && pageNumber == PAGE_NUMBER_TO_STOP) {
        myStopRangeSearchAction.actionPerformed(ActionUtil.createEmptyEvent());
      }
      //sleep(100);
      return new Page(EVERY_PAGE_TEXT, pageNumber);
    }

    @Override
    public String getName() {
      return FILE_NAME;
    }
  }
}
