/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.fileTypes.impl;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.fileTypes.FileNameMatcherFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author peter
 */
public class IgnoredPatternSet {
  private final Set<String> myMasks = new LinkedHashSet<>();
  private final FileTypeAssocTable<Boolean> myIgnorePatterns = new FileTypeAssocTable<Boolean>().copy();

  @NotNull
  Set<String> getIgnoreMasks() {
    return Collections.unmodifiableSet(myMasks);
  }

  public void setIgnoreMasks(@NotNull String list) {
    clearPatterns();

    StringTokenizer tokenizer = new StringTokenizer(list, ";");
    while (tokenizer.hasMoreTokens()) {
      String ignoredFile = tokenizer.nextToken();
      addIgnoreMask(ignoredFile);
    }
  }

  void addIgnoreMask(@NotNull String ignoredFile) {
    if (myIgnorePatterns.findAssociatedFileType(ignoredFile) == null) {
      myMasks.add(ignoredFile);
      myIgnorePatterns.addAssociation(FileNameMatcherFactory.getInstance().createMatcher(ignoredFile), Boolean.TRUE);
    }
  }

  public boolean isIgnored(@NotNull CharSequence fileName) {
    if (myIgnorePatterns.findAssociatedFileType(fileName) == Boolean.TRUE) {
      return true;
    }

    //Quite a hack, but still we need to have some name, which
    //won't be caught by VFS for sure.
    return StringUtil.endsWith(fileName, FileUtil.ASYNC_DELETE_EXTENSION);
  }

  void clearPatterns() {
    myMasks.clear();
    myIgnorePatterns.removeAllAssociations(Boolean.TRUE);
  }
}
