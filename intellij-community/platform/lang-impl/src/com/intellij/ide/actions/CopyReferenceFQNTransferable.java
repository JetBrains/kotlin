// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

 class CopyReferenceFQNTransferable implements Transferable {
  private final String fqn;

  CopyReferenceFQNTransferable(String fqn) {
    this.fqn = fqn;
  }

  @Override
  public DataFlavor[] getTransferDataFlavors() {
    return new DataFlavor[]{CopyReferenceAction.ourFlavor, DataFlavor.stringFlavor};
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return ArrayUtilRt.find(getTransferDataFlavors(), flavor) != -1;
  }

  @Override
  @Nullable
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    if (isDataFlavorSupported(flavor)) {
      return fqn;
    }
    return null;
  }
}