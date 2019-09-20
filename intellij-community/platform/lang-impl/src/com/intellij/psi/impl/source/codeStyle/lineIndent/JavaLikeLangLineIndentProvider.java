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
package com.intellij.psi.impl.source.codeStyle.lineIndent;

import com.intellij.application.options.CodeStyle;
import com.intellij.formatting.Indent;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider;
import com.intellij.psi.impl.source.codeStyle.SemanticEditorPosition;
import com.intellij.psi.impl.source.codeStyle.SemanticEditorPosition.SyntaxElement;
import com.intellij.psi.impl.source.codeStyle.lineIndent.IndentCalculator.BaseLineOffsetCalculator;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.formatting.Indent.Type;
import static com.intellij.formatting.Indent.Type.*;
import static com.intellij.psi.impl.source.codeStyle.lineIndent.JavaLikeLangLineIndentProvider.JavaLikeElement.*;

/**
 * A base class for Java-like language line indent provider. 
 * If a LineIndentProvider is not provided, {@link FormatterBasedLineIndentProvider} is used.
 * If a registered provider is unable to calculate the indentation, 
 * {@link FormatterBasedIndentAdjuster} will be used.
 */
public abstract class JavaLikeLangLineIndentProvider implements LineIndentProvider{
  
  public enum JavaLikeElement implements SyntaxElement {
    Whitespace,
    Semicolon,
    BlockOpeningBrace,
    BlockClosingBrace,
    ArrayOpeningBracket,
    ArrayClosingBracket,
    RightParenthesis,
    LeftParenthesis,
    Colon,
    SwitchCase,
    SwitchDefault,
    ElseKeyword,
    IfKeyword,
    ForKeyword,
    TryKeyword,
    DoKeyword,
    BlockComment,
    DocBlockStart,
    DocBlockEnd,
    LineComment,
    Comma,
    LanguageStartDelimiter
  }
  
  
  @Nullable
  @Override
  public String getLineIndent(@NotNull Project project, @NotNull Editor editor, @Nullable Language language, int offset) {
    if (offset > 0) {
      IndentCalculator indentCalculator = getIndent(project, editor, language, offset - 1);
      if (indentCalculator != null) {
        return indentCalculator.getIndentString(language, getPosition(editor, offset - 1));
      }
    }
    else {
      return "";
    }
    return null;
  }
  
