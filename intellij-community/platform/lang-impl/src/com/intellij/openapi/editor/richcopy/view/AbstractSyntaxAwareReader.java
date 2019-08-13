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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author Denis Zhdanov
 */
public abstract class AbstractSyntaxAwareReader extends Reader {

  private static final Logger LOG = Logger.getInstance(AbstractSyntaxAwareReader.class);

  protected String myRawText;
  @NotNull
  protected final SyntaxInfo mySyntaxInfo;

  @Nullable private transient Reader myDelegate;

  public AbstractSyntaxAwareReader(@NotNull SyntaxInfo syntaxInfo) {
    mySyntaxInfo = syntaxInfo;
  }
  
  @Override
  public int read() throws IOException {
    return getDelegate().read();
  }

  @Override
  public int read(@NotNull char[] cbuf, int off, int len) throws IOException {
    return getDelegate().read(cbuf, off, len);
  }

  @Override
  public void close() throws IOException {
    myDelegate = null;
  }

  public void setRawText(String rawText) {
    myRawText = rawText;
  }
  
  @NotNull
  private Reader getDelegate() {
    if (myDelegate != null) {
      return myDelegate;
    }

    myDelegate = new StringReader(getBuffer().toString());
    return myDelegate;
  }

  @NotNull
  public final CharSequence getBuffer() {
    final StringBuilder buffer = new StringBuilder();
    try {
      build(buffer, Registry.intValue("editor.richcopy.max.size.megabytes") * FileUtilRt.MEGABYTE);
    }
    catch (Exception e) {
      LOG.error(e);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Resulting text: \n" + buffer);
    }
    return buffer;
  }

  protected abstract void build(@NotNull StringBuilder holder, int maxLength);
}
