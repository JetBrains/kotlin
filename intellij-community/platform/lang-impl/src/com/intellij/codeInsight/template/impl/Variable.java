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

package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.template.Expression;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Variable implements Cloneable {
  private final String myName;
  private boolean myAlwaysStopAt;

  @Nullable private String myExpressionString;
  private Expression myExpression = null;

  private String myDefaultValueString;
  private Expression myDefaultValueExpression;
  private final boolean mySkipOnStart;

  public Variable(@NotNull String name, @Nullable Expression expression, @Nullable Expression defaultValueExpression, 
                  boolean alwaysStopAt, boolean skipOnStart) {
    myName = name;
    myExpression = expression;
    myDefaultValueExpression = defaultValueExpression;
    myAlwaysStopAt = alwaysStopAt;
    mySkipOnStart = skipOnStart;
  }

  public Variable(@NotNull String name, @Nullable String expression, @Nullable String defaultValueString, boolean alwaysStopAt) {
    myName = name;
    myExpressionString = StringUtil.notNullize(expression);
    myDefaultValueString = StringUtil.notNullize(defaultValueString);
    myAlwaysStopAt = alwaysStopAt;
    mySkipOnStart = false;
  }

  @NotNull
  public String getExpressionString() {
    return StringUtil.notNullize(myExpressionString);
  }

  public void setExpressionString(@Nullable String expressionString) {
    myExpressionString = expressionString;
    myExpression = null;
  }

  @NotNull
  public Expression getExpression() {
    if (myExpression == null) {
      if (myName.equals(TemplateImpl.SELECTION)) {
        myExpression = new SelectionNode();
      }
      else {
        myExpression = MacroParser.parse(myExpressionString);
      }
    }
    return myExpression;
  }

  @NotNull
  public String getDefaultValueString() {
    return StringUtil.notNullize(myDefaultValueString);
  }

  public void setDefaultValueString(@Nullable String defaultValueString) {
    myDefaultValueString = defaultValueString;
    myDefaultValueExpression = null;
  }

  @NotNull
  public Expression getDefaultValueExpression() {
    if (myDefaultValueExpression == null) {
      myDefaultValueExpression = MacroParser.parse(myDefaultValueString);
    }
    return myDefaultValueExpression;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  public boolean isAlwaysStopAt() {
    if (myName.equals(TemplateImpl.SELECTION)) return false;
    return myAlwaysStopAt;
  }

  public void setAlwaysStopAt(boolean alwaysStopAt) {
    myAlwaysStopAt = alwaysStopAt;
  }

  @Override
  public Object clone() {
    return new Variable(myName, myExpressionString, myDefaultValueString, myAlwaysStopAt);
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Variable)) return false;

    final Variable variable = (Variable) o;

    if (myAlwaysStopAt != variable.myAlwaysStopAt) return false;
    if (mySkipOnStart != variable.mySkipOnStart) return false;
    if (myDefaultValueString != null ? !myDefaultValueString.equals(variable.myDefaultValueString) : variable.myDefaultValueString != null) return false;
    if (myExpressionString != null ? !myExpressionString.equals(variable.myExpressionString) : variable.myExpressionString != null) return false;
    if (!myName.equals(variable.myName)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myName.hashCode();
    result = 29 * result + (myAlwaysStopAt ? 1 : 0);
    result = 29 * result + (mySkipOnStart ? 1 : 0);
    result = 29 * result + (myExpressionString != null ? myExpressionString.hashCode() : 0);
    result = 29 * result + (myDefaultValueString != null ? myDefaultValueString.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Variable{" +
           "myName='" + myName + '\'' +
           ", myAlwaysStopAt=" + myAlwaysStopAt +
           ", myExpressionString='" + myExpressionString + '\'' +
           ", myDefaultValueString='" + myDefaultValueString + '\'' +
           ", mySkipOnStart=" + mySkipOnStart +
           '}';
  }

  public boolean skipOnStart() {
    return mySkipOnStart;
  }
}