  @Nullable
  protected IndentCalculator getIndent(@NotNull Project project, @NotNull Editor editor, @Nullable Language language, int offset) {
    IndentCalculatorFactory myFactory = new IndentCalculatorFactory(project, editor);
    if (getPosition(editor, offset).matchesRule(
      position -> position.isAt(Whitespace) &&
                  position.isAtMultiline())) {
      if (getPosition(editor, offset).before().isAt(Comma)) {
        SemanticEditorPosition position = getPosition(editor,offset);
        if (position.hasEmptyLineAfter(offset) &&
            !position.after().matchesRule(
              p->p.isAtAnyOf(ArrayClosingBracket, BlockOpeningBrace, BlockClosingBrace, RightParenthesis) || p.isAtEnd()) &&
            position.findLeftParenthesisBackwardsSkippingNestedWithPredicate(
              LeftParenthesis,
              RightParenthesis,
              self -> self.isAtAnyOf(BlockClosingBrace, BlockOpeningBrace, Semicolon)).isAt(LeftParenthesis)) {
          return myFactory.createIndentCalculator(NONE, IndentCalculator.LINE_AFTER);
        }
      }
      else if (afterOptionalWhitespaceOnSameLine(editor, offset).matchesRule(
        position -> position.isAt(BlockClosingBrace) && !position.after().afterOptional(Whitespace).isAt(Comma))) {
        return myFactory.createIndentCalculator(
          NONE,
          position -> {
            position.moveToLeftParenthesisBackwardsSkippingNested(BlockOpeningBrace, BlockClosingBrace);
            if (!position.isAtEnd()) {
              return getBlockStatementStartOffset(position);
            }
            return -1;
          });
      }
      else if (getPosition(editor, offset).beforeOptional(Whitespace).isAt(BlockClosingBrace)) {
        SemanticEditorPosition position = getPosition(editor, offset).beforeOptional(Whitespace).before();
        position.moveToLeftParenthesisBackwardsSkippingNested(BlockOpeningBrace, BlockClosingBrace);
        position.moveBefore();
        int statementStart = getStatementStartOffset(position, true);
        position = getPosition(editor, statementStart);
        if (!isStartOfStatementWithOptionalBlock(position)) {
          return myFactory.createIndentCalculator(getBlockIndentType(editor, language), IndentCalculator.LINE_BEFORE);
        }
        else {
          return myFactory
            .createIndentCalculator(getBlockIndentType(editor, language), this::getFirstUppermostControlStructureKeywordOffset);
        }
      }
      else if (getPosition(editor, offset).before().isAt(Semicolon)) {
        SemanticEditorPosition beforeSemicolon = getPosition(editor, offset).before().beforeOptional(Semicolon);
        if (beforeSemicolon.isAt(BlockClosingBrace)) {
          beforeSemicolon.moveBeforeParentheses(BlockOpeningBrace, BlockClosingBrace);
        }
        int statementStart = getStatementStartOffset(beforeSemicolon, dropIndentAfterReturnLike(beforeSemicolon));
        SemanticEditorPosition atStatementStart = getPosition(editor, statementStart);
        if (atStatementStart.isAt(BlockOpeningBrace)) {
          return myFactory.createIndentCalculator(getIndentInBlock(project, language, atStatementStart), this::getDeepBlockStatementStartOffset);
        }
        if (!isInsideForLikeConstruction(atStatementStart)) {
          return myFactory.createIndentCalculator(NONE, position -> statementStart);
        }
      }
      else if (isInArray(editor, offset)) {
        return myFactory.createIndentCalculator(getIndentInBrackets(), IndentCalculator.LINE_BEFORE);
      }
      else if (getPosition(editor, offset).before().isAt(LeftParenthesis)) {
        return myFactory.createIndentCalculator(CONTINUATION, IndentCalculator.LINE_BEFORE);
      }
      else if (getPosition(editor, offset).matchesRule(
        position -> {
          moveBeforeEndLineComments(position);
          if (position.isAt(BlockOpeningBrace)) {
            return !position.before().beforeOptionalMix(LineComment, BlockComment, Whitespace).isAt(LeftParenthesis);
          }
          return false;
        }
      )) {
        SemanticEditorPosition position = getPosition(editor, offset).before().beforeOptionalMix(LineComment, BlockComment, Whitespace);
        return myFactory.createIndentCalculator(getIndentInBlock(project, language, position), this::getBlockStatementStartOffset);
      }
      else if (getPosition(editor, offset).before().matchesRule(
        position -> isColonAfterLabelOrCase(position) || position.isAtAnyOf(ElseKeyword, DoKeyword))) {
        return myFactory.createIndentCalculator(NORMAL, IndentCalculator.LINE_BEFORE);
      }
      else if (getPosition(editor, offset).matchesRule(
        position -> {
          position.moveBefore();
          if (position.isAt(BlockComment)) {
            return position.before().isAt(Whitespace) && position.isAtMultiline();
          }
          return false;
        }
      )) {
        int offsetBeforeComment = getPosition(editor, offset).findStartOf(BlockComment);
        return getIndent(project, editor, language, offsetBeforeComment);
      }
      else if (getPosition(editor, offset).before().isAt(DocBlockEnd)) {
        return myFactory.createIndentCalculator(NONE, position -> position.findStartOf(DocBlockStart));
      }
      else {
        SemanticEditorPosition position = getPosition(editor, offset);
        position = position.before().beforeOptionalMix(LineComment, BlockComment, Whitespace);
        if (position.isAt(RightParenthesis)) {
          int offsetAfterParen = position.getStartOffset() + 1;
          position.moveBeforeParentheses(LeftParenthesis, RightParenthesis);
          if (!position.isAtEnd()) {
            position.moveBeforeOptional(Whitespace);
            if (position.isAt(IfKeyword) || position.isAt(ForKeyword)) {
              SyntaxElement element = position.getCurrElement();
              assert element != null;
              final int controlKeywordOffset = position.getStartOffset();
              Type indentType = getPosition(editor, offsetAfterParen).afterOptional(Whitespace).isAt(BlockOpeningBrace) ? NONE : NORMAL;
              return myFactory.createIndentCalculator(indentType, baseLineOffset -> controlKeywordOffset);
            }
          }
        }
      }
    }
    //return myFactory.createIndentCalculator(NONE, IndentCalculator.LINE_BEFORE); /* TO CHECK UNCOVERED CASES */
    return null;
  }

