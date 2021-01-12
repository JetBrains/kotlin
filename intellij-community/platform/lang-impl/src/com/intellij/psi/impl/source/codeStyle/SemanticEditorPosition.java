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
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.HighlighterIteratorWrapper;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.Integer.min;

/**
 * @author Rustam Vishnyakov
 */
public class SemanticEditorPosition {
  public interface SyntaxElement {}
  
  private final EditorEx myEditor;
  private final HighlighterIterator myIterator;
  private final CharSequence myChars;
  private final Function<IElementType, SyntaxElement> myTypeMapper;
  private final BiFunction<? super EditorEx, ? super Integer, ? extends HighlighterIterator> myCreateHighlighterIteratorAtOffset;

  private SemanticEditorPosition(@NotNull EditorEx editor, int offset,
                                 @NotNull BiFunction<? super EditorEx, ? super Integer, ? extends HighlighterIterator> createHighlighterIteratorAtOffset,
                                 @NotNull Function<IElementType, SyntaxElement> typeMapper) {
    myCreateHighlighterIteratorAtOffset = createHighlighterIteratorAtOffset;
    myEditor = editor;
    myChars = myEditor.getDocument().getCharsSequence();
    myIterator = createHighlighterIteratorAtOffset.apply(editor, offset);
    myTypeMapper = typeMapper;
  }

  public void moveBeforeOptional(@NotNull SyntaxElement syntaxElement) {
    if (!myIterator.atEnd()) {
      if (syntaxElement.equals(map(myIterator.getTokenType()))) myIterator.retreat();
    }
  }

  public SemanticEditorPosition beforeOptional(@NotNull SyntaxElement syntaxElement) {
    return copyAnd(position -> position.moveBeforeOptional(syntaxElement));
  }
  
  public void moveBeforeOptionalMix(SyntaxElement @NotNull ... elements) {
    while (isAtAnyOf(elements)) {
      myIterator.retreat();
    }
  }

  public SemanticEditorPosition beforeOptionalMix(SyntaxElement @NotNull ... elements) {
    return copyAnd(position -> position.moveBeforeOptionalMix(elements));
  }
  
  public void moveAfterOptionalMix(SyntaxElement @NotNull ... elements)  {
    while (isAtAnyOf(elements)) {
      myIterator.advance();
    }
  }


  public SemanticEditorPosition afterOptionalMix(SyntaxElement @NotNull ... elements) {
    return copyAnd(position -> position.moveAfterOptionalMix(elements));
  }

  public boolean isAtMultiline() {
    if (!myIterator.atEnd()) {
      return CharArrayUtil.containLineBreaks(myChars, myIterator.getStart(), myIterator.getEnd());
    }
    return false;
  }

  /**
   * Checks if there are line breaks strictly after the given offset till the end of the current element.
   *
   * @param offset The offset to search line breaks after.
   * @return True if there are line breaks after the given offset.
   */
  public boolean hasLineBreaksAfter(int offset) {
    if (!myIterator.atEnd() && offset >= 0) {
      int offsetAfter = offset + 1;
      if (offsetAfter < myIterator.getEnd()) {
        return CharArrayUtil.containLineBreaks(myChars, offsetAfter, myIterator.getEnd());
      }
    }
    return false;
  }

  public boolean isAtMultiline(SyntaxElement... elements) {
    return isAtAnyOf(elements) && CharArrayUtil.containLineBreaks(myChars, myIterator.getStart(), myIterator.getEnd());
  }
  
  public void moveBefore() {
    if (!myIterator.atEnd()) {
      myIterator.retreat();
    }
  }

  public SemanticEditorPosition before() {
    return copyAnd(position -> position.moveBefore());
  }
  
  public void moveAfterOptional(@NotNull SyntaxElement syntaxElement) {
    if (!myIterator.atEnd()) {
      if (syntaxElement.equals(map(myIterator.getTokenType()))) myIterator.advance();
    }
  }

