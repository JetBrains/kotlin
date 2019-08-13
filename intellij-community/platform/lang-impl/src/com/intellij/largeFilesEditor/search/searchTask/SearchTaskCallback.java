// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchTask;

import com.intellij.largeFilesEditor.search.SearchResult;

import java.io.IOException;
import java.util.ArrayList;

public interface SearchTaskCallback {

  void tellSearchIsFinished(SearchTaskBase caller, long curPageNumber);

  void tellSearchProgress(SearchTaskBase caller, long curPageNumber, long pagesAmount);

  void tellFrameSearchResultsFound(RangeSearchTask caller, ArrayList<SearchResult> allMatchesAtFrame);

  void tellSearchWasStopped(SearchTaskBase caller, long curPageNumber);

  void tellSearchWasCatchedException(SearchTaskBase caller, IOException e);

  void tellClosestResultFound(CloseSearchTask caller, ArrayList<SearchResult> allMatchesAtFrame, int index);
}
