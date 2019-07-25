// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.find;

public class FindInEditorFunctionalTest extends AbstractFindInEditorTest {
  public void testNextFromFoldedRegion() {
    init("<caret>first line\nsecond line");
    initFind();
    setTextToFind("line");
    checkResultByText("first <selection>line</selection>\nsecond line");
    getEditor().getFoldingModel().runBatchFoldingOperation(() ->
                                                             getEditor().getFoldingModel().addFoldRegion(0, 11, "...").setExpanded(false));
    checkResultByText("<caret>first line\nsecond line");
    nextOccurrence();
    checkResultByText("first <selection>line</selection>\nsecond line");
    nextOccurrence();
    checkResultByText("first line\nsecond <selection>line</selection>");
  }
}
