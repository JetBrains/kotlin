// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.highlighting;

import com.intellij.ide.highlighter.custom.SyntaxTable;
import com.intellij.ide.highlighter.custom.impl.CustomFileTypeBraceMatcher;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageBraceMatching;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

public final class BraceMatchingUtil {
  public static final int UNDEFINED_TOKEN_GROUP = -1;

  private BraceMatchingUtil() {
  }

  public static boolean isPairedBracesAllowedBeforeTypeInFileType(@NotNull IElementType lbraceType,
                                                                  final IElementType tokenType,
                                                                  @NotNull FileType fileType) {
    try {
      return getBraceMatcher(fileType, lbraceType).isPairedBracesAllowedBeforeType(lbraceType, tokenType);
    }
    catch (AbstractMethodError incompatiblePluginThatWeDoNotCare) {
      // Do nothing
    }
    return true;
  }

  @TestOnly
  public static int getMatchedBraceOffset(@NotNull Editor editor, boolean forward, @NotNull PsiFile file) {
    Document document = editor.getDocument();
    int offset = editor.getCaretModel().getOffset();
    EditorHighlighter editorHighlighter = BraceHighlightingHandler.getLazyParsableHighlighterIfAny(file.getProject(), editor, file);
    HighlighterIterator iterator = editorHighlighter.createIterator(offset);
    boolean matched = matchBrace(document.getCharsSequence(), file.getFileType(), iterator, forward);
    if (!matched) throw new AssertionError();
    return iterator.getStart();
  }

  /**
   * @see #computeHighlightingAndNavigationContext(Editor, PsiFile, int)
   */
  @Nullable
  public static BraceHighlightingAndNavigationContext computeHighlightingAndNavigationContext(@NotNull Editor editor,
                                                                                              @NotNull PsiFile file) {
    return computeHighlightingAndNavigationContext(editor, file, editor.getCaretModel().getOffset());
  }

  /**
   * Computes context that should be used to highlight and navigate matching braces
   *
   * @param offset offset we are computing for. Some implementations may need to compute this not only for caret position, but for other offsets, e.g. skipping spaces behind or ahead.
   * @return a context should be used or null if there is no matching braces at offset
   * @apiNote this method contains a logic for selecting braces in different complicated situations, like {@code (<caret>(} and so on.
   * It does not looks forward/behind skipping spaces, like highlighting does.
   */
  @Nullable
  public static BraceHighlightingAndNavigationContext computeHighlightingAndNavigationContext(@NotNull Editor editor,
                                                                                              @NotNull PsiFile file,
                                                                                              int offset) {
    final EditorHighlighter highlighter = BraceHighlightingHandler.getLazyParsableHighlighterIfAny(file.getProject(), editor, file);
    final CharSequence text = editor.getDocument().getCharsSequence();

    final HighlighterIterator iterator = highlighter.createIterator(offset);
    final FileType fileType = iterator.atEnd() ? null : getFileType(file, iterator.getStart());

    boolean isBeforeOrInsideLeftBrace = fileType != null && isLBraceToken(iterator, text, fileType);
    boolean isBeforeOrInsideRightBrace = !isBeforeOrInsideLeftBrace && fileType != null && isRBraceToken(iterator, text, fileType);
    boolean isInsideBrace = (isBeforeOrInsideLeftBrace || isBeforeOrInsideRightBrace) && iterator.getStart() < offset;

    final HighlighterIterator preOffsetIterator = offset > 0 && !isInsideBrace ? highlighter.createIterator(offset - 1) : null;
    final FileType preOffsetFileType = preOffsetIterator != null ? getFileType(file, preOffsetIterator.getStart()) : null;

    boolean isAfterLeftBrace = preOffsetIterator != null &&
                               isLBraceToken(preOffsetIterator, text, preOffsetFileType);
    boolean isAfterRightBrace = !isAfterLeftBrace && preOffsetIterator != null &&
                                isRBraceToken(preOffsetIterator, text, preOffsetFileType);

    int offsetTokenStart = iterator.atEnd() ? -1 : iterator.getStart();
    int preOffsetTokenStart = preOffsetIterator == null || preOffsetIterator.atEnd() ? -1 : preOffsetIterator.getStart();

    if (editor.getSettings().isBlockCursor()) {
      if (isBeforeOrInsideLeftBrace && matchBrace(text, fileType, iterator, true)) {
        return new BraceHighlightingAndNavigationContext(offsetTokenStart, iterator.getStart(), isInsideBrace);
      }
      else if (isBeforeOrInsideRightBrace && matchBrace(text, fileType, iterator, false)) {
        return new BraceHighlightingAndNavigationContext(offsetTokenStart, iterator.getStart(), isInsideBrace);
      }
      else if (isAfterRightBrace && matchBrace(text, preOffsetFileType, preOffsetIterator, false)) {
        return new BraceHighlightingAndNavigationContext(preOffsetTokenStart, preOffsetIterator.getStart(), true);
      }
      else if (isAfterLeftBrace && matchBrace(text, preOffsetFileType, preOffsetIterator, true)) {
        return new BraceHighlightingAndNavigationContext(preOffsetTokenStart, preOffsetIterator.getStart(), true);
      }
    }
    else {
      if (isAfterRightBrace && matchBrace(text, preOffsetFileType, preOffsetIterator, false)) {
        return new BraceHighlightingAndNavigationContext(preOffsetTokenStart, preOffsetIterator.getStart(), true);
      }
      else if (isBeforeOrInsideLeftBrace && matchBrace(text, fileType, iterator, true)) {
        return new BraceHighlightingAndNavigationContext(offsetTokenStart, iterator.getEnd(), isInsideBrace);
      }
      else if (isAfterLeftBrace && matchBrace(text, preOffsetFileType, preOffsetIterator, true)) {
        return new BraceHighlightingAndNavigationContext(preOffsetTokenStart, preOffsetIterator.getEnd(), true);
      }
      else if (isBeforeOrInsideRightBrace && matchBrace(text, fileType, iterator, false)) {
        return new BraceHighlightingAndNavigationContext(offsetTokenStart, iterator.getStart(), isInsideBrace);
      }
    }
    return null;
  }

