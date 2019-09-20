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

package com.intellij.psi.impl.include;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Dmitry Avdeev
 */
class FileIncludeInfoImpl extends FileIncludeInfo {

  public final String providerId;

  FileIncludeInfoImpl(@NotNull String path, int offset, boolean runtimeOnly, String providerId) {
    super(new File(path).getName(), path, offset, runtimeOnly);
    this.providerId = providerId;
  }

  @SuppressWarnings({"RedundantIfStatement"})
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FileIncludeInfoImpl that = (FileIncludeInfoImpl)o;

    if (!fileName.equals(that.fileName)) return false;
    if (!path.equals(that.path)) return false;
    if (offset != that.offset) return false;
    if (runtimeOnly != that.runtimeOnly) return false;
    if (!providerId.equals(that.providerId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = fileName.hashCode();
    result = 31 * result + path.hashCode();
    result = 31 * result + offset;
    result = 31 * result + (runtimeOnly ? 1 : 0);
    return result;
  }
}