  private SemanticEditorPosition afterOptionalWhitespaceOnSameLine(@NotNull Editor editor, int offset) {
    SemanticEditorPosition position = getPosition(editor, offset);
    if (position.isAt(Whitespace)) {
      if (position.hasLineBreaksAfter(offset)) return position;
      position.moveAfter();
    }
    return position;
  }

  /**
   * Checks that the current offset is inside array. By default it is assumed to be after opening array bracket
   * but can be overridden for more complicated logic, for example, the following case in Java: []{&lt;caret&gt;}.
   *
   * @param editor The editor.
   * @param offset The current offset in the editor.
   * @return {@code true} if the position is inside array.
   */
  protected boolean isInArray(@NotNull Editor editor, int offset) {
    return getPosition(editor, offset).before().isAt(ArrayOpeningBracket);
  }

  /**
   * Checking the document context in position for return-like token (i.e. {@code return}, {@code break}, {@code continue}),
   * after that we need to reduce the indent (for example after {@code break;} in {@code switch} statement).
   *
   * @param statementBeforeSemicolon position in the document context
   * @return true, if need to reduce the indent
   */
  protected boolean dropIndentAfterReturnLike(@NotNull SemanticEditorPosition statementBeforeSemicolon) {
    return false;
  }

  protected boolean isColonAfterLabelOrCase(@NotNull SemanticEditorPosition position) {
    return position.isAt(Colon)
           && getPosition(position.getEditor(), position.getStartOffset()).isAfterOnSameLine(SwitchCase, SwitchDefault);
  }

  protected boolean isInsideForLikeConstruction(SemanticEditorPosition position) {
    return position.isAfterOnSameLine(ForKeyword);
  }

  /**
   * Returns the start offset of the statement or new-line-'{' that owns the code block in {@code position}.
   * 
   * Custom implementation for language can overwrite the default behavior for multi-lines statements like
   * <pre>{@code
   *    template<class T>
   *    class A {};
   * }</pre>
   * or check indentation after new-line-'{' vs the brace style.
   * 
   * @param position the position in the code block
   */
  protected int getBlockStatementStartOffset(@NotNull SemanticEditorPosition position) {
    moveBeforeEndLineComments(position);
    position.moveBeforeOptional(BlockOpeningBrace);
    if (position.isAt(Whitespace)) {
      if (position.isAtMultiline()) {
        return position.after().getStartOffset();
      }
      position.moveBefore();
    }
    return getStatementStartOffset(position, false);
  }

  private static void moveBeforeEndLineComments(@NotNull SemanticEditorPosition position) {
    position.moveBefore();
    while (!position.isAtMultiline() && position.isAtAnyOf(LineComment, BlockComment, Whitespace)) {
      position.moveBefore();
    }
  }

  /**
   * Returns the start offset of the statement that owns the code block in {@code position}  
   * 
   * @param position the position in the code block
   */
  protected int getDeepBlockStatementStartOffset(@NotNull SemanticEditorPosition position) {
    position.moveToLeftParenthesisBackwardsSkippingNested(BlockOpeningBrace, BlockClosingBrace);
    return getBlockStatementStartOffset(position);
  }

