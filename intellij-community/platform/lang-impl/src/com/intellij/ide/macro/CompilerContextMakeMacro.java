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

package com.intellij.ide.macro;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.DataContext;

public final class CompilerContextMakeMacro extends Macro {
  @Override
  public String getName() {
    return "IsMake";
  }

  @Override
  public String getDescription() {
    return IdeBundle.message("macro.compiler.context.is.make");
  }

  @Override
  public String expand(DataContext dataContext) {
    Object make = dataContext.getData("COMPILER_CONTEXT_MAKE");
    if (make instanceof Boolean) {
      return make.toString();
    }
    return null;
  }
}