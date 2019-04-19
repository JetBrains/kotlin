/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.psi.codeStyle;


import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;


public class TimeStampedIndentOptions extends CommonCodeStyleSettings.IndentOptions {
  private long myTimeStamp;
  private int myOriginalIndentOptionsHash;
  private boolean myDetected;

  protected TimeStampedIndentOptions(CommonCodeStyleSettings.IndentOptions toCopyFrom, long timeStamp) {
    copyFrom(toCopyFrom);
    myTimeStamp = timeStamp;
    myOriginalIndentOptionsHash = toCopyFrom.hashCode();
  }

  public void setDetected(boolean isDetected) {
    myDetected = isDetected;
  }

  void setTimeStamp(long timeStamp) {
    myTimeStamp = timeStamp;
  }

  public void setOriginalIndentOptionsHash(int originalIndentOptionsHash) {
    myOriginalIndentOptionsHash = originalIndentOptionsHash;
  }

  public boolean isOutdated(@NotNull Document document, @NotNull CommonCodeStyleSettings.IndentOptions defaultForFile) {
    return document.getModificationStamp() != myTimeStamp
           || defaultForFile.hashCode() != myOriginalIndentOptionsHash;
  }

  public boolean isDetected() {
    return myDetected;
  }
}
