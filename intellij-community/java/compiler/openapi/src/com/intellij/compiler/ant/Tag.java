/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Eugene Zhuravlev
 */
public class Tag extends CompositeGenerator {
  public static final Tag[] EMPTY_ARRAY = new Tag[0];
  private final String myTagName;
  private final Pair[] myTagOptions;

  public Tag(@NonNls String tagName, Pair... tagOptions) {
    myTagName = tagName;
    myTagOptions = tagOptions;
  }

  @Override
  public void generate(PrintWriter out) throws IOException {
    out.print("<");
    out.print(myTagName);
    if (myTagOptions != null && myTagOptions.length > 0) {
      out.print(" ");
      int generated = 0;
      for (final Pair option : myTagOptions) {
        if (option == null) {
          continue;
        }
        if (generated > 0) {
          out.print(" ");
        }
        out.print((String)option.getFirst());
        out.print("=\"");
        out.print(StringUtil.escapeXmlEntities((String)option.getSecond()));
        out.print("\"");
        generated += 1;
      }
    }
    if (getGeneratorCount() > 0) {
      out.print(">");
      shiftIndent();
      try {
        super.generate(out);
      }
      finally {
        unshiftIndent();
      }
      crlf(out);
      out.print("</");
      out.print(myTagName);
      out.print(">");
    }
    else {
      out.print("/>");
    }
  }

  @Nullable
  protected static Couple<String> pair(@NonNls String v1, @Nullable @NonNls String v2) {
    if (v2 == null) {
      return null;
    }
    return Couple.of(v1, v2);
  }
}