  private int getStatementStartOffset(@NotNull SemanticEditorPosition position, boolean ignoreLabels) {
    Language currLanguage = position.getLanguage();
    while (!position.isAtEnd()) {
      if (currLanguage == Language.ANY || currLanguage == null) currLanguage = position.getLanguage();
      if (!ignoreLabels && isColonAfterLabelOrCase(position)) {
        SemanticEditorPosition afterColon = getPosition(position.getEditor(), position.getStartOffset())
          .afterOptionalMix(Whitespace, BlockComment)
          .after()
          .afterOptionalMix(Whitespace, LineComment);
        return afterColon.getStartOffset();
      }
      else if (position.isAt(RightParenthesis)) {
        position.moveBeforeParentheses(LeftParenthesis, RightParenthesis);
        continue;
      }
      else if (position.isAt(BlockClosingBrace)) {
        position.moveBeforeParentheses(BlockOpeningBrace, BlockClosingBrace);
        continue;
      }
      else if (position.isAt(ArrayClosingBracket)) {
        position.moveBeforeParentheses(ArrayOpeningBracket, ArrayClosingBracket);
        continue;
      }
      else if (isStartOfStatementWithOptionalBlock(position)) {
        return position.getStartOffset();
      }
      else if (position.isAtAnyOf(Semicolon,
                                  BlockOpeningBrace, 
                                  BlockComment, 
                                  DocBlockEnd, 
                                  LeftParenthesis,
                                  LanguageStartDelimiter) ||
               (position.getLanguage() != Language.ANY) && !position.isAtLanguage(currLanguage)) {
        SemanticEditorPosition statementStart = position.copy();
        statementStart = statementStart.after().afterOptionalMix(Whitespace, LineComment);
        if (!isIndentProvider(statementStart, ignoreLabels)) {
          final SemanticEditorPosition maybeColon = statementStart.afterOptionalMix(Whitespace, BlockComment).after();
          final SemanticEditorPosition afterColonStatement = maybeColon.after().after();
          if (atColonWithNewLineAfterColonStatement(maybeColon, afterColonStatement)) {
            return afterColonStatement.getStartOffset();
          }
          if (atBlockStartAndNeedBlockIndent(position)) {
            return position.getStartOffset();
          }
        }
        else if (!statementStart.isAtEnd()) {
          return statementStart.getStartOffset();
        }
      }
      position.moveBefore();
    }
    return 0;
  }

  /**
   * Returns {@code true} if the {@code position} starts a statement that <i>can</i> have a code block and the statement
   * is the first in the code line.
   * In C-like languages it is one of {@code if, else, for, while, do, try}. 
   * 
   * @param position
   */
  protected boolean isStartOfStatementWithOptionalBlock(@NotNull SemanticEditorPosition position) {
    return position.matchesRule(
      self -> {
        final SemanticEditorPosition before = self.before();
        return before.isAt(Whitespace)
               && before.isAtMultiline()
               && self.isAtAnyOf(ElseKeyword,
                                 IfKeyword,
                                 ForKeyword,
                                 TryKeyword,
                                 DoKeyword);
      });
  }

  private static boolean atBlockStartAndNeedBlockIndent(@NotNull SemanticEditorPosition position) {
    return position.isAt(BlockOpeningBrace);
  }

  private static boolean atColonWithNewLineAfterColonStatement(@NotNull SemanticEditorPosition maybeColon,
                                                               @NotNull SemanticEditorPosition afterColonStatement) {
    return maybeColon.isAt(Colon)
           && maybeColon.after().isAtMultiline(Whitespace)
           && !afterColonStatement.isAtEnd();
  }


  /**
   * Search for the topmost control structure keyword which doesn't have any code block delimiters like {@code {...}}. For example:
   * <pre>
   * 1  for (...)
   * 2      if (...)
   * 3          for(...) {}
   * 4  [position]
   * </pre>
   * The method will return an offset of the first {@code for} on line 1.
   * @return
   */
  private int getFirstUppermostControlStructureKeywordOffset(@NotNull SemanticEditorPosition position) {
    SemanticEditorPosition curr = position.copy();
    while (!curr.isAtEnd()) {
      if (isStartOfStatementWithOptionalBlock(curr)) {
        SemanticEditorPosition candidate = curr.copy();
        curr.moveBefore();
        curr.moveBeforeOptionalMix(Whitespace, LineComment, BlockComment);
        if (!curr.isAt(RightParenthesis)) {
          return candidate.getStartOffset();
        }
        else {
          curr.moveBeforeParentheses(LeftParenthesis, RightParenthesis);
          continue;
        }
      }
      else if (curr.isAt(BlockClosingBrace)) {
        curr.moveBeforeParentheses(BlockOpeningBrace, BlockClosingBrace);
        continue;
      }
      curr.moveBefore();
    }
    return position.before().getStartOffset();
  }

