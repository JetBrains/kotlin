// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import com.intellij.util.Function;

import java.io.File;

public class FunctionUtils {

  public static final Function<File, String> FILE_TO_PATH = new Function<File, String>() {
    @Override
    public String fun(File file) {
      return file.getPath();
    }

    @Override
    public String toString() {
      return "Function.FILE_TO_PATH";
    }
  };
}
