// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.slicer;

import com.intellij.openapi.util.NlsContexts;

public class SliceFilterParseException extends Exception {
  public SliceFilterParseException(@NlsContexts.DialogMessage String message) {
    super(message);
  }
}
