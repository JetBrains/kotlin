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
package com.intellij.execution.filters;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

public class PatternBasedFileHyperlinkRawDataFinder implements FileHyperlinkRawDataFinder {
  private static final int UNKNOWN = -2;

  private final PatternHyperlinkFormat[] myLinkFormats;

  public PatternBasedFileHyperlinkRawDataFinder(@NotNull PatternHyperlinkFormat[] linkFormats) {
    myLinkFormats = linkFormats;
  }

  @NotNull
  @Override
  public List<FileHyperlinkRawData> find(@NotNull String line) {
    Pair<Matcher, PatternHyperlinkFormat> pair = findMatcher(line);
    if (pair == null) {
      return Collections.emptyList();
    }
    Matcher matcher = pair.first;
    PatternHyperlinkFormat format = pair.second;
    PatternHyperlinkPart[] linkParts = format.getLinkParts();
    int groupCount = matcher.groupCount();
    if (groupCount > linkParts.length) {
      return Collections.emptyList();
    }
    String path = null;
    int lineNumber = -1, columnNumber = -1;
    int hyperlinkStartInd = -1, hyperlinkEndInd = -1;
    boolean hyperlinkFreezed = false;
    for (int i = 1; i <= groupCount; i++) {
      String value = matcher.group(i);
      if (value == null) {
        continue;
      }
      PatternHyperlinkPart part = linkParts[i - 1];
      if (part == PatternHyperlinkPart.HYPERLINK) {
        hyperlinkStartInd = matcher.start(i);
        hyperlinkEndInd = matcher.end(i);
        hyperlinkFreezed = true;
      }
      else if (part == PatternHyperlinkPart.PATH) {
        path = value;
        if (!hyperlinkFreezed) {
          hyperlinkStartInd = matcher.start(i);
          hyperlinkEndInd = matcher.end(i);
        }
      }
      else if (part == PatternHyperlinkPart.LINE) {
        value = StringUtil.trimStart(value, ":");
        lineNumber = StringUtil.parseInt(value, UNKNOWN);
        hyperlinkEndInd = tryExtendHyperlinkEnd(hyperlinkFreezed, hyperlinkEndInd, matcher.start(i), matcher.end(i));
      }
      else if (part == PatternHyperlinkPart.COLUMN) {
        value = StringUtil.trimStart(value, ":");
        columnNumber = StringUtil.parseInt(value, UNKNOWN);
        if (columnNumber != UNKNOWN) {
          hyperlinkEndInd = tryExtendHyperlinkEnd(hyperlinkFreezed, hyperlinkEndInd, matcher.start(i), matcher.end(i));
        }
      }
    }
    if (path == null || lineNumber == UNKNOWN || columnNumber == UNKNOWN ||  hyperlinkStartInd == -1) {
      return Collections.emptyList();
    }
    if (!format.isZeroBasedLineNumbering()) {
      lineNumber--;
    }
    if (!format.isZeroBasedColumnNumbering()) {
      columnNumber--;
    }
    lineNumber = Math.max(lineNumber, -1);
    columnNumber = Math.max(columnNumber, -1);
    FileHyperlinkRawData data = new FileHyperlinkRawData(path, lineNumber, columnNumber,
                                                         hyperlinkStartInd, hyperlinkEndInd);
    return Collections.singletonList(data);
  }

  private static int tryExtendHyperlinkEnd(boolean hyperlinkFreezed, int hyperlinkEndInd,
                                           int groupStartInd, int groupEndInd) {
    if (!hyperlinkFreezed && (hyperlinkEndInd == groupStartInd || hyperlinkEndInd + 1 == groupStartInd)) {
      return groupEndInd;
    }
    return hyperlinkEndInd;
  }

  @Nullable
  private Pair<Matcher, PatternHyperlinkFormat> findMatcher(@NotNull String line) {
    for (PatternHyperlinkFormat linkFormat : myLinkFormats) {
      Matcher matcher = linkFormat.getPattern().matcher(line);
      if (matcher.find()) {
        return Pair.create(matcher, linkFormat);
      }
    }
    return null;
  }
}
