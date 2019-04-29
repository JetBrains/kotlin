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
package com.intellij.ide.util.gotoByName;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ChooseByNameItemProvider {
  @NotNull
  List<String> filterNames(@NotNull ChooseByNameBase base, @NotNull String[] names, @NotNull String pattern);

  boolean filterElements(@NotNull ChooseByNameBase base,
                         @NotNull String pattern,
                         boolean everywhere,
                         @NotNull ProgressIndicator cancelled,
                         @NotNull Processor<Object> consumer);
}
