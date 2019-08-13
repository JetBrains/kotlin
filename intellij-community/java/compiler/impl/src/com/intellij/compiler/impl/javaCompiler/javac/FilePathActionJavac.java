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
package com.intellij.compiler.impl.javaCompiler.javac;

import com.intellij.compiler.OutputParser;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eugene Zhuravlev
 */
public class FilePathActionJavac extends JavacParserAction {
  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.impl.javaCompiler.javac.FilePathActionJavac");
  private final Matcher myJdk7FormatMatcher;

  public FilePathActionJavac(final Matcher matcher) {
    super(matcher);
    myJdk7FormatMatcher = Pattern.compile("^\\w+\\[(.+)\\]$", Pattern.CASE_INSENSITIVE).matcher("");
  }

  @Override
  protected void doExecute(final String line, String filePath, final OutputParser.Callback callback) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Process parsing message: " + filePath);
    }

    // for jdk7: cut off characters wrapping the path. e.g. "RegularFileObject[C:/tmp/bugs/src/a/Demo1.java]"
    if (myJdk7FormatMatcher.reset(filePath).matches()) {
      filePath = myJdk7FormatMatcher.group(1);
    }

    int index = filePath.lastIndexOf('/');
    final String name = index >= 0 ? filePath.substring(index + 1) : filePath;

    final FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(name);
    if (StdFileTypes.JAVA.equals(fileType)) {
      callback.fileProcessed(filePath);
      callback.setProgressText(CompilerBundle.message("progress.parsing.file", name));
    }
    else if (StdFileTypes.CLASS.equals(fileType)) {
      callback.fileGenerated(filePath);
    }
  }
}
