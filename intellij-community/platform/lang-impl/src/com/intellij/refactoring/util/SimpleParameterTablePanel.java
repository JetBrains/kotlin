/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.refactoring.util;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.ColumnInfo;

import java.util.function.Predicate;

public abstract class SimpleParameterTablePanel extends AbstractParameterTablePanel<AbstractVariableData> {
  public SimpleParameterTablePanel(Predicate<? super String> parameterNameValidator) {
    super(new PassParameterColumnInfo(), new NameColumnInfo(parameterNameValidator));
  }

  public SimpleParameterTablePanel(Project project, Language language, ColumnInfo... columnInfos) {
    super(new PassParameterColumnInfo(), new NameColumnInfo(language, project));
  }
}
