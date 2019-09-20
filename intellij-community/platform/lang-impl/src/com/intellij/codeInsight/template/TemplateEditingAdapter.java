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

package com.intellij.codeInsight.template;

import com.intellij.codeInsight.template.impl.TemplateState;
import org.jetbrains.annotations.NotNull;

/**
 * @author ven
 */
public abstract class TemplateEditingAdapter implements TemplateEditingListener {

  @Override
  public void beforeTemplateFinished(@NotNull final TemplateState state, final Template template) {
  }

  @Override
  public void templateFinished(@NotNull Template template, boolean brokenOff) {
  }

  @Override
  public void templateCancelled(Template template) {
  }

  @Override
  public void currentVariableChanged(@NotNull TemplateState templateState, Template template, int oldIndex, int newIndex) {
  }

  @Override
  public void waitingForInput(Template template) {
  }
}
