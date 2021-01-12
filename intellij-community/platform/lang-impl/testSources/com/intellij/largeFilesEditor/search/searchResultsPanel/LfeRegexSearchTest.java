// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchResultsPanel;

import com.intellij.largeFilesEditor.editor.Page;
import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskOptions;
import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightPlatformTestCase;
import org.assertj.core.util.Arrays;
import org.junit.Assert;

import java.util.List;

/**
 * The goals of this test are:
 * - To check, that Range Search can work properly in "the same thread" mode
 * - To check, that regex search mode can find a string,
 * that has length less than 1 page length,
 * even if it's located at page break.
 * - To check, that it works as expected.
 * - To check, that the order of search results is right.
 * - To check, that there are not duplicated search results.
 */
public class LfeRegexSearchTest extends LightPlatformTestCase {

  private static final String FILE_NAME = "testFileName.test";
  private static final String[] PAGES = Arrays.array(
    ".... 0aaaa1 0",
    "aaaa0cccc1gg",
    "gg0cccc1zzzz1 ...",
    "... 0pppp1"
  );

  private static final String STRING_TO_FIND = "0\\S+1";

  private static final String[] EXPECTED_FOUND_STRINGS = Arrays.array(
    "0aaaa1",
    "0aaaa0cccc1",
    // here "aaaa0cccc1gggg0cccc1zzzz1" is devided into overlapping "0aaaa0cccc1" and "0cccc1gggg0zzzz1"
    // because of technical features of Range Search of large files
    "0cccc1gggg0cccc1zzzz1",
    "0cccc1zzzz1",
    "0pppp1"
  );

  private FileDataProviderForSearch myFileDataProviderForSearch;
  private RangeSearch myRangeSearch;

  public void test() {
    myFileDataProviderForSearch = new MyFileDataProviderForSearch();
    myRangeSearch = new RangeSearch(new MockVirtualFile(FILE_NAME), getProject(), new MyRangeSearchCallback());

    SearchTaskOptions options = new SearchTaskOptions();
    options.setRegularExpression(true);
    options.setStringToFind(STRING_TO_FIND);

    myRangeSearch.runNewSearch(options, myFileDataProviderForSearch, false);  // "false" turns on "the same thread" mode
    checkResults();
  }

  private void checkResults() {
    List<SearchResult> results = myRangeSearch.getSearchResultsList();

    Assert.assertEquals(EXPECTED_FOUND_STRINGS.length, results.size());

    for (int i = 0; i < EXPECTED_FOUND_STRINGS.length; i++) {
      assertEquals(EXPECTED_FOUND_STRINGS[i], results.get(i).foundString);
    }
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

  private static class MyFileDataProviderForSearch implements FileDataProviderForSearch {
    @Override
    public long getPagesAmount() {
      return PAGES.length;
    }

    @Override
    public Page getPage_wait(long pageNumber) {
      return new Page(PAGES[(int)pageNumber], pageNumber, pageNumber == getPagesAmount() - 1);
    }

    @Override
    public String getName() {
      return FILE_NAME;
    }
  }
}
