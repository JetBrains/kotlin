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
package com.intellij.util.textCompletion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class DefaultTextCompletionValueDescriptor<T> implements TextCompletionValueDescriptor<T> {
  @NotNull
  protected abstract String getLookupString(@NotNull T item);

  @Nullable
  protected Icon getIcon(@NotNull T item) {
    return null;
  }

  @Nullable
  protected String getTailText(@NotNull T item) {
    return null;
  }

  @Nullable
  protected String getTypeText(@NotNull T item) {
    return null;
  }

  @Nullable
  protected InsertHandler<LookupElement> createInsertHandler(@NotNull final T item) {
    return null;
  }

  @Override
  public int compare(T item1, T item2) {
    return StringUtil.compare(getLookupString(item1), getLookupString(item2), false);
  }

  @NotNull
  @Override
  public LookupElementBuilder createLookupBuilder(@NotNull T item) {
    LookupElementBuilder builder = LookupElementBuilder.create(item, getLookupString(item))
      .withIcon(getIcon(item));

    InsertHandler<LookupElement> handler = createInsertHandler(item);
    if (handler != null) {
      builder = builder.withInsertHandler(handler);
    }

    String tailText = getTailText(item);
    if (tailText != null) {
      builder = builder.withTailText(tailText, true);
    }

    String typeText = getTypeText(item);
    if (typeText != null) {
      builder = builder.withTypeText(typeText);
    }
    return builder;
  }

  public static class StringValueDescriptor extends DefaultTextCompletionValueDescriptor<String> {
    @NotNull
    @Override
    public String getLookupString(@NotNull String item) {
      return item;
    }
  }
}
