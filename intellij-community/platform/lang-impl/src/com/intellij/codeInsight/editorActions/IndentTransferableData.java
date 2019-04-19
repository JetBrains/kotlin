/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import org.jetbrains.annotations.NonNls;

import java.awt.datatransfer.DataFlavor;
import java.io.Serializable;

/**
 * @author yole
 */
public class IndentTransferableData implements TextBlockTransferableData, Serializable {
  private static @NonNls DataFlavor ourFlavor;

  private final int myOffset;

  public IndentTransferableData(int offset) {
    myOffset = offset;
  }


  public int getOffset() {
    return myOffset;
  }

  @Override
  public DataFlavor getFlavor() {
    return getDataFlavorStatic();
  }

  public static DataFlavor getDataFlavorStatic() {
    if (ourFlavor != null) {
      return ourFlavor;
    }
    try {
      ourFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + IndentTransferableData.class.getName(), "Python indent transferable data");
    }
    catch (NoClassDefFoundError | IllegalArgumentException e) {
      return null;
    }
    return ourFlavor;
  }

  @Override
  public int getOffsetCount() {
    return 0;
  }

  @Override
  public int getOffsets(int[] offsets, int index) {
    return index;
  }

  @Override
  public int setOffsets(int[] offsets, int index) {
    return index;
  }

  @Override
  protected IndentTransferableData clone() {
    return new IndentTransferableData(myOffset);
  }
}
