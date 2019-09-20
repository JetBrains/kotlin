/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.editor.richcopy.view;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.richcopy.model.SyntaxInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author Denis Zhdanov
 */
abstract class AbstractSyntaxAwareInputStreamTransferableData extends InputStream implements RawTextWithMarkup {
  private static final Logger LOG = Logger.getInstance(AbstractSyntaxAwareInputStreamTransferableData.class);

  String myRawText;
  @NotNull
  final SyntaxInfo mySyntaxInfo;
  @NotNull
  private final DataFlavor myDataFlavor;

  @Nullable private transient InputStream myDelegate;

  AbstractSyntaxAwareInputStreamTransferableData(@NotNull SyntaxInfo syntaxInfo, @NotNull DataFlavor flavor) {
    mySyntaxInfo = syntaxInfo;
    myDataFlavor = flavor;
  }

  @Override
  public DataFlavor getFlavor() {
    return myDataFlavor;
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
  public int read() throws IOException {
    return getDelegate().read();
  }

  @Override
  public int read(@NotNull byte[] b, int off, int len) throws IOException {
    return getDelegate().read(b, off, len);
  }

  @Override
  public void close() throws IOException {
    myDelegate = null;
  }

  @Override
  public void setRawText(String rawText) {
    myRawText = rawText;
  }

  @NotNull
  private InputStream getDelegate() {
    if (myDelegate != null) {
      return myDelegate;
    }

    int maxLength = Registry.intValue("editor.richcopy.max.size.megabytes") * FileUtilRt.MEGABYTE;
    final StringBuilder buffer = new StringBuilder();
    try {
      build(buffer, maxLength);
    }
    catch (Exception e) {
      LOG.error(e);
    }
    String s = buffer.toString();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Resulting text: \n" + s);
    }
    try {
      myDelegate = new ByteArrayInputStream(s.getBytes(getCharset()));
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return myDelegate;
  }
  
  protected abstract void build(@NotNull StringBuilder holder, int maxLength);

  @NotNull
  protected abstract String getCharset();

  @Override
  public synchronized void mark(int readlimit) {
    getDelegate().mark(readlimit);
  }

  @Override
  public synchronized void reset() throws IOException {
    getDelegate().reset();
  }

  @Override
  public boolean markSupported() {
    return getDelegate().markSupported();
  }
}
