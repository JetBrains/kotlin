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
package com.intellij.ide.macro;

import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Zhuravlev
 */
public abstract class PromptingMacro extends Macro{

  @Override
  public final String expand(DataContext dataContext) throws ExecutionCancelledException {
    final String userInput = promptUser(dataContext);
    if (userInput == null) {
      throw new ExecutionCancelledException();
    }
    return userInput;
  }


  /**
   * Called from expand() method
   *
   * @param dataContext
   * @return user input. If null is returned, ExecutionCancelledException is thrown by expand() method
   */
  @Nullable
  protected abstract String promptUser(DataContext dataContext);
}
