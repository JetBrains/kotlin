/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.compiler.ant.taskdefs;

import com.intellij.compiler.ant.Tag;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Eugene Zhuravlev
 */
public class ZipFileSet extends Tag{
  public static final ZipFileSet[] EMPTY_ARRAY = new ZipFileSet[0];

  private ZipFileSet(@NonNls String tagName, Pair... tagOptions) {
    super(tagName, tagOptions);
  }

  public ZipFileSet(@NonNls String fileOrDir, @NonNls final String relativePath, boolean isDir) {
    super("zipfileset",
          pair(isDir ? "dir" : "file", fileOrDir),
          pair("prefix", prefix(isDir, relativePath)));
  }

  public static ZipFileSet createUnpackedSet(@NonNls String zipFilePath, @NotNull String relativePath, final boolean isDir) {
    return new ZipFileSet("zipfileset",
                          pair("src", zipFilePath),
                          pair("prefix", prefix(isDir, relativePath)));
  }

  @Nullable
  private static String prefix(final boolean isDir, final String relativePath) {
    String path;
    if (isDir) {
      path = relativePath;
    }
    else {
      final String parent = new File(relativePath).getParent();
      path = parent == null ? "" : FileUtil.toSystemIndependentName(parent);
    }
    if (path != null) {
      path = StringUtil.trimStart(path, "/");
    }
    return !StringUtil.isEmpty(path) ? path : null;
  }

}