  /**
   * Checking the document context in position as indent-provider.
   *
   * @param statementStartPosition position is the document
   * @param ignoreLabels {@code true}, if labels cannot be used as indent-providers in the context.
   * @return {@code true}, if statement is indent-provider (by default)
   */
  protected boolean isIndentProvider(@NotNull SemanticEditorPosition statementStartPosition, boolean ignoreLabels) {
    return true;
  }

  /**
   * Returns abstract semantic position in {@code editor} for indent calculation.  
   * 
   * @param editor the editor in action   
   * @param offset the offset in the {@code editor}    
   */
  public SemanticEditorPosition getPosition(@NotNull Editor editor, int offset) {
    return SemanticEditorPosition.createEditorPosition((EditorEx)editor, offset,
                                                       (_editor, _offset) -> getIteratorAtPosition(_editor, _offset),
                                                       tokenType -> mapType(tokenType));
  }

  @NotNull
  protected HighlighterIterator getIteratorAtPosition(@NotNull EditorEx editor, int offset) {
    return editor.getHighlighter().createIterator(offset);
  }

  @Nullable
  protected abstract SyntaxElement mapType(@NotNull IElementType tokenType);
  
  
  @Nullable
  protected Indent getIndentInBlock(@NotNull Project project,
                                    @Nullable Language language,
                                    @NotNull SemanticEditorPosition blockStartPosition) {
    if (language != null) {
      CommonCodeStyleSettings settings = CodeStyle.getSettings(blockStartPosition.getEditor()).getCommonSettings(language);
      if (settings.BRACE_STYLE == CommonCodeStyleSettings.NEXT_LINE_SHIFTED) {
        return getDefaultIndentFromType(settings.METHOD_BRACE_STYLE == CommonCodeStyleSettings.NEXT_LINE_SHIFTED ? NONE : null);
      }
    }
    return getDefaultIndentFromType(NORMAL);
  }
  
  @Contract("_, null -> null")
  private static Type getBlockIndentType(@NotNull Editor editor, @Nullable Language language) {
    if (language != null) {
      CommonCodeStyleSettings settings = CodeStyle.getSettings(editor).getCommonSettings(language);
      if (settings.BRACE_STYLE == CommonCodeStyleSettings.NEXT_LINE || settings.BRACE_STYLE == CommonCodeStyleSettings.END_OF_LINE) {
        return NONE;
      }
    }
    return null;
  }

  @Contract("null -> null")
  protected static Indent getDefaultIndentFromType(@Nullable Type type) {
    return type == null
           ? null
           : Indent.getIndent(type, 0, false, false);
  }
  
  public static class IndentCalculatorFactory {
    private final Project myProject;
    private final Editor myEditor;

    public IndentCalculatorFactory(Project project, Editor editor) {
      myProject = project;
      myEditor = editor;
    }

    @Nullable
    public IndentCalculator createIndentCalculator(@Nullable Type indentType, @Nullable BaseLineOffsetCalculator baseLineOffsetCalculator) {
      return createIndentCalculator(getDefaultIndentFromType(indentType), baseLineOffsetCalculator);
    }

    @Nullable
    public IndentCalculator createIndentCalculator(@Nullable Indent indent, @Nullable BaseLineOffsetCalculator baseLineOffsetCalculator) {
      return indent != null ?
             new IndentCalculator(myProject,
                                  myEditor,
                                  baseLineOffsetCalculator != null
                                  ? baseLineOffsetCalculator
                                  : IndentCalculator.LINE_BEFORE,
                                  indent)
                            : null;
    }
  }

  @Override
  @Contract("null -> false")
  public final boolean isSuitableFor(@Nullable Language language) {
    return language != null && isSuitableForLanguage(language);
  }
  
  public abstract boolean isSuitableForLanguage(@NotNull Language language);

  protected Type getIndentTypeInBrackets() {
    return CONTINUATION;
  }

  protected Indent getIndentInBrackets() {
    return getDefaultIndentFromType(getIndentTypeInBrackets());
  }
}
