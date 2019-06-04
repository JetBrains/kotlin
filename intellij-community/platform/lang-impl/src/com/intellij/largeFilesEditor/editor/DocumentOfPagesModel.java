// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;

public class DocumentOfPagesModel {

  private static final Logger LOG = Logger.getInstance(DocumentOfPagesModel.class);

  private final Document myDocument;
  private final ArrayList<Page> pagesInDocument = new ArrayList<>();

  /**
   * corresponding symbol offset to each page in {@code pagesInDocument}
   * list relatively to document.
   * <pre>
   * symbolOffsetToEndOfPage.get(0) = pagesInDocument.get(0).getText().length(),
   * symbolOffsetToEndOfPage.get(i) =
   *   symbolOffsetToEndOfPage.get(i-1) +
   *   pagesInDocument.get(i).getText().length() <pre/>
   */
  private final ArrayList<Integer> symbolOffsetToEndOfPage = new ArrayList<>();

  DocumentOfPagesModel(Document document) {
    myDocument = document;
  }

  public Document getDocument() {
    return myDocument;
  }

  public int getPagesAmount() {
    return pagesInDocument.size();
  }

  public ArrayList<Page> getPagesList() {
    return pagesInDocument;
  }

  public Page getPageByIndex(int index) {
    return pagesInDocument.get(index);
  }

  public Page getFirstPage() {
    if (pagesInDocument.isEmpty()) return null;
    return pagesInDocument.get(0);
  }

  public Page getLastPage() {
    if (pagesInDocument.isEmpty()) return null;
    return pagesInDocument.get(pagesInDocument.size() - 1);
  }

  public void addDocumentListener(DocumentListener listener) {
    myDocument.addDocumentListener(listener);
  }

  public void removeDocumentListener(DocumentListener listener) {
    myDocument.removeDocumentListener(listener);
  }

  public void addPageIntoEnd(Page page, Project project) {
    pagesInDocument.add(page);

    symbolOffsetToEndOfPage.add(
      (symbolOffsetToEndOfPage.isEmpty() ? 0 : symbolOffsetToEndOfPage.get(symbolOffsetToEndOfPage.size() - 1))
      + page.getText().length());

    WriteCommandAction.runWriteCommandAction(
      project,
      myDocument.getTextLength() == 0 ? () -> myDocument.setText(page.getText())
                                      : () -> myDocument.insertString(myDocument.getTextLength(), page.getText()));
  }

  public void removeLastPage(Project project) {
    if (pagesInDocument.size() > 0) {
      int indexOfLastPage = pagesInDocument.size() - 1;
      Page lastPage = pagesInDocument.get(indexOfLastPage);
      pagesInDocument.remove(indexOfLastPage);
      symbolOffsetToEndOfPage.remove(indexOfLastPage);
      WriteCommandAction.runWriteCommandAction(
        project,
        () -> myDocument.deleteString(
          myDocument.getTextLength() - lastPage.getText().length(), myDocument.getTextLength()));
    }
  }

  public void removeAllPages(Project project) {
    pagesInDocument.clear();
    symbolOffsetToEndOfPage.clear();
    WriteCommandAction.runWriteCommandAction(
      project,
      () -> myDocument.deleteString(0, myDocument.getTextLength()));
  }

  public AbsoluteSymbolPosition offsetToAbsoluteSymbolPosition(int offset) {
    if (offset < 0 || offset > myDocument.getTextLength()) {
      throw new IllegalArgumentException("offset=" + offset + " document.length=" + myDocument.getTextLength());
    }

    for (int i = pagesInDocument.size() - 1; i >= 0; i--) {
      int symbolOffsetToStartOfPage = getSymbolOffsetToStartOfPage(i);
      if (offset >= symbolOffsetToStartOfPage) {
        return new AbsoluteSymbolPosition(pagesInDocument.get(i).getPageNumber(),
                                          offset - symbolOffsetToStartOfPage);
      }
    }

    // unreachable code
    LOG.warn(new UnsupportedOperationException("offset=" + offset + " document.length=" + myDocument.getTextLength()
                                               + " pagesInDocument.size=" + pagesInDocument.size()));
    return new AbsoluteSymbolPosition(0, 0);
  }

  public int absoluteSymbolPositionToOffset(AbsoluteSymbolPosition absolutePosition) {
    if (absolutePosition == null || pagesInDocument.size() == 0) {
      return 0;
    }

    if (absolutePosition.pageNumber < getFirstPage().getPageNumber()) {
      return 0;
    }

    if (absolutePosition.pageNumber > getLastPage().getPageNumber()) {
      return myDocument.getTextLength();
    }

    for (int i = 0; i < pagesInDocument.size(); i++) {
      if (absolutePosition.pageNumber == pagesInDocument.get(i).getPageNumber()) {
        return getSymbolOffsetToStartOfPage(i) + absolutePosition.symbolOffsetInPage;
      }
    }

    // unreachable code
    LOG.warn(new UnsupportedOperationException("absolutePosition=" + absolutePosition + " document.length=" + myDocument.getTextLength()
                                               + " pagesInDocument.size=" + pagesInDocument.size()));
    return 0;
  }

  public int getSymbolOffsetToStartOfPage(int indexOfPage) {
    if (indexOfPage == 0) {
      return 0;
    }

    if (indexOfPage >= 1 && indexOfPage <= symbolOffsetToEndOfPage.size()) {
      return symbolOffsetToEndOfPage.get(indexOfPage - 1);
    }

    int emergencyResult = 0;
    if (indexOfPage > symbolOffsetToEndOfPage.size()) {
      emergencyResult = symbolOffsetToEndOfPage.get(symbolOffsetToEndOfPage.size() - 1);
    }
    LOG.info(new IllegalArgumentException(
      "Large File Editor Subsystem] DocumentOfPagesModel.getSymbolOffsetToStartOfPage(...):" +
      " indexOfPage=" + indexOfPage + " symbolOffsetToEndOfPage.size()=" + symbolOffsetToEndOfPage.size()));
    return emergencyResult;
  }

  public int tryGetIndexOfNeededPageInList(long needPageNumber) {
    for (int i = 0; i < pagesInDocument.size(); i++) {
      if (pagesInDocument.get(i).getPageNumber() == needPageNumber) {
        return i;
      }
    }
    return -1;
  }
}
