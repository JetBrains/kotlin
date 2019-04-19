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

package com.intellij.compiler.ant;

import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.TestOnly;

public class BuildTargetsFactoryImpl extends BuildTargetsFactory {


  @Override
  public Generator createComment(final String comment) {
    return new Comment(comment);
  }

  @Override
  @TestOnly
  public GenerationOptions getDefaultOptions(Project project) {
    return new GenerationOptionsImpl(project, true, false, false, true, ArrayUtil.EMPTY_STRING_ARRAY);
  }
}