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
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.Exclude;
import com.intellij.compiler.ant.taskdefs.PatternSet;
import com.intellij.openapi.fileTypes.FileTypeManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * @author Eugene Zhuravlev
 */
public class IgnoredFiles extends Generator{
  private final PatternSet myPatternSet;

  public IgnoredFiles() {
    myPatternSet = new PatternSet(BuildProperties.PROPERTY_IGNORED_FILES);
    final StringTokenizer tokenizer = new StringTokenizer(FileTypeManager.getInstance().getIgnoredFilesList(), ";", false);
    while(tokenizer.hasMoreTokens()) {
      final String filemask = tokenizer.nextToken();
      myPatternSet.add(new Exclude("**/" + filemask + "/**"));
    }
  }



  @Override
  public void generate(PrintWriter out) throws IOException {
    myPatternSet.generate(out);
  }
}
