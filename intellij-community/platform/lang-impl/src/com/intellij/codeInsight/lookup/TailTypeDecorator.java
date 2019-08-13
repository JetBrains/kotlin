// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Consider using {@link com.intellij.codeInsight.completion.InsertHandler} instead
 * @author peter
 */
public abstract class TailTypeDecorator<T extends LookupElement> extends LookupElementDecorator<T> {
  public TailTypeDecorator(T delegate) {
    super(delegate);
  }

  public static <T extends LookupElement> TailTypeDecorator<T> withTail(T element, final TailType type) {
    return new TailTypeDecorator<T>(element) {
      @Override
      protected TailType computeTailType(InsertionContext context) {
        return type;
      }
    };
  }

  @Nullable
  protected abstract TailType computeTailType(InsertionContext context);

  @Override
  public void handleInsert(@NotNull InsertionContext context) {
    final LookupElement delegate = getDelegate();
    final TailType tailType = computeTailType(context);

    final LookupItem lookupItem = delegate.as(LookupItem.CLASS_CONDITION_KEY);
    if (lookupItem != null && tailType != null) {
      lookupItem.setTailType(TailType.UNKNOWN);
    }
    delegate.handleInsert(context);
    if (tailType != null && tailType.isApplicable(context)) {
      PostprocessReformattingAspect.getInstance(context.getProject()).doPostponedFormatting();
      int tailOffset = context.getTailOffset();
      if (tailOffset < 0) {
        throw new AssertionError("tailOffset < 0: delegate=" + getDelegate() + "; this=" + this + "; tail=" + tailType);
      }
      tailType.processTail(context.getEditor(), tailOffset);
    }
  }

}
