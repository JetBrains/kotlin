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
package com.intellij.codeInsight.actions;

public class ReformatCodeRunOptions implements LayoutCodeOptions {

  private boolean myRearrangeCode;
  private boolean myOptimizeImports;
  private TextRangeType myProcessingScope;

  public ReformatCodeRunOptions(TextRangeType processingScope) {
    myProcessingScope = processingScope;
  }

  public void setProcessingScope(TextRangeType processingScope) {
    myProcessingScope = processingScope;
  }

  @Override
  public boolean isOptimizeImports() {
    return myOptimizeImports;
  }

  @Override
  public boolean isRearrangeCode() {
    return myRearrangeCode;
  }

  public ReformatCodeRunOptions setRearrangeCode(boolean rearrangeCode) {
    myRearrangeCode = rearrangeCode;
    return this;
  }

  public ReformatCodeRunOptions setOptimizeImports(boolean optimizeImports) {
    myOptimizeImports = optimizeImports;
    return this;
  }

  @Override
  public TextRangeType getTextRangeType() {
    return myProcessingScope;
  }

}

