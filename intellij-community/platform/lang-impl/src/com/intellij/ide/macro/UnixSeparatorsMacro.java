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
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.Nullable;

public final class UnixSeparatorsMacro extends Macro implements SecondQueueExpandMacro{
  @Override
  public String getName() {
    return "UnixSeparators";
  }

  @Override
  public String getDescription() {
    return IdeBundle.message("macro.unix.separators");
  }

  @Nullable
  @Override
  public String expand(DataContext dataContext, String... args) throws ExecutionCancelledException {
    if (args.length == 1) {
      return FileUtil.toSystemIndependentName(args[0]);
    }
    return super.expand(dataContext, args);
  }

  @Nullable
  @Override
  public String expand(DataContext dataContext) {
    return null;
  }
}
