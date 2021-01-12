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
package com.intellij.refactoring;

import com.intellij.openapi.util.Ref;
import com.intellij.usageView.UsageInfo;

/**
 * @author dsl
 */
public abstract class RefactoringImpl<T extends BaseRefactoringProcessor> implements Refactoring {
  protected final T myProcessor;

  public RefactoringImpl(T refactoringProcessor) {
    myProcessor = refactoringProcessor;
  }

  @Override
  public void setPreviewUsages(boolean value) {
    myProcessor.setPreviewUsages(value);
  }

  @Override
  public boolean isPreviewUsages() {
    return myProcessor.isPreviewUsages();
  }

  @Override
  public void setInteractive(Runnable prepareSuccessfulCallback) {
    myProcessor.setPrepareSuccessfulSwingThreadCallback(prepareSuccessfulCallback);
  }

  @Override
  public boolean isInteractive() {
    return myProcessor.myPrepareSuccessfulSwingThreadCallback != null;
  }

  @Override
  public UsageInfo[] findUsages() {
    return myProcessor.findUsages();
  }

  @Override
  public boolean preprocessUsages(Ref<UsageInfo[]> usages) {
    return myProcessor.preprocessUsages(usages);
  }

  @Override
  public boolean shouldPreviewUsages(UsageInfo[] usages) {
    return myProcessor.isPreviewUsages(usages);
  }

  @Override
  public void doRefactoring(UsageInfo[] usages) {
    myProcessor.execute(usages);
  }

  @Override
  public void run() {
    myProcessor.run();
  }


}
