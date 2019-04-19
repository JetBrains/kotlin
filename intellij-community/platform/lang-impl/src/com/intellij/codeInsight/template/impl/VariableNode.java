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

package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import org.jetbrains.annotations.Nullable;

public class VariableNode extends Expression {
  private final String myName;
  private final Expression myInitialValue;

  public VariableNode(String name, @Nullable Expression initialValue) {
    myName = name;
    myInitialValue = initialValue;
  }

  @Override
  public Result calculateResult(ExpressionContext context) {
    if (myInitialValue != null){
      return myInitialValue.calculateResult(context);
    }
    return TemplateManagerImpl.getTemplateState(context.getEditor()).getVariableValue(getName());
  }

  @Override
  public Result calculateQuickResult(ExpressionContext context) {
    if (myInitialValue != null){
      return myInitialValue.calculateQuickResult(context);
    }
    return TemplateManagerImpl.getTemplateState(context.getEditor()).getVariableValue(getName());
  }

  @Override
  public LookupElement[] calculateLookupItems(ExpressionContext context) {
    if (myInitialValue == null){
      return null;
    }
    return myInitialValue.calculateLookupItems(context);
  }

  public String getName() {
    return myName;
  }

  @Nullable public Expression getInitialValue() {
    return myInitialValue;
  }
}
