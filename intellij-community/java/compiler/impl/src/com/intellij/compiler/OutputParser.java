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
package com.intellij.compiler;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

public abstract class OutputParser {
  protected final List<ParserAction> myParserActions = new ArrayList<>(10);

  public interface Callback {
    String getNextLine();        
    String getCurrentLine();
    void pushBack(String line);
    void setProgressText(String text);
    void fileProcessed(String path);
    void fileGenerated(String path);
    void message(CompilerMessageCategory category, String message, @NonNls String url, int lineNum, int columnNum);
  }

  public boolean processMessageLine(Callback callback) {
    final String line = callback.getNextLine();
    if(line == null) {
      return false;
    }
    // common statistics messages (javac & jikes)
    for (ParserAction action : myParserActions) {
      if (action.execute(line, callback)) {
        return true;
      }
    }
    if (StringUtil.startsWithChar(line, '[') && StringUtil.endsWithChar(line, ']')) {
      // at this point any meaningful output surrounded with '[' and ']' characters is processed, so
      // suppress messages like "[total 4657ms]" or "[search path for source files: []]"
      return true;
    }
    return false;
  }

  protected static void addMessage(Callback callback, CompilerMessageCategory type, String message) {
    if(message == null || message.trim().length() == 0) {
      return;
    }
    addMessage(callback, type, message, null, -1, -1);
  }

  protected static void addMessage(Callback callback, CompilerMessageCategory type, String text, String url, int line, int column){
    callback.message(type, text, url, line, column);
  }
}