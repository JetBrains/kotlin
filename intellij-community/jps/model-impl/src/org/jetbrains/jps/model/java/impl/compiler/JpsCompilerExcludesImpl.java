/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jps.model.java.impl.compiler;

import com.intellij.openapi.util.io.FileUtilRt;
import org.jetbrains.jps.model.java.compiler.JpsCompilerExcludes;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author nik
 */
public class JpsCompilerExcludesImpl implements JpsCompilerExcludes {
  private final Set<File> myFiles = new LinkedHashSet<>();
  private final Set<File> myDirectories = new LinkedHashSet<>();
  private final Set<File> myRecursivelyExcludedDirectories = new LinkedHashSet<>();

  @Override
  public void addExcludedFile(String url) {
    addExcludedFile(JpsPathUtil.urlToFile(url));
  }

  @Override
  public void addExcludedDirectory(String url, boolean recursively) {
    addExcludedDirectory(JpsPathUtil.urlToFile(url), recursively);
  }

  protected void addExcludedFile(File file) {
    myFiles.add(file);
  }

  protected void addExcludedDirectory(File dir, boolean recursively) {
    (recursively ? myRecursivelyExcludedDirectories : myDirectories).add(dir);
  }

  @Override
  public boolean isExcluded(File file) {
    if (myFiles.contains(file)) {
      return true;
    }

    if (!myDirectories.isEmpty() || !myRecursivelyExcludedDirectories.isEmpty()) { // optimization
      File parent = FileUtilRt.getParentFile(file);
      if (myDirectories.contains(parent)) {
        return true;
      }

      while (parent != null) {
        if (myRecursivelyExcludedDirectories.contains(parent)) {
          return true;
        }
        parent = FileUtilRt.getParentFile(parent);
      }
    }
    return false;
  }

  @Override
  public Set<File> getExcludedFiles() {
    return myFiles;
  }

  @Override
  public Set<File> getExcludedDirectories() {
    return myDirectories;
  }

  @Override
  public Set<File> getRecursivelyExcludedDirectories() {
    return myRecursivelyExcludedDirectories;
  }
}
