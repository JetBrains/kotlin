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

package com.intellij.codeInsight.editorActions;

import java.awt.datatransfer.DataFlavor;
import java.io.Serializable;

public class FoldingTransferableData implements TextBlockTransferableData, Serializable {
  private final FoldingData[] myFoldingDatas;

  public FoldingTransferableData(final FoldingData[] foldingDatas) {
    myFoldingDatas = foldingDatas;
  }

  @Override
  public DataFlavor getFlavor() {
    return FoldingData.getDataFlavor();
  }

  @Override
  public int getOffsetCount() {
    return myFoldingDatas.length * 2;
  }

  @Override
  public int getOffsets(final int[] offsets, int index) {
    for (FoldingData data : myFoldingDatas) {
      offsets[index++] = data.startOffset;
      offsets[index++] = data.endOffset;
    }
    return index;
  }

  @Override
  public int setOffsets(final int[] offsets, int index) {
    for (FoldingData data : myFoldingDatas) {
      data.startOffset = offsets[index++];
      data.endOffset = offsets[index++];
    }
    return index;
  }

  @Override
  protected FoldingTransferableData clone() {
    FoldingData[] newFoldingData = new FoldingData[myFoldingDatas.length];
    for (int i = 0; i < myFoldingDatas.length; i++) {
      newFoldingData[i] = (FoldingData)myFoldingDatas[i].clone();
    }
    return new FoldingTransferableData(newFoldingData);
  }

  public FoldingData[] getData() {
    return myFoldingDatas;
  }
}