  @NotNull
  public static FileType getFileType(PsiFile file, int offset) {
    return PsiUtilBase.getPsiFileAtOffset(file, offset).getFileType();
  }

  private static class MatchBraceContext {
    private final CharSequence fileText;
    private final FileType fileType;
    private final HighlighterIterator iterator;
    private final boolean forward;

    private final IElementType brace1Token;
    private final int group;
    private final String brace1TagName;
    private final boolean isStrict;
    private final boolean isCaseSensitive;
    @NotNull
    private final BraceMatcher myMatcher;

    private final Stack<IElementType> myBraceStack = new Stack<>();
    private final Stack<String> myTagNameStack = new Stack<>();

    MatchBraceContext(@NotNull CharSequence fileText, @NotNull FileType fileType, @NotNull HighlighterIterator iterator, boolean forward) {
      this(fileText, fileType, iterator, forward,
           isStrictTagMatching(getBraceMatcher(fileType, iterator), fileType, getTokenGroup(iterator.getTokenType(), fileType)));
    }

    MatchBraceContext(@NotNull CharSequence fileText,
                      @NotNull FileType fileType,
                      @NotNull HighlighterIterator iterator,
                      boolean forward,
                      boolean strict) {
      this.fileText = fileText;
      this.fileType = fileType;
      this.iterator = iterator;
      this.forward = forward;

      myMatcher = getBraceMatcher(fileType, iterator);
      brace1Token = this.iterator.getTokenType();
      group = getTokenGroup(brace1Token, this.fileType);
      brace1TagName = getTagName(myMatcher, this.fileText, this.iterator);

      isCaseSensitive = areTagsCaseSensitive(myMatcher, this.fileType, group);
      isStrict = strict;
    }

    boolean doBraceMatch() {
      myBraceStack.clear();
      myTagNameStack.clear();
      myBraceStack.push(brace1Token);
      if (isStrict) {
        myTagNameStack.push(brace1TagName);
      }
      boolean matched = false;
      while (true) {
        advance(iterator, forward);
        if (iterator.atEnd()) {
          break;
        }

        IElementType tokenType = iterator.getTokenType();

        if (getTokenGroup(tokenType, fileType) != group) {
          continue;
        }
        String tagName = getTagName(myMatcher, fileText, iterator);
        if (!isStrict && !Comparing.equal(brace1TagName, tagName, isCaseSensitive)) continue;
        if (isBraceToken(iterator, fileText, fileType, !forward)) {
          myBraceStack.push(tokenType);
          if (isStrict) {
            myTagNameStack.push(tagName);
          }
        }
        else if (isBraceToken(iterator, fileText, fileType, forward)) {
          IElementType topTokenType = myBraceStack.pop();
          String topTagName = null;
          if (isStrict) {
            topTagName = myTagNameStack.pop();
          }

          if (!isStrict) {
            final IElementType baseType = myMatcher.getOppositeBraceTokenType(tokenType);
            if (myBraceStack.contains(baseType)) {
              while (!isPairBraces(topTokenType, tokenType, fileType) && !myBraceStack.empty()) {
                topTokenType = myBraceStack.pop();
              }
            }
            else if ((brace1TagName == null || !brace1TagName.equals(tagName)) && !isPairBraces(topTokenType, tokenType, fileType)) {
              // Ignore non-matched opposite-direction brace for non-strict processing.
              myBraceStack.push(topTokenType);
              continue;
            }
          }

          if (!isPairBraces(topTokenType, tokenType, fileType)
              || isStrict && !Comparing.equal(topTagName, tagName, isCaseSensitive)) {
            matched = false;
            break;
          }

          if (myBraceStack.isEmpty()) {
            matched = true;
            break;
          }
        }
      }
      return matched;
    }
  }

