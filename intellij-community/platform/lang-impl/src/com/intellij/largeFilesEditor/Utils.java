// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor;

public class Utils {

  public static int calculatePagePositionPercent(long currentPageNumber, long pageAmount) {
    int progressValue;
    if (currentPageNumber == 0) {
      progressValue = 0;
    }
    else if (currentPageNumber == pageAmount - 1) {
      progressValue = 100;
    }
    else {
      progressValue = (int)(1 + 99 * (currentPageNumber - 1) / (pageAmount - 1 - 1)); // magic formula
    }
    return progressValue;
  }
}
