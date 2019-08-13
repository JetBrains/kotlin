/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.lang.ITokenTypeRemapper;
import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ListIterator;

/**
 * @author peter
 */
public abstract class PrattBuilder {
  public abstract Lexer getLexer();

  public abstract void setTokenTypeRemapper(@Nullable ITokenTypeRemapper remapper);

  public abstract MutableMarker mark();

  public PrattBuilder createChildBuilder(int priority, @Nullable String expectedMessage) {
    return createChildBuilder(priority).expecting(expectedMessage);
  }

  public PrattBuilder createChildBuilder(int priority) {
    return createChildBuilder().withLowestPriority(priority);
  }

  @Nullable
  public IElementType parseChildren(int priority, @Nullable String expectedMessage) {
    return createChildBuilder(priority, expectedMessage).parse();
  }

  protected abstract PrattBuilder createChildBuilder();

  public boolean assertToken(final PrattTokenType type) {
    if (checkToken(type)) {
      return true;
    }
    error(type.getExpectedText(this));
    return false;
  }

  public boolean assertToken(IElementType type, @NotNull String errorMessage) {
    if (checkToken(type)) {
      return true;
    }
    error(errorMessage);
    return false;
  }

  public boolean checkToken(IElementType type) {
    if (isToken(type)) {
      advance();
      return true;
    }
    return false;
  }

  public abstract void advance();

  public abstract void error(@NotNull String errorText);

  public boolean isEof() {
    return isToken(null);
  }

  public boolean isToken(@Nullable IElementType type) {
    return getTokenType() == type;
  }

  @Nullable
  public abstract IElementType getTokenType();

  @Nullable
  public abstract String getTokenText();

  public abstract void reduce(@NotNull IElementType type);

  public ListIterator<IElementType> getBackResultIterator() {
    final List<IElementType> resultTypes = getResultTypes();
    return resultTypes.listIterator(resultTypes.size());
  }

  public abstract List<IElementType> getResultTypes();

  public abstract PrattBuilder getParent();

  public abstract int getPriority();

  public abstract int getCurrentOffset();

  public abstract PrattBuilder expecting(@Nullable String expectedMessage);

  public abstract PrattBuilder withLowestPriority(int priority);

  @Nullable
  public abstract IElementType parse();
}