  public static synchronized boolean matchBrace(@NotNull CharSequence fileText,
                                                @NotNull FileType fileType,
                                                @NotNull HighlighterIterator iterator,
                                                boolean forward) {
    return new MatchBraceContext(fileText, fileType, iterator, forward).doBraceMatch();
  }


  public static synchronized boolean matchBrace(@NotNull CharSequence fileText,
                                                @NotNull FileType fileType,
                                                @NotNull HighlighterIterator iterator,
                                                boolean forward,
                                                boolean isStrict) {
    return new MatchBraceContext(fileText, fileType, iterator, forward, isStrict).doBraceMatch();
  }

  public static boolean findStructuralLeftBrace(@NotNull FileType fileType,
                                                @NotNull HighlighterIterator iterator,
                                                @NotNull CharSequence fileText) {
    final Stack<IElementType> braceStack = new Stack<>();
    final Stack<String> tagNameStack = new Stack<>();

    BraceMatcher matcher = getBraceMatcher(fileType, iterator);

    while (!iterator.atEnd()) {
      if (isStructuralBraceToken(fileType, iterator, fileText)) {
        if (isRBraceToken(iterator, fileText, fileType)) {
          braceStack.push(iterator.getTokenType());
          tagNameStack.push(getTagName(matcher, fileText, iterator));
        }
        if (isLBraceToken(iterator, fileText, fileType)) {
          if (braceStack.isEmpty()) return true;

          final int group = matcher.getBraceTokenGroupId(iterator.getTokenType());

          final IElementType topTokenType = braceStack.pop();
          final IElementType tokenType = iterator.getTokenType();

          boolean isStrict = isStrictTagMatching(matcher, fileType, group);
          boolean isCaseSensitive = areTagsCaseSensitive(matcher, fileType, group);

          String topTagName = null;
          String tagName = null;
          if (isStrict) {
            topTagName = tagNameStack.pop();
            tagName = getTagName(matcher, fileText, iterator);
          }

          if (!isPairBraces(topTokenType, tokenType, fileType)
              || isStrict && !Comparing.equal(topTagName, tagName, isCaseSensitive)) {
            return false;
          }
        }
      }

      iterator.retreat();
    }

    return false;
  }

  public static boolean isStructuralBraceToken(@NotNull FileType fileType,
                                               @NotNull HighlighterIterator iterator,
                                               @NotNull CharSequence text) {
    BraceMatcher matcher = getBraceMatcher(fileType, iterator);
    return matcher.isStructuralBrace(iterator, text, fileType);
  }

  public static boolean isLBraceToken(@NotNull HighlighterIterator iterator, @NotNull CharSequence fileText, @NotNull FileType fileType) {
    final BraceMatcher braceMatcher = getBraceMatcher(fileType, iterator);

    return braceMatcher.isLBraceToken(iterator, fileText, fileType);
  }

  public static boolean isRBraceToken(@NotNull HighlighterIterator iterator, @NotNull CharSequence fileText, @NotNull FileType fileType) {
    final BraceMatcher braceMatcher = getBraceMatcher(fileType, iterator);

    return braceMatcher.isRBraceToken(iterator, fileText, fileType);
  }

  public static boolean isPairBraces(@NotNull IElementType tokenType1, @NotNull IElementType tokenType2, @NotNull FileType fileType) {
    BraceMatcher matcher = getBraceMatcher(fileType, tokenType1);
    return matcher.isPairBraces(tokenType1, tokenType2);
  }