  public SemanticEditorPosition afterOptional(@NotNull SyntaxElement syntaxElement) {
    return copyAnd(position -> position.moveAfterOptional(syntaxElement));
  }

  public void moveAfter() {
    if (!myIterator.atEnd()) {
      myIterator.advance();
    }
  }

  public SemanticEditorPosition after() {
    return copyAnd(position -> position.moveAfter());
  }
  
  public void moveBeforeParentheses(@NotNull SyntaxElement leftParenthesis, @NotNull SyntaxElement rightParenthesis) {
    skipParentheses(false, leftParenthesis, rightParenthesis);
  }

  public void moveAfterParentheses(@NotNull SyntaxElement leftParenthesis, @NotNull SyntaxElement rightParenthesis) {
    skipParentheses(true, leftParenthesis, rightParenthesis);
  }

  protected void skipParentheses(boolean forward, @NotNull SyntaxElement leftParenthesis, @NotNull SyntaxElement rightParenthesis) {
    int parenLevel = 0;
    while (!myIterator.atEnd()) {
      SyntaxElement currElement = map(myIterator.getTokenType());
      if (forward) {
        myIterator.advance();
      }
      else {
        myIterator.retreat();
      }
      if (rightParenthesis.equals(currElement)) {
        parenLevel++;
      }
      else if (leftParenthesis.equals(currElement)) {
        parenLevel--;
        if (parenLevel < 1) {
          break;
        }
      }
    }
  }

  public SemanticEditorPosition beforeParentheses(@NotNull SyntaxElement leftParenthesis, @NotNull SyntaxElement rightParenthesis) {
    return copyAnd(position -> position.moveBeforeParentheses(leftParenthesis, rightParenthesis));
  }

  public void moveToLeftParenthesisBackwardsSkippingNested(@NotNull SyntaxElement leftParenthesis,
                                                           @NotNull SyntaxElement rightParenthesis) {
    moveToLeftParenthesisBackwardsSkippingNestedWithPredicate(leftParenthesis, rightParenthesis, any -> false);
  }

  public SemanticEditorPosition findLeftParenthesisBackwardsSkippingNested(@NotNull SyntaxElement leftParenthesis,
                                                                           @NotNull SyntaxElement rightParenthesis) {
    return copyAnd(position -> position.moveToLeftParenthesisBackwardsSkippingNested(leftParenthesis, rightParenthesis));
  }

  public void moveToLeftParenthesisBackwardsSkippingNestedWithPredicate(@NotNull SyntaxElement leftParenthesis,
                                                                        @NotNull SyntaxElement rightParenthesis,
                                                                        @NotNull Predicate<? super SemanticEditorPosition> terminationCondition) {
    while (!myIterator.atEnd()) {
      if (terminationCondition.test(this)) {
        break;
      }
      if (rightParenthesis.equals(map(myIterator.getTokenType()))) {
        moveBeforeParentheses(leftParenthesis, rightParenthesis);
        continue;
      }
      else if (leftParenthesis.equals(map(myIterator.getTokenType()))) {
        break; 
      }
      myIterator.retreat();
    }
  }

  public SemanticEditorPosition findLeftParenthesisBackwardsSkippingNestedWithPredicate(
    @NotNull SyntaxElement leftParenthesis,
    @NotNull SyntaxElement rightParenthesis,
    @NotNull Predicate<? super SemanticEditorPosition> terminationCondition)
  {
    return copyAnd(position -> position.moveToLeftParenthesisBackwardsSkippingNestedWithPredicate(
      leftParenthesis, rightParenthesis, terminationCondition));
  }

  public boolean isAfterOnSameLine(SyntaxElement @NotNull ... syntaxElements) {
    return elementAfterOnSameLine(syntaxElements) != null;
  }

  @Nullable
  public SyntaxElement elementAfterOnSameLine(SyntaxElement @NotNull ... syntaxElements) {
    myIterator.retreat();
    while (!myIterator.atEnd() && !isAtMultiline()) {
      SyntaxElement currElement = map(myIterator.getTokenType());
      for (SyntaxElement element : syntaxElements) {
        if (element.equals(currElement)) return element;
      }
      myIterator.retreat();
    }
    return null;
  }
  
