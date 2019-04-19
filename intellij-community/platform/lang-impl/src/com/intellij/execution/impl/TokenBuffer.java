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
package com.intellij.execution.impl;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Queue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Stores list of tokens (a token is {@link TokenInfo} which is a text plus {@link ConsoleViewContentType} plus {@link HyperlinkInfo})
 * Tries to maintain the total token text length not more than {@link #maxCapacity}, trims tokens from the beginning on overflow.
 * Add token via {@link #print(String, ConsoleViewContentType, HyperlinkInfo)}
 * Get all tokens via {@link #drain()}
 */
class TokenBuffer {
  // special token which means that the deferred text starts with "\r" so it shouldn't be appended to the document end.
  // Instead, the last line of the document should be removed
  static final TokenInfo CR_TOKEN = new TokenInfo(ConsoleViewContentType.SYSTEM_OUTPUT, "\r", null);
  private final int maxCapacity;  // if size becomes > maxCapacity we should trim tokens from the beginning
  private final Queue<TokenInfo> tokens = new Queue<>(10); // each call to print() is stored here
  private int size; // total lengths of all tokens
  private int startIndex; // index of text start in the first TokeInfo. This TokenInfo can become sliced after total size overflows maxCapacity

  TokenBuffer(int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  void print(@NotNull String text, @NotNull ConsoleViewContentType contentType, @Nullable HyperlinkInfo info) {
    int start = 0;
    while (start < text.length()) {
      if (hasTrailingCR()) {
        combineTrailingCRWith(text);
      }
      int crIndex = text.indexOf('\r', start);
      if (crIndex == -1 || crIndex == text.length() - 1) {
        TokenInfo tokenInfo = new TokenInfo(contentType, text.substring(start), info);
        tokens.addLast(tokenInfo);
        size += tokenInfo.length();
        break;
      }

      if (start!=crIndex) {
        TokenInfo tokenInfo = new TokenInfo(contentType, text.substring(start, crIndex), info);
        tokens.addLast(tokenInfo);
        size += tokenInfo.length();
      }
      removeLastLine();
      // text[start..crIndex) should be removed
      start = crIndex + 1;
    }
    trim();
  }

  // has to combine "\r" from the previous token with "\n" from current token to make it LF
  private boolean hasTrailingCR() {
    return !tokens.isEmpty() && tokens.peekLast() != CR_TOKEN && StringUtil.endsWithChar(tokens.peekLast().getText(), '\r');
  }

  // \r with \n should be \n
  // \r with other character c should be remove last line, c
  private void combineTrailingCRWith(@NotNull String currentText) {
    if (StringUtil.startsWith(currentText, "\n")) {
      TokenInfo last = tokens.removeLast();
      String lastTextWithNoCR = last.getText().substring(0, last.length() - 1);
      if (!lastTextWithNoCR.isEmpty()) {
        TokenInfo newLast = new TokenInfo(last.contentType, lastTextWithNoCR, last.getHyperlinkInfo());
        tokens.addLast(newLast);
        size --;
      }
      return;
    }
    removeLastLine();
  }

  private void removeLastLine() {
    // when \r happens, need to delete the last line
    while (!tokens.isEmpty() && tokens.peekLast() != CR_TOKEN) {
      TokenInfo last = tokens.removeLast();
      String text = last.getText();
      int lfIndex = text.lastIndexOf('\n');
      if (lfIndex != -1) {
        // split token
        TokenInfo newToken = new TokenInfo(last.contentType, text.substring(0, lfIndex + 1), last.getHyperlinkInfo());
        tokens.addLast(newToken);
        size -= text.length() - newToken.length();
        return;
      }
      // remove the token entirely, move to the previous
      size -= text.length();
    }
    if (tokens.isEmpty()) {
      // \r at the very beginning. return CR_TOKEN to signal this
      tokens.addLast(CR_TOKEN);
      size ++;
    }
  }

  private void trim() {
    // toss tokens from the beginning until size became < maxCapacity
    while (size - startIndex > maxCapacity) {
      TokenInfo info = tokens.peekFirst();
      int length = info.length() - startIndex;
      if (length > size - maxCapacity) {
        // slice a part of this info
        startIndex += size - maxCapacity;
        break;
      }
      startIndex = 0;
      tokens.pullFirst();
      size -= info.length();
    }

    //assert tokens.toList().stream().mapToInt(TokenInfo::length).sum() == size;
  }

  int length() {
    return size - startIndex;
  }

  void clear() {
    tokens.clear();
    startIndex = 0;
    size = 0;
  }

  @NotNull
  CharSequence getText() {
    if (hasTrailingCR()) {
      removeLastLine();
    }
    return getRawText(getInfos());
  }

  @NotNull
  static CharSequence getRawText(@NotNull List<TokenInfo> tokens) {
    int size = 0;
    for (TokenInfo token : tokens) {
      size += token.getText().length();
    }
    StringBuilder result = new StringBuilder(size);
    for (TokenInfo token : tokens) {
      result.append(token.getText());
    }
    return result.toString();
  }

  // the first token may be CR_TOKEN meaning that instead of appending it we should delete the last line of the document
  // all the remaining text is guaranteed not to contain CR_TOKEN - they can be appended safely to the document end
  @NotNull
  List<TokenInfo> drain() {
    if (hasTrailingCR()) {
      removeLastLine();
    }
    try {
      return getInfos();
    }
    finally {
      clear();
    }
  }

  @NotNull
  private List<TokenInfo> getInfos() {
    List<TokenInfo> list = tokens.toList();
    if (startIndex != 0) {
      // slice the first token
      TokenInfo first = list.get(0);
      TokenInfo sliced = new TokenInfo(first.contentType, first.getText().substring(startIndex), first.getHyperlinkInfo());
      return ContainerUtil.concat(Collections.singletonList(sliced), list.subList(1, list.size()));
    }
    return list;
  }

  int getCycleBufferSize() {
    return maxCapacity;
  }

  static class TokenInfo {
    @NotNull
    final ConsoleViewContentType contentType;
    private final String text;
    private final HyperlinkInfo myHyperlinkInfo;

    TokenInfo(@NotNull ConsoleViewContentType contentType,
              @NotNull String text,
              @Nullable HyperlinkInfo hyperlinkInfo) {
      this.contentType = contentType;
      myHyperlinkInfo = hyperlinkInfo;
      this.text = text;
    }

    int length() {
      return text.length();
    }

    @Override
    public String toString() {
      return contentType + "[" + length() + "]";
    }

    HyperlinkInfo getHyperlinkInfo() {
      return myHyperlinkInfo;
    }

    @NotNull
    String getText() {
      return text;
    }
  }
}