  private static int getTokenGroup(@Nullable IElementType tokenType, FileType fileType) {
    return tokenType == null ? -1 : getBraceMatcher(fileType, tokenType).getBraceTokenGroupId(tokenType);
  }

  public static int findLeftmostLParen(@NotNull HighlighterIterator iterator,
                                       @NotNull IElementType lparenTokenType,
                                       @NotNull CharSequence fileText,
                                       @NotNull FileType fileType) {
    return findLeftOrRightParenth(iterator, lparenTokenType, fileText, fileType, false, false);
  }

  public static int findLeftLParen(@NotNull HighlighterIterator iterator,
                                   @NotNull IElementType lparenTokenType,
                                   @NotNull CharSequence fileText,
                                   @NotNull FileType fileType) {
    return findLeftOrRightParenth(iterator, lparenTokenType, fileText, fileType, false, true);
  }

  public static int findRightmostRParen(@NotNull HighlighterIterator iterator,
                                        @NotNull IElementType rparenTokenType,
                                        @NotNull CharSequence fileText,
                                        @NotNull FileType fileType) {
    return findLeftOrRightParenth(iterator, rparenTokenType, fileText, fileType, true, false);
  }

  /**
   * Goes through the iterator and finds the left or right parenth corresponding to the provided options
   *
   * @param iterator iterator of the file
   * @param targetParenTokenType the parenth token type you're searching for.
   * @param fileText text of the file
   * @param fileType the file's type
   * @param searchingForRight are we going to the right and searching for the right parenth? (e.g. ')', ']', '}')
   * @param stopOnFirstFinishedGroup are we searching for the last corresponding brace in a line of wrapped braces?
   *                                   Examples: `<caret>[{}]()(){}()` if stopOnFirstFinishedGroup == true
   *                                                            ^
   *
   *                                             `<caret>[{}]()(){}()` if stopOnFirstFinishedGroup == true
   *                                                          ^
   * @return the offset of the found parenth or -1
   */
  private static int findLeftOrRightParenth(@NotNull HighlighterIterator iterator,
                                            @NotNull IElementType targetParenTokenType,
                                            @NotNull CharSequence fileText,
                                            @NotNull FileType fileType,
                                            boolean searchingForRight,
                                            boolean stopOnFirstFinishedGroup) {
    int lastBraceOffset = -1;

    Stack<IElementType> braceStack = new Stack<>();
    while (!iterator.atEnd()) {
      IElementType tokenType = iterator.getTokenType();

      if (isBraceToken(iterator, fileText, fileType, searchingForRight)) {
        if (braceStack.isEmpty()) {
          if (tokenType != targetParenTokenType) break; //unmatched braces

          lastBraceOffset = iterator.getStart();
          if (stopOnFirstFinishedGroup) break; //first brace group finished. stop searching
        }
        else {
          IElementType topToken = braceStack.pop();
          if (!isPairBraces(tokenType, topToken, fileType)) break; // unmatched braces
        }
      }
      else if (isBraceToken(iterator, fileText, fileType, !searchingForRight)) {
        braceStack.push(iterator.getTokenType());
      }

      advance(iterator, searchingForRight);
    }

    return lastBraceOffset;
  }

  private static boolean isBraceToken(@NotNull HighlighterIterator iterator,
                                      @NotNull CharSequence fileText,
                                      @NotNull FileType fileType,
                                      boolean searchingForRight) {
    return searchingForRight ? isRBraceToken(iterator, fileText, fileType)
                             : isLBraceToken(iterator, fileText, fileType);
  }

  private static void advance(@NotNull HighlighterIterator iterator, boolean forward) {
    if (forward) {
      iterator.advance();
    }
    else {
      iterator.retreat();
    }
  }

  private static class BraceMatcherHolder {
    private static final BraceMatcher ourDefaultBraceMatcher = new DefaultBraceMatcher();
    private static final BraceMatcher nullBraceMatcher = new DefaultBraceMatcher();
  }

  @NotNull
  public static BraceMatcher getBraceMatcher(@NotNull FileType fileType, @NotNull HighlighterIterator iterator) {
    IElementType tokenType = iterator.getTokenType();
    return tokenType == null ? BraceMatcherHolder.ourDefaultBraceMatcher : getBraceMatcher(fileType, tokenType);
  }

  @NotNull
  public static BraceMatcher getBraceMatcher(@NotNull FileType fileType, @NotNull IElementType type) {
    return getBraceMatcher(fileType, type.getLanguage());
  }

