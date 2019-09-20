/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.util.Locale;

/**
 * Allows to specify relative output prefix for Path.
 * Used to support searching for nested form files under source roots with package prefixes.
 *
 * @author nik
 */
public class PrefixedPath extends Path {
  private String myPrefix;

  public PrefixedPath(Project project) {
    super(project);
  }

  public PrefixedPath(Project p, String path) {
    super(p, path);
  }

  public String getPrefix() {
    return myPrefix;
  }

  public void setPrefix(String prefix) {
    myPrefix = prefix;
  }

  public File findFile(String relativePath) {
    relativePath = trimStartSlash(relativePath);
    String prefix = myPrefix;
    if (prefix != null) {
      prefix = trimStartSlash(ensureEndsWithSlash(prefix));
      if (!relativePath.toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH))) {
        return null;
      }
      relativePath = relativePath.substring(prefix.length());
    }

    String[] dirsList = list();
    for (String aDirsList : dirsList) {
      String fullPath = ensureEndsWithSlash(aDirsList) + relativePath;
      File file = new File(fullPath.replace('/', File.separatorChar));
      if (file.isFile()) {
        return file;
      }
    }
    return null;
  }

  private static String trimStartSlash(String path) {
    if (path.startsWith("/")) return path.substring(1);
    return path;
  }

  private static String ensureEndsWithSlash(String path) {
    if (!path.endsWith("/")) return path + "/";
    return path;
  }
}