  public boolean isAt(@NotNull SyntaxElement syntaxElement) {
    return !myIterator.atEnd() && syntaxElement.equals(map(myIterator.getTokenType()));
  }

  public boolean isAt(@NotNull IElementType elementType) {
    return !myIterator.atEnd() && myIterator.getTokenType() == elementType;
  }
  
  public boolean isAtEnd() {
    return myIterator.atEnd();
  }
  
  public int getStartOffset() {
    return myIterator.getStart();
  }

  public boolean isAtAnyOf(SyntaxElement @NotNull ... syntaxElements) {
    if (!myIterator.atEnd()) {
      SyntaxElement currElement = map(myIterator.getTokenType());
      for (SyntaxElement element : syntaxElements) {
        if (element.equals(currElement)) return true;
      }
    }
    return false;
  }

  public CharSequence getChars() {
    return myChars;
  }
  
  
  public int findStartOf(@NotNull SyntaxElement element) {
    while (!myIterator.atEnd()) {
      if (element.equals(map(myIterator.getTokenType()))) return myIterator.getStart();
      myIterator.retreat();
    }
    return -1;
  }

  public boolean hasEmptyLineAfter(int offset) {
    for (int i = offset + 1; i < myIterator.getEnd(); i++) {
      if (myChars.charAt(i) == '\n') return true;
    }
    return false;
  }

  public EditorEx getEditor() {
    return myEditor;
  }
  
  @Nullable
  public Language getLanguage() {
    return !myIterator.atEnd() ? myIterator.getTokenType().getLanguage() : null;
  }
  
  public boolean isAtLanguage(@Nullable Language language) {
    if (language != null && !myIterator.atEnd()) {
      return language== Language.ANY || myIterator.getTokenType().getLanguage().is(language); 
    }
    return false;
  }

  @Nullable
  public SyntaxElement getCurrElement() {
    return !myIterator.atEnd() ? map(myIterator.getTokenType()) : null;
  }
  
  public boolean matchesRule(@NotNull Rule rule) {
    return rule.check(this);
  }

  public interface Rule {
    boolean check(SemanticEditorPosition position);
  }
  
  public SyntaxElement map(@NotNull IElementType elementType) {
    return myTypeMapper.apply(elementType);
  }

  @Override
  public String toString() {
    return myIterator.atEnd() 
       ? "atEnd"
       : myIterator.getTokenType().toString() 
         + "=>" 
         + getChars().subSequence(getStartOffset(), min(getStartOffset() + 255, getChars().length()));
  }

  public SemanticEditorPosition copy() {
      return createEditorPosition(myEditor, 
                                  isAtEnd() ? -1 : myIterator.getStart(),
                                  (editor, offset) -> !isAtEnd() 
                                  ? myCreateHighlighterIteratorAtOffset.apply(editor, offset)
                                  : new HighlighterIteratorWrapper(myIterator) { // A wrapper around current iterator to make it immutable.
                                    @Override
                                    public void advance() {
                                      // do nothing
                                    }
                                    @Override
                                    public void retreat() {
                                      // do nothing
                                    }
                                  }, 
                                  myTypeMapper);
  }

  public SemanticEditorPosition copyAnd(@NotNull Consumer<? super SemanticEditorPosition> modifier) {
    SemanticEditorPosition position = copy();
    modifier.accept(position);
    return position;
  }
  
  @NotNull
  public static SemanticEditorPosition createEditorPosition(@NotNull EditorEx editor, int offset,
                                                            @NotNull BiFunction<? super EditorEx, ? super Integer, ? extends HighlighterIterator> createHighlighterIteratorAtOffset,
                                                            @NotNull Function<IElementType, SyntaxElement> typeMapper) {
    return new SemanticEditorPosition(editor, offset, createHighlighterIteratorAtOffset, typeMapper);
  }
}