  @NotNull
  public static BraceMatcher getBraceMatcher(@NotNull FileType fileType, @NotNull Language lang) {
    PairedBraceMatcher matcher = LanguageBraceMatching.INSTANCE.forLanguage(lang);
    if (matcher != null) {
      if (matcher instanceof XmlAwareBraceMatcher) {
        return (XmlAwareBraceMatcher)matcher;
      }
      else if (matcher instanceof PairedBraceMatcherAdapter) {
        return (BraceMatcher)matcher;
      }
      else {
        return new PairedBraceMatcherAdapter(matcher, lang);
      }
    }

    final BraceMatcher byFileType = getBraceMatcherByFileType(fileType);
    if (byFileType != null) return byFileType;

    if (fileType instanceof LanguageFileType) {
      final Language language = ((LanguageFileType)fileType).getLanguage();
      if (lang != language) {
        final FileType type1 = lang.getAssociatedFileType();
        if (type1 != null) {
          final BraceMatcher braceMatcher = getBraceMatcherByFileType(type1);
          if (braceMatcher != null) {
            return braceMatcher;
          }
        }

        matcher = LanguageBraceMatching.INSTANCE.forLanguage(language);
        if (matcher != null) {
          return new PairedBraceMatcherAdapter(matcher, language);
        }
      }
    }

    return BraceMatcherHolder.ourDefaultBraceMatcher;
  }

  @Nullable
  private static BraceMatcher getBraceMatcherByFileType(@NotNull FileType fileType) {
    BraceMatcher matcher = FileTypeBraceMather.getInstance().forFileType(fileType);
    if (matcher != null) return matcher;

    if (fileType instanceof AbstractFileType) {
      SyntaxTable table = ((AbstractFileType)fileType).getSyntaxTable();
      if (table.isHasBraces() || table.isHasBrackets() || table.isHasParens()) {
        return CustomFileTypeBraceMatcher.INSTANCE;
      }
    }

    return null;
  }

  private static boolean isStrictTagMatching(@NotNull BraceMatcher matcher, @NotNull FileType fileType, final int group) {
    return matcher instanceof XmlAwareBraceMatcher && ((XmlAwareBraceMatcher)matcher).isStrictTagMatching(fileType, group);
  }

  private static boolean areTagsCaseSensitive(@NotNull BraceMatcher matcher, @NotNull FileType fileType, final int tokenGroup) {
    return matcher instanceof XmlAwareBraceMatcher && ((XmlAwareBraceMatcher)matcher).areTagsCaseSensitive(fileType, tokenGroup);
  }

  @Nullable
  private static String getTagName(@NotNull BraceMatcher matcher, @NotNull CharSequence fileText, @NotNull HighlighterIterator iterator) {
    if (matcher instanceof XmlAwareBraceMatcher) return ((XmlAwareBraceMatcher)matcher).getTagName(fileText, iterator);
    return null;
  }

  private static class DefaultBraceMatcher implements BraceMatcher {
    @Override
    public int getBraceTokenGroupId(final IElementType tokenType) {
      return UNDEFINED_TOKEN_GROUP;
    }

    @Override
    public boolean isLBraceToken(final HighlighterIterator iterator, final CharSequence fileText, final FileType fileType) {
      return false;
    }

    @Override
    public boolean isRBraceToken(final HighlighterIterator iterator, final CharSequence fileText, final FileType fileType) {
      return false;
    }

    @Override
    public boolean isPairBraces(final IElementType tokenType, final IElementType tokenType2) {
      return false;
    }

    @Override
    public boolean isStructuralBrace(final HighlighterIterator iterator, final CharSequence text, final FileType fileType) {
      return false;
    }

    @Override
    public IElementType getOppositeBraceTokenType(@NotNull final IElementType type) {
      return null;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull final IElementType lbraceType, @Nullable final IElementType contextType) {
      return true;
    }

    @Override
    public int getCodeConstructStart(final PsiFile file, final int openingBraceOffset) {
      return openingBraceOffset;
    }
  }

  /**
   * Describes a brace matching/navigation context computed by {@link #computeHighlightingAndNavigationContext}
   */
  public static final class BraceHighlightingAndNavigationContext {
    public final int currentBraceOffset;
    public final int navigationOffset;
    public final boolean isCaretAfterBrace;

    public BraceHighlightingAndNavigationContext(int currentBraceOffset, int navigationOffset, boolean isCaretAfterBrace) {
      this.currentBraceOffset = currentBraceOffset;
      this.navigationOffset = navigationOffset;
      this.isCaretAfterBrace = isCaretAfterBrace;
    }
  }
}
