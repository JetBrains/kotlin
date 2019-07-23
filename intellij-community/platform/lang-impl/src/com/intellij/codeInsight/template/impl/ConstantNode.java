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
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class ConstantNode extends Expression {
  private final Result myValue;
  private final LookupElement[] myLookupElements;

  public ConstantNode(@NotNull String value) {
    this(new TextResult(value));
  }

  public ConstantNode(@Nullable Result value) {
    this(value, LookupElement.EMPTY_ARRAY);
  }

  private ConstantNode(@Nullable Result value, @NotNull LookupElement... lookupElements) {
    myValue = value;
    myLookupElements = lookupElements;
  }

  public ConstantNode withLookupItems(@NotNull LookupElement... lookupElements) {
    return new ConstantNode(myValue, lookupElements);
  }

  public ConstantNode withLookupItems(@NotNull Collection<? extends LookupElement> lookupElements) {
    return new ConstantNode(myValue, lookupElements.toArray(LookupElement.EMPTY_ARRAY));
  }

  public ConstantNode withLookupStrings(@NotNull String... lookupElements) {
    return new ConstantNode(myValue, ContainerUtil.map2Array(lookupElements, LookupElement.class, LookupElementBuilder::create));
  }

  public ConstantNode withLookupStrings(@NotNull Collection<String> lookupElements) {
    return new ConstantNode(myValue, ContainerUtil.map2Array(lookupElements, LookupElement.class, LookupElementBuilder::create));
  }

  @Override
  public Result calculateResult(ExpressionContext context) {
    return myValue;
  }

  @Override
  public Result calculateQuickResult(ExpressionContext context) {
    return myValue;
  }

  @Override
  public boolean requiresCommittedPSI() {
    return false;
  }

  @Override
  public LookupElement[] calculateLookupItems(ExpressionContext context) {
    return myLookupElements;
  }

}
