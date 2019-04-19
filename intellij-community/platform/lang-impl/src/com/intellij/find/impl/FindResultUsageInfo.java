/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.find.impl;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiFileRange;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

public class FindResultUsageInfo extends UsageInfo {
  private final FindManager myFindManager;
  private final FindModel myFindModel;
  private final SmartPsiFileRange myAnchor;

  private Boolean myCachedResult;
  private long myTimestamp;

  private static final Key<Long> DOCUMENT_TIMESTAMP_KEY = Key.create("FindResultUsageInfo.DOCUMENT_TIMESTAMP_KEY");

  public FindResultUsageInfo(@NotNull FindManager finder,
                             @NotNull PsiFile file,
                             int offset,
                             @NotNull FindModel findModel,
                             @NotNull FindResult result) {
    super(file, result.getStartOffset(), result.getEndOffset());

    myFindManager = finder;
    myFindModel = findModel;

    assert result.isStringFound();

    if (myFindModel.isRegularExpressions() ||
        myFindModel.isInCommentsOnly() ||
        myFindModel.isInStringLiteralsOnly() ||
        myFindModel.isExceptStringLiterals() ||
        myFindModel.isExceptCommentsAndStringLiterals() ||
        myFindModel.isExceptComments()) {
      myAnchor = SmartPointerManager.getInstance(getProject()).createSmartPsiFileRangePointer(file, TextRange.from(offset, 0));
    }
    else {
      myAnchor = null;
    }
  }

  @Override
  public boolean isValid() {
    if (!super.isValid()) return false;

    Document document = PsiDocumentManager.getInstance(getProject()).getDocument(getPsiFile());
    if (document == null) {
      myCachedResult = null;
      return false;
    }

    Boolean cachedResult = myCachedResult;
    if (document.getModificationStamp() == myTimestamp && cachedResult != null) {
      return cachedResult;
    }
    myTimestamp = document.getModificationStamp();

    Segment segment = getSegment();
    boolean isFileOrBinary = isFileOrBinary();
    if (segment == null && !isFileOrBinary) {
      myCachedResult = false;
      return false;
    }

    VirtualFile file = getPsiFile().getVirtualFile();
    if (isFileOrBinary) {
      myCachedResult = file.isValid();
      return myCachedResult;
    }

    Segment searchOffset;
    if (myAnchor != null) {
      searchOffset = myAnchor.getRange();
      if (searchOffset == null) {
        myCachedResult = false;
        return false;
      }
    }
    else {
      searchOffset = segment;
    }

    int offset = searchOffset.getStartOffset();
    Long data = myFindModel.getUserData(DOCUMENT_TIMESTAMP_KEY);
    if (data == null || data != myTimestamp) {
      data = myTimestamp;
      FindManagerImpl.clearPreviousFindData(myFindModel);
    }
    myFindModel.putUserData(DOCUMENT_TIMESTAMP_KEY, data);
    FindResult result;
    do {
      result = myFindManager.findString(document.getCharsSequence(), offset, myFindModel, file);
      offset = result.getEndOffset() == offset ? offset + 1 : result.getEndOffset();
      if (!result.isStringFound()) {
        myCachedResult = false;
        return false;
      }
    } while (result.getStartOffset() < segment.getStartOffset());

    boolean ret = segment.getStartOffset() == result.getStartOffset() && segment.getEndOffset() == result.getEndOffset();
    myCachedResult = ret;
    return ret;
  }

  private PsiFile getPsiFile() {
    return (PsiFile)getElement();
  }
}
