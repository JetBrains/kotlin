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

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Eugene Zhuravlev
 */
public class Comment extends Generator{
  private final String myComment;
  private final Generator myCommentedData;

  public Comment(String comment) {
    this(comment, null);
  }

  public Comment(Generator commentedData) {
    this(null, commentedData);
  }

  public Comment(String comment, Generator commentedData) {
    myComment = comment;
    myCommentedData = commentedData;
  }

  @Override
  public void generate(PrintWriter out) throws IOException {
    if (myComment != null) {
      out.print("<!-- ");
      out.print(myComment);
      out.print(" -->");
      if (myCommentedData != null) {
        crlf(out);
      }
    }
    if (myCommentedData != null) {
      out.print("<!-- ");
      crlf(out);
      myCommentedData.generate(out);
      crlf(out);
      out.print(" -->");
    }
  }
}
