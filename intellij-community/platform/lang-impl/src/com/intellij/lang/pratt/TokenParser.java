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
package com.intellij.lang.pratt;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter
 */
public abstract class TokenParser {

  public abstract boolean parseToken(PrattBuilder builder);

  public static TokenParser infix(final int rightPriority, @NotNull final IElementType compositeType) {
    return infix(rightPriority, compositeType, null);
  }

  public static TokenParser infix(final int rightPriority, @NotNull final IElementType compositeType, @Nullable final String errorMessage) {
    return new ReducingParser() {
      @Override
      @Nullable
      public IElementType parseFurther(final PrattBuilder builder) {
        builder.createChildBuilder(rightPriority, errorMessage).parse();
        return compositeType;
      }
    };
  }

  public static TokenParser postfix(@NotNull final IElementType compositeType) {
    return new ReducingParser() {
      @Override
      @Nullable
      public IElementType parseFurther(final PrattBuilder builder) {
        return compositeType;
      }
    };
  }

}
