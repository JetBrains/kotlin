// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl;

import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.highlighting.HighlightManagerImpl;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.find.*;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.impl.livePreview.SearchResults;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.ParserDefinition;
import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.Lexer;
import com.intellij.navigation.NavigationItem;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.StringPattern;
import com.intellij.psi.*;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.ReplacePromptDialog;
import com.intellij.usages.ChunkExtractor;
import com.intellij.usages.impl.SyntaxHighlighterOverEditorHighlighter;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.IntObjectMap;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.ImmutableCharSequence;
import com.intellij.util.text.StringSearcher;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FindManagerImpl extends FindManager {
  private static final Logger LOG = Logger.getInstance(FindManagerImpl.class);

  private final FindUsagesManager myFindUsagesManager;
  private boolean isFindWasPerformed;
  private boolean isSelectNextOccurrenceWasPerformed;
  private Point myReplaceInFilePromptPos = new Point(-1, -1);
  private Point myReplaceInProjectPromptPos = new Point(-1, -1);
  private final FindModel myFindInProjectModel = new FindModel();
  private final FindModel myFindInFileModel = new FindModel();
  private FindModel myFindNextModel;
  private FindModel myPreviousFindModel;
  private static final FindResultImpl NOT_FOUND_RESULT = new FindResultImpl();
  private final Project myProject;
  private static final Key<Boolean> HIGHLIGHTER_WAS_NOT_FOUND_KEY = Key.create("com.intellij.find.impl.FindManagerImpl.HighlighterNotFoundKey");

  private FindUIHelper myHelper;
  private static final NotificationGroup GROUP = new NotificationGroup("Find Problems", NotificationDisplayType.STICKY_BALLOON, false);

  public FindManagerImpl(@NotNull Project project) {
    myProject = project;

    FindSettings findSettings = FindSettings.getInstance();
    findSettings.initModelBySetings(myFindInProjectModel);

    myFindInFileModel.setCaseSensitive(findSettings.isLocalCaseSensitive());
    myFindInFileModel.setWholeWordsOnly(findSettings.isLocalWholeWordsOnly());
    myFindInFileModel.setRegularExpressions(findSettings.isLocalRegularExpressions());

    myFindUsagesManager = new FindUsagesManager(myProject);
    myFindInProjectModel.setMultipleFiles(true);

    NotificationsConfigurationImpl.remove("FindInPath");
    Disposer.register(project, () -> {
      if (myHelper != null) {
        Disposer.dispose(myHelper);
      }
    });
  }

  @Override
  public FindModel createReplaceInFileModel() {
    FindModel model = new FindModel();
    model.copyFrom(getFindInFileModel());
    model.setReplaceState(true);
    model.setPromptOnReplace(false);
    return model;
  }

  @Override
  public int showPromptDialog(@NotNull final FindModel model, String title) {
    return showPromptDialogImpl(model, title, null);
  }

  @PromptResultValue
  private int showPromptDialogImpl(@NotNull final FindModel model,
                                   @NlsContexts.DialogTitle String title,
                                   @Nullable final MalformedReplacementStringException exception) {
    ReplacePromptDialog replacePromptDialog = new ReplacePromptDialog(model.isMultipleFiles(), title, myProject, exception) {
      @Override
      @Nullable
      public Point getInitialLocation() {
        if (model.isMultipleFiles() && myReplaceInProjectPromptPos.x >= 0 && myReplaceInProjectPromptPos.y >= 0){
          return myReplaceInProjectPromptPos;
        }
        if (!model.isMultipleFiles() && myReplaceInFilePromptPos.x >= 0 && myReplaceInFilePromptPos.y >= 0){
          return myReplaceInFilePromptPos;
        }
        return null;
      }
    };

    replacePromptDialog.show();

    if (model.isMultipleFiles()){
      myReplaceInProjectPromptPos = replacePromptDialog.getLocation();
    }
    else{
      myReplaceInFilePromptPos = replacePromptDialog.getLocation();
    }
    //noinspection MagicConstant
    return replacePromptDialog.getExitCode();
  }

  void changeGlobalSettings(FindModel findModel) {
    String stringToFind = findModel.getStringToFind();
    FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(myProject);

    if (!StringUtil.isEmpty(stringToFind)) {
      findInProjectSettings.addStringToFind(stringToFind);
    }
    if (!findModel.isMultipleFiles()) {
      setFindWasPerformed();
    }
    if (findModel.isReplaceState()) {
      findInProjectSettings.addStringToReplace(findModel.getStringToReplace());
    }
    if (findModel.isMultipleFiles() && !findModel.isProjectScope() && findModel.getDirectoryName() != null) {
      findInProjectSettings.addDirectory(findModel.getDirectoryName());
      myFindInProjectModel.setWithSubdirectories(findModel.isWithSubdirectories());
    }
  }

  @Override
  public void showFindDialog(@NotNull FindModel model, @NotNull Runnable okHandler) {
    if (myHelper == null || Disposer.isDisposed(myHelper)) {
      myHelper = new FindUIHelper(myProject, model, okHandler);
        Disposer.register(myHelper, () -> myHelper = null);
    }
    else {
      myHelper.setModel(model);
      myHelper.setOkHandler(okHandler);
    }
    myHelper.showUI();
  }

  @Override
  @NotNull
  public FindModel getFindInFileModel() {
    return myFindInFileModel;
  }

  @Override
  @NotNull
  public FindModel getFindInProjectModel() {
    myFindInProjectModel.setFromCursor(false);
    myFindInProjectModel.setForward(true);
    myFindInProjectModel.setGlobal(true);
    myFindInProjectModel.setMultiline(Registry.is("ide.find.as.popup.allow.multiline"));
    myFindInProjectModel.setSearchInProjectFiles(false);
    return myFindInProjectModel;
  }

  @Override
  public boolean findWasPerformed() {
    return isFindWasPerformed;
  }

  @Override
  public void setFindWasPerformed() {
    isFindWasPerformed = true;
    isSelectNextOccurrenceWasPerformed = false;
  }

  @Override
  public boolean selectNextOccurrenceWasPerformed() {
    return isSelectNextOccurrenceWasPerformed;
  }

  @Override
  public void setSelectNextOccurrenceWasPerformed() {
    isSelectNextOccurrenceWasPerformed = true;
    isFindWasPerformed = false;
  }

  @Override
  public FindModel getFindNextModel() {
    return myFindNextModel;
  }

  @Override
  public FindModel getFindNextModel(@NotNull final Editor editor) {
    if (myFindNextModel == null) return null;

    EditorSearchSession search = EditorSearchSession.get(editor);
    if (search != null && !isSelectNextOccurrenceWasPerformed) {
      String textInField = search.getTextInField();
      if (!Objects.equals(textInField, myFindInFileModel.getStringToFind()) && !textInField.isEmpty()) {
        FindModel patched = new FindModel();
        patched.copyFrom(myFindNextModel);
        patched.setStringToFind(textInField);
        return patched;
      }
    }

    return myFindNextModel;
  }

  @Override
  public void setFindNextModel(FindModel findNextModel) {
    myFindNextModel = findNextModel;
    myProject.getMessageBus().syncPublisher(FIND_MODEL_TOPIC).findNextModelChanged();
  }

  @Override
  @NotNull
  public FindResult findString(@NotNull CharSequence text, int offset, @NotNull FindModel model){
    return findString(text, offset, model, null);
  }

  @NotNull
  @Override
  public FindResult findString(@NotNull CharSequence text, int offset, @NotNull FindModel model, @Nullable VirtualFile file) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("offset="+offset);
      LOG.debug("textlength="+text.length());
      LOG.debug(model.toString());
    }

    return findStringLoop(text, offset, model, file, getFindContextPredicate(model, file, text));
  }

  private FindResult findStringLoop(CharSequence text, int offset, FindModel model, VirtualFile file, @Nullable Predicate<? super FindResult> filter) {
    final char[] textArray = CharArrayUtil.fromSequenceWithoutCopying(text);
    while(true) {
      FindResult result = doFindString(text, textArray, offset, model, file);
      if (filter == null || filter.test(result)) {
        if (!model.isWholeWordsOnly()) {
          return result;
        }
        if (!result.isStringFound()) {
          return result;
        }
        if (isWholeWord(text, result.getStartOffset(), result.getEndOffset())) {
          return result;
        }
      }

      offset = model.isForward() ? result.getStartOffset() + 1 : result.getEndOffset() - 1;
      if (offset > text.length() || offset < 0) return NOT_FOUND_RESULT;
    }
  }

  private static class FindExceptCommentsOrLiteralsData implements Predicate<FindResult> {
    private final VirtualFile myFile;
    private final FindModel myFindModel;
    private final TreeMap<Integer, Integer> mySkipRangesSet;
    private final CharSequence myText;

    static FindExceptCommentsOrLiteralsData create(@NotNull VirtualFile file,
                                                   @NotNull FindModel model,
                                                   @NotNull CharSequence text,
                                                   @NotNull FindManagerImpl manager) {
      TreeMap<Integer, Integer> skipRangesSet = new TreeMap<>();

      if (model.isExceptComments() || model.isExceptCommentsAndStringLiterals()) {
        addRanges(file, model, text, skipRangesSet, FindModel.SearchContext.IN_COMMENTS, manager);
      }

      if (model.isExceptStringLiterals() || model.isExceptCommentsAndStringLiterals()) {
        addRanges(file, model, text, skipRangesSet, FindModel.SearchContext.IN_STRING_LITERALS, manager);
      }

      return new FindExceptCommentsOrLiteralsData(file, model.clone(), ImmutableCharSequence.asImmutable(text), skipRangesSet);
    }

    FindExceptCommentsOrLiteralsData(@NotNull VirtualFile file,
                                     @NotNull FindModel model,
                                     @NotNull CharSequence text,
                                     @NotNull TreeMap<Integer, Integer> skipRangesSet) {
      myFile = file;
      myFindModel = model.clone();
      myText = ImmutableCharSequence.asImmutable(text);
      mySkipRangesSet = skipRangesSet;
    }

    private static void addRanges(VirtualFile file,
                                  FindModel model,
                                  CharSequence text,
                                  TreeMap<Integer, Integer> result,
                                  FindModel.SearchContext searchContext,
                                  FindManagerImpl manager) {
      FindModel clonedModel = model.clone();
      clonedModel.setSearchContext(searchContext);
      clonedModel.setForward(true);
      int offset = 0;

      while(true) {
        FindResult customResult = manager.findStringLoop(text, offset, clonedModel, file, null);
        if (!customResult.isStringFound()) break;
        result.put(customResult.getStartOffset(), customResult.getEndOffset());
        offset = Math.max(customResult.getEndOffset(), offset + 1);  // avoid loop for zero size reg exps matches
        if (offset >= text.length()) break;
      }
    }

    boolean isAcceptableFor(FindModel model, VirtualFile file, CharSequence text) {
      return Comparing.equal(myFile, file) &&
             myFindModel.equals(model) &&
             myText.length() == text.length()
        ;
    }

    @Override
    public boolean test(@Nullable FindResult input) {
      if (input == null || !input.isStringFound()) return true;
      NavigableMap<Integer, Integer> map = mySkipRangesSet.headMap(input.getStartOffset(), true);
      for(Map.Entry<Integer, Integer> e:map.descendingMap().entrySet()) {
        // [e.key, e.value] intersect with [input.start, input.end]
        if (e.getKey() <= input.getStartOffset() && (input.getStartOffset() <= e.getValue() || e.getValue() >= input.getEndOffset())) return false;
        if (e.getValue() <= input.getStartOffset()) break;
      }
      return true;
    }
  }
  private static final Key<ThreadLocal<FindExceptCommentsOrLiteralsData>> ourExceptCommentsOrLiteralsDataKey = KeyWithDefaultValue.create("except.comments.literals.search.data", () -> new ThreadLocal<>());

  private Predicate<FindResult> getFindContextPredicate(@NotNull FindModel model, @Nullable VirtualFile file, @NotNull CharSequence text) {
    if (file == null) return null;
    FindModel.SearchContext context = model.getSearchContext();
    if( context == FindModel.SearchContext.ANY || context == FindModel.SearchContext.IN_COMMENTS ||
        context == FindModel.SearchContext.IN_STRING_LITERALS) {
      return null;
    }

    ThreadLocal<FindExceptCommentsOrLiteralsData> data;
    synchronized (model) {
      data = model.getUserData(ourExceptCommentsOrLiteralsDataKey);
      assert data != null;
    }

    FindExceptCommentsOrLiteralsData currentThreadData = data.get();
    if (currentThreadData == null || !currentThreadData.isAcceptableFor(model, file, text)) {
      data.set(currentThreadData = FindExceptCommentsOrLiteralsData.create(file, model, text, this));
    }
    return currentThreadData;
  }

  @Override
  public int showMalformedReplacementPrompt(@NotNull FindModel model, String title, MalformedReplacementStringException exception) {
    return showPromptDialogImpl(model, title, exception);
  }

  @Override
  public FindModel getPreviousFindModel() {
    return myPreviousFindModel;
  }

  @Override
  public void setPreviousFindModel(FindModel previousFindModel) {
    myPreviousFindModel = previousFindModel;
  }

  private static boolean isWholeWord(CharSequence text, int startOffset, int endOffset) {
    boolean isWordStart;

    if (startOffset != 0) {
      boolean previousCharacterIsIdentifier = Character.isJavaIdentifierPart(text.charAt(startOffset - 1)) &&
                                              (startOffset <= 1 || text.charAt(startOffset - 2) != '\\');
      boolean previousCharacterIsSameAsNext = text.charAt(startOffset - 1) == text.charAt(startOffset);

      boolean firstCharacterIsIdentifier = Character.isJavaIdentifierPart(text.charAt(startOffset));
      isWordStart = firstCharacterIsIdentifier ? !previousCharacterIsIdentifier : !previousCharacterIsSameAsNext;
    }
    else {
      isWordStart = true;
    }

    boolean isWordEnd;

    if (endOffset != text.length()) {
      boolean nextCharacterIsIdentifier = Character.isJavaIdentifierPart(text.charAt(endOffset));
      boolean nextCharacterIsSameAsPrevious = endOffset > 0 && text.charAt(endOffset) == text.charAt(endOffset - 1);
      boolean lastSearchedCharacterIsIdentifier = endOffset > 0 && Character.isJavaIdentifierPart(text.charAt(endOffset - 1));

      isWordEnd = lastSearchedCharacterIsIdentifier ? !nextCharacterIsIdentifier : !nextCharacterIsSameAsPrevious;
    }
    else {
      isWordEnd = true;
    }

    return isWordStart && isWordEnd;
  }

  @NotNull
  private static FindModel normalizeIfMultilined(@NotNull FindModel findmodel) {
    if (findmodel.isMultiline()) {
      final FindModel model = new FindModel();
      model.copyFrom(findmodel);
      final String s = model.getStringToFind();
      String newStringToFind;

      if (findmodel.isRegularExpressions()) {
        newStringToFind = StringUtil.replace(s, "\\n", "\n"); // temporary convert back escaped symbols
        newStringToFind = newStringToFind.replaceAll( "\n", "\\\\n\\\\s*"); // add \\s* for convenience
      } else {
        newStringToFind = StringUtil.escapeToRegexp(s);
        newStringToFind = newStringToFind.replaceAll("\\\\n\\s*", "\\\\n\\\\s*");
        model.setRegularExpressions(true);
      }
      model.setStringToFind(newStringToFind);

      return model;
    }
    return findmodel;
  }

  @NotNull
  private FindResult doFindString(@NotNull CharSequence text,
                                         char @Nullable [] textArray,
                                         int offset,
                                         @NotNull FindModel findmodel,
                                         @Nullable VirtualFile file) {
    FindModel model = normalizeIfMultilined(findmodel);
    String toFind = model.getStringToFind();
    if (toFind.isEmpty()){
      return NOT_FOUND_RESULT;
    }

    if (model.isInCommentsOnly() || model.isInStringLiteralsOnly()) {
      if (file == null) return NOT_FOUND_RESULT;
      return findInCommentsAndLiterals(text, textArray, offset, model, file);
    }

    if (model.isRegularExpressions()){
      return findStringByRegularExpression(text, offset, model, file);
    }

    final StringSearcher searcher = createStringSearcher(model);

    int index;
    if (model.isForward()){
      final int res = searcher.scan(text, textArray, offset, text.length());
      index = res < 0 ? -1 : res;
    }
    else {
      index = offset == 0 ? -1 : searcher.scan(text, textArray, 0, offset-1);
    }
    if (index < 0){
      return NOT_FOUND_RESULT;
    }
    return new FindResultImpl(index, index + toFind.length());
  }

  @NotNull
  private static StringSearcher createStringSearcher(@NotNull FindModel model) {
    return new StringSearcher(model.getStringToFind(), model.isCaseSensitive(), model.isForward());
  }

  static void clearPreviousFindData(FindModel model) {
    synchronized (model) {
      model.putUserData(ourCommentsLiteralsSearchDataKey, null);
      model.putUserData(ourExceptCommentsOrLiteralsDataKey, null);
    }
  }

  private static class CommentsLiteralsSearchData {
    final VirtualFile lastFile;
    int startOffset;
    final SyntaxHighlighterOverEditorHighlighter highlighter;

    TokenSet tokensOfInterest;
    final StringSearcher searcher;
    final Matcher matcher;
    final Set<Language> relevantLanguages;
    final FindModel model;

    CommentsLiteralsSearchData(VirtualFile lastFile, Set<Language> relevantLanguages,
                               SyntaxHighlighterOverEditorHighlighter highlighter, TokenSet tokensOfInterest,
                               StringSearcher searcher, Matcher matcher, FindModel model) {
      this.lastFile = lastFile;
      this.highlighter = highlighter;
      this.tokensOfInterest = tokensOfInterest;
      this.searcher = searcher;
      this.matcher = matcher;
      this.relevantLanguages = relevantLanguages;
      this.model = model;
    }
  }

  private static final Key<ThreadLocal<CommentsLiteralsSearchData>> ourCommentsLiteralsSearchDataKey = KeyWithDefaultValue.create("comments.literals.search.data", () -> new ThreadLocal<>());

  @NotNull
  private FindResult findInCommentsAndLiterals(@NotNull CharSequence text,
                                                      char[] textArray,
                                                      int offset,
                                                      @NotNull FindModel model,
                                                      @NotNull final VirtualFile file) {
    ThreadLocal<CommentsLiteralsSearchData> data;
    synchronized (model) {
      data = model.getUserData(ourCommentsLiteralsSearchDataKey);
      assert data != null;
    }

    FileType ftype = file.getFileType();
    Language lang = LanguageUtil.getLanguageForPsi(myProject, file);

    CommentsLiteralsSearchData currentThreadData = data.get();
    if (currentThreadData == null || !Comparing.equal(currentThreadData.lastFile, file) || !currentThreadData.model.equals(model)) {
      SyntaxHighlighter highlighter = getHighlighter(file, lang);

      if (highlighter == null) {
        // no syntax highlighter -> no search
        return NOT_FOUND_RESULT;
      }

      TokenSet tokensOfInterest = TokenSet.EMPTY;
      Set<Language> relevantLanguages;
      if (lang != null) {
        final Language finalLang = lang;
        relevantLanguages = ReadAction.compute(() -> {
          THashSet<Language> result = new THashSet<>();

          FileViewProvider viewProvider = PsiManager.getInstance(myProject).findViewProvider(file);
          if (viewProvider != null) {
            result.addAll(viewProvider.getLanguages());
          }

          if (result.isEmpty()) {
            result.add(finalLang);
          }
          return result;
        });

        for (Language relevantLanguage : relevantLanguages) {
          tokensOfInterest = addTokenTypesForLanguage(model, relevantLanguage, tokensOfInterest);
        }
      }
      else {
        relevantLanguages = new HashSet<>();
        if (ftype instanceof AbstractFileType) {
          if (model.isInCommentsOnly()) {
            tokensOfInterest = TokenSet.create(CustomHighlighterTokenType.LINE_COMMENT, CustomHighlighterTokenType.MULTI_LINE_COMMENT);
          }
          if (model.isInStringLiteralsOnly()) {
            tokensOfInterest = TokenSet.orSet(tokensOfInterest, TokenSet
              .create(CustomHighlighterTokenType.STRING, CustomHighlighterTokenType.SINGLE_QUOTED_STRING));
          }
        }
      }

      Matcher matcher = model.isRegularExpressions() ? compileRegExp(model, "") : null;
      StringSearcher searcher = matcher != null ? null : new StringSearcher(model.getStringToFind(), model.isCaseSensitive(), true);
      LayeredLexer.ourDisableLayersFlag.set(Boolean.TRUE);

      try {
        SyntaxHighlighterOverEditorHighlighter highlighterAdapter =
          new SyntaxHighlighterOverEditorHighlighter(highlighter, file, myProject);
        currentThreadData =
          new CommentsLiteralsSearchData(file, relevantLanguages, highlighterAdapter, tokensOfInterest, searcher, matcher, model.clone());
        currentThreadData.highlighter.restart(text);
      }
      finally {
        LayeredLexer.ourDisableLayersFlag.set(null);
      }

      data.set(currentThreadData);
    }

    int initialStartOffset = model.isForward() && currentThreadData.startOffset < offset ? currentThreadData.startOffset : 0;
    currentThreadData.highlighter.resetPosition(initialStartOffset);
    final Lexer lexer = currentThreadData.highlighter.getHighlightingLexer();

    IElementType tokenType;
    TokenSet tokens = currentThreadData.tokensOfInterest;

    int lastGoodOffset = 0;
    boolean scanningForward = model.isForward();
    FindResultImpl prevFindResult = NOT_FOUND_RESULT;

    while ((tokenType = lexer.getTokenType()) != null) {
      if (lexer.getState() == 0) lastGoodOffset = lexer.getTokenStart();

      final TextAttributesKey[] keys = currentThreadData.highlighter.getTokenHighlights(tokenType);

      if (tokens.contains(tokenType) ||
          model.isInStringLiteralsOnly() && ChunkExtractor.isHighlightedAsString(keys) ||
          model.isInCommentsOnly() && ChunkExtractor.isHighlightedAsComment(keys)
        ) {
        int start = lexer.getTokenStart();
        int end = lexer.getTokenEnd();
        if (model.isInStringLiteralsOnly()) { // skip literal quotes itself from matching
          char c = text.charAt(start);
          if (c == '"' || c == '\'') {
            while (start < end && c == text.charAt(start)) {
              ++start;
              if (c == text.charAt(end - 1) && start < end) --end;
            }
          }
        }

        final int tokenContentStart = start;

        while (true) {
          FindResultImpl findResult = null;

          if (currentThreadData.searcher != null) {
            int matchStart = currentThreadData.searcher.scan(text, textArray, start, end);

            if (matchStart != -1 && matchStart >= start) {
              final int matchEnd = matchStart + model.getStringToFind().length();
              if (matchStart >= offset || !scanningForward)
                findResult = new FindResultImpl(matchStart, matchEnd);
              else {
                start = matchEnd;
                continue;
              }
            }
          }
          else if (start <= end) {
            currentThreadData.matcher.reset(StringPattern.newBombedCharSequence(text.subSequence(tokenContentStart, end)));
            currentThreadData.matcher.region(start - tokenContentStart, end - tokenContentStart);
            currentThreadData.matcher.useTransparentBounds(true);
            if (currentThreadData.matcher.find()) {
              final int matchEnd = tokenContentStart + currentThreadData.matcher.end();
              int matchStart = tokenContentStart + currentThreadData.matcher.start();
              if (matchStart >= offset || !scanningForward) {
                findResult = new FindResultImpl(matchStart, matchEnd);
              }
              else {
                int diff = 0;
                if (start == end || start == matchEnd) {
                  diff = 1;
                }
                start = matchEnd + diff;
                continue;
              }
            }
          }

          if (findResult != null) {
            if (scanningForward) {
              currentThreadData.startOffset = lastGoodOffset;
              return findResult;
            }
            else {

              if (findResult.getEndOffset() >= offset) return prevFindResult;
              prevFindResult = findResult;
              start = findResult.getEndOffset();
              continue;
            }
          }
          break;
        }
      }
      else {
        Language tokenLang = tokenType.getLanguage();
        if (tokenLang != lang && tokenLang != Language.ANY && !currentThreadData.relevantLanguages.contains(tokenLang)) {
          tokens = addTokenTypesForLanguage(model, tokenLang, tokens);
          currentThreadData.tokensOfInterest = tokens;
          currentThreadData.relevantLanguages.add(tokenLang);
        }
      }

      lexer.advance();
    }

    return prevFindResult;
  }

  private static TokenSet addTokenTypesForLanguage(FindModel model, Language lang, TokenSet tokensOfInterest) {
    ParserDefinition definition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
    if (definition != null) {
      tokensOfInterest = TokenSet.orSet(tokensOfInterest, model.isInCommentsOnly() ? definition.getCommentTokens(): TokenSet.EMPTY);
      tokensOfInterest = TokenSet.orSet(tokensOfInterest, model.isInStringLiteralsOnly() ? definition.getStringLiteralElements() : TokenSet.EMPTY);
    }
    return tokensOfInterest;
  }

  private static SyntaxHighlighter getHighlighter(VirtualFile file, @Nullable Language lang) {
    SyntaxHighlighter syntaxHighlighter = lang != null ? SyntaxHighlighterFactory.getSyntaxHighlighter(lang, null, file) : null;
    if (lang == null || syntaxHighlighter instanceof PlainSyntaxHighlighter) {
      syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(file.getFileType(), null, file);
    }

    return syntaxHighlighter;
  }

  private FindResult findStringByRegularExpression(CharSequence text,
                                                   int startOffset,
                                                   FindModel model,
                                                   VirtualFile file) {
    Matcher matcher = compileRegExp(model, text);
    if (matcher == null) {
      return NOT_FOUND_RESULT;
    }
    try {
      if (model.isForward()) {
        if (matcher.find(startOffset)) {
          if (matcher.end() <= text.length()) {
            return new FindResultImpl(matcher.start(), matcher.end());
          }
        }
        return NOT_FOUND_RESULT;
      }
      else {
        int start = -1;
        int end = -1;
        while (matcher.find() && matcher.end() < startOffset) {
          start = matcher.start();
          end = matcher.end();
        }
        if (start < 0) {
          return NOT_FOUND_RESULT;
        }
        return new FindResultImpl(start, end);
      }
    } catch (StackOverflowError soe) {
      String stringToFind = model.getStringToFind();

      if (!ApplicationManager.getApplication().isHeadlessEnvironment() &&
          ourReportedPatterns.put(stringToFind.hashCode(), Boolean.TRUE) == null) {
        String content = stringToFind + " produced stack overflow when matching content of the file";
        LOG.info(content);
        GROUP.createNotification(FindBundle.message("notification.title.regular.expression.failed.to.match"),
                                     content + " " + file.getPath(),
                                 NotificationType.ERROR,
                                 null
                                   ).notify(myProject);
      }
      return NOT_FOUND_RESULT;
    }
  }

  private static final IntObjectMap<Boolean> ourReportedPatterns = ContainerUtil.createConcurrentIntObjectMap();

  private static Matcher compileRegExp(FindModel model, CharSequence text) {
    Pattern pattern = model.compileRegExp();
    return pattern == null ?
           null :
           pattern.matcher( StringPattern.newBombedCharSequence(text) );
  }

  @Override
  public String getStringToReplace(@NotNull String foundString, @NotNull FindModel model,
                                   int startOffset, @NotNull CharSequence documentText) throws MalformedReplacementStringException{
    String toReplace = model.getStringToReplace();
    if (model.isRegularExpressions()) {
      return getStringToReplaceByRegexp(model, documentText, startOffset);
    }
    if (model.isPreserveCase()) {
      return replaceWithCaseRespect (toReplace, foundString);
    }
    return toReplace;
  }

  private static String getStringToReplaceByRegexp(@NotNull final FindModel model, @NotNull CharSequence text, int startOffset) throws MalformedReplacementStringException {
    Matcher matcher = compileRegexAndFindFirst(model, text, startOffset);
    return getStringToReplaceByRegexp(model, matcher);
  }

  private static String getStringToReplaceByRegexp(@NotNull final FindModel model, Matcher matcher) throws MalformedReplacementStringException{
    if (matcher == null) return null;
    try {
      String toReplace = model.getStringToReplace();
      return new RegExReplacementBuilder(matcher).createReplacement(toReplace);
    }
    catch (Exception e) {
      throw createMalformedReplacementException(model, e);
    }
  }

  private static Matcher compileRegexAndFindFirst(FindModel model, CharSequence text, int startOffset) {
    model = normalizeIfMultilined(model);
    Matcher matcher = compileRegExp(model, text);
    assert matcher != null;

    if (model.isForward()){
      if (!matcher.find(startOffset)) {
        return null;
      }
      if (matcher.end() > text.length()) {
        return null;
      }
    }
    else {
      int start = -1;
      while(matcher.find() && matcher.end() < startOffset){
        start = matcher.start();
      }
      if (start < 0){
        return null;
      }
    }
    return matcher;
  }

  private static MalformedReplacementStringException createMalformedReplacementException(FindModel model, Exception e) {
    return new MalformedReplacementStringException(FindBundle.message("find.replace.invalid.replacement.string", model.getStringToReplace()), e);
  }

  private static String replaceWithCaseRespect(String toReplace, String foundString) {
    if (foundString.isEmpty() || toReplace.isEmpty()) return toReplace;
    StringBuilder buffer = new StringBuilder();

    if (Character.isUpperCase(foundString.charAt(0))) {
      buffer.append(Character.toUpperCase(toReplace.charAt(0)));
    }
    else {
      buffer.append(Character.toLowerCase(toReplace.charAt(0)));
    }

    if (toReplace.length() == 1) return buffer.toString();

    if (foundString.length() == 1) {
      buffer.append(toReplace.substring(1));
      return buffer.toString();
    }

    boolean isReplacementLowercase = true;
    boolean isReplacementUppercase = true;
    for (int i = 1; i < toReplace.length(); i++) {
      char replacementChar = toReplace.charAt(i);
      if (!Character.isLetter(replacementChar)) continue;
      isReplacementLowercase &= Character.isLowerCase(replacementChar);
      isReplacementUppercase &= Character.isUpperCase(replacementChar);
      if (!isReplacementLowercase && !isReplacementUppercase) break;
    }

    boolean isTailUpper = true;
    boolean isTailLower = true;
    for (int i = 1; i < foundString.length(); i++) {
      char foundChar = foundString.charAt(i);
      if (!Character.isLetter(foundChar)) continue;
      isTailUpper &= Character.isUpperCase(foundChar);
      isTailLower &= Character.isLowerCase(foundChar);
      if (!isTailUpper && !isTailLower) break;
    }

    if (isTailUpper && (isReplacementLowercase || isReplacementUppercase)) {
      buffer.append(StringUtil.toUpperCase(toReplace.substring(1)));
    }
    else if (isTailLower && (isReplacementLowercase || isReplacementUppercase)) {
      buffer.append(StringUtil.toLowerCase(toReplace.substring(1)));
    }
    else {
      buffer.append(toReplace.substring(1));
    }
    return buffer.toString();
  }

  @Override
  public boolean canFindUsages(@NotNull PsiElement element) {
    return element.isValid() && myFindUsagesManager.canFindUsages(element);
  }

  @Override
  public void findUsages(@NotNull PsiElement element) {
    findUsages(element, false);
  }

  @Override
  public void findUsagesInScope(@NotNull PsiElement element, @NotNull SearchScope searchScope) {
    myFindUsagesManager.findUsages(element, null, null, false, searchScope);
  }

  @Override
  public void findUsages(@NotNull PsiElement element, boolean showDialog) {
    myFindUsagesManager.findUsages(element, null, null, showDialog, null);
  }

  @Override
  public void showSettingsAndFindUsages(NavigationItem @NotNull [] targets) {
    FindUsagesManager.showSettingsAndFindUsages(targets);
  }

  @Override
  public void clearFindingNextUsageInFile() {
    myFindUsagesManager.clearFindingNextUsageInFile();
  }

  @Override
  public void findUsagesInEditor(@NotNull PsiElement element, @NotNull FileEditor fileEditor) {
    if (fileEditor instanceof TextEditor) {
      TextEditor textEditor = (TextEditor)fileEditor;
      Editor editor = textEditor.getEditor();
      Document document = editor.getDocument();
      PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);

      myFindUsagesManager.findUsages(element, psiFile, fileEditor, false, null);
    }
  }

  private static boolean tryToFindNextUsageViaEditorSearchComponent(Editor editor, SearchResults.Direction forwardOrBackward) {
    EditorSearchSession search = EditorSearchSession.get(editor);
    if (search != null && search.hasMatches()) {
      if (!search.isSearchInProgress()) {
        if (forwardOrBackward == SearchResults.Direction.UP) {
          search.searchBackward();
        }
        else {
          search.searchForward();
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean findNextUsageInEditor(@NotNull FileEditor fileEditor) {
    if (!(fileEditor instanceof TextEditor)) return false;
    return findNextUsageInFile(((TextEditor) fileEditor).getEditor(), SearchResults.Direction.DOWN);
  }

  @Override
  public boolean findNextUsageInEditor(@NotNull Editor editor) {
    return findNextUsageInFile(editor, SearchResults.Direction.DOWN);
  }

  @Override
  public boolean findPreviousUsageInEditor(@NotNull Editor editor) {
    return findNextUsageInFile(editor, SearchResults.Direction.UP);
  }

  private boolean findNextUsageInFile(@NotNull Editor editor, @NotNull SearchResults.Direction direction) {
    editor.getCaretModel().removeSecondaryCarets();
    if (tryToFindNextUsageViaEditorSearchComponent(editor, direction)) {
      return true;
    }

    RangeHighlighter[] highlighters = ((HighlightManagerImpl)HighlightManager.getInstance(myProject)).getHighlighters(editor);
    if (highlighters.length > 0) {
      return highlightNextHighlighter(highlighters, editor, editor.getCaretModel().getOffset(), direction == SearchResults.Direction.DOWN, false);
    }

    if (direction == SearchResults.Direction.DOWN) {
      return myFindUsagesManager.findNextUsageInFile(editor);
    }
    return myFindUsagesManager.findPreviousUsageInFile(editor);
  }

  @Override
  public boolean findPreviousUsageInEditor(@NotNull FileEditor fileEditor) {
    if (!(fileEditor instanceof TextEditor)) return false;
    return findNextUsageInFile(((TextEditor) fileEditor).getEditor(), SearchResults.Direction.UP);
  }

  private static boolean highlightNextHighlighter(RangeHighlighter[] highlighters, Editor editor, int offset, boolean isForward, boolean secondPass) {
    RangeHighlighter highlighterToSelect = null;
    Object wasNotFound = editor.getUserData(HIGHLIGHTER_WAS_NOT_FOUND_KEY);
    for (RangeHighlighter highlighter : highlighters) {
      int start = highlighter.getStartOffset();
      int end = highlighter.getEndOffset();
      if (highlighter.isValid() && start < end) {
        if (isForward && (start > offset || start == offset && secondPass)) {
          if (highlighterToSelect == null || highlighterToSelect.getStartOffset() > start) highlighterToSelect = highlighter;
        }
        if (!isForward && (end < offset || end == offset && secondPass)) {
          if (highlighterToSelect == null || highlighterToSelect.getEndOffset() < end) highlighterToSelect = highlighter;
        }
      }
    }
    if (highlighterToSelect != null) {
      expandFoldRegionsIfNecessary(editor, highlighterToSelect.getStartOffset(), highlighterToSelect.getEndOffset());
      editor.getSelectionModel().setSelection(highlighterToSelect.getStartOffset(), highlighterToSelect.getEndOffset());
      editor.getCaretModel().moveToOffset(highlighterToSelect.getStartOffset());
      ScrollType scrollType;
      if (secondPass) {
        scrollType = isForward ? ScrollType.CENTER_UP : ScrollType.CENTER_DOWN;
      }
      else {
        scrollType = isForward ? ScrollType.CENTER_DOWN : ScrollType.CENTER_UP;
      }
      editor.getScrollingModel().scrollToCaret(scrollType);
      editor.putUserData(HIGHLIGHTER_WAS_NOT_FOUND_KEY, null);
      return true;
    }

    if (wasNotFound == null) {
      editor.putUserData(HIGHLIGHTER_WAS_NOT_FOUND_KEY, Boolean.TRUE);
      String message = FindBundle.message("find.highlight.no.more.highlights.found");
      if (isForward) {
        AnAction action=ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_NEXT);
        String shortcutsText=KeymapUtil.getFirstKeyboardShortcutText(action);
        if (shortcutsText.isEmpty()) {
          message = FindBundle.message("find.search.again.from.top.action.message", message);
        }
        else {
          message = FindBundle.message("find.search.again.from.top.hotkey.message", message, shortcutsText);
        }
      }
      else {
        AnAction action=ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_PREVIOUS);
        String shortcutsText=KeymapUtil.getFirstKeyboardShortcutText(action);
        if (shortcutsText.isEmpty()) {
          message = FindBundle.message("find.search.again.from.bottom.action.message", message);
        }
        else {
          message = FindBundle.message("find.search.again.from.bottom.hotkey.message", message, shortcutsText);
        }
      }
      JComponent component = HintUtil.createInformationLabel(message);
      final LightweightHint hint = new LightweightHint(component);
      HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, HintManager.UNDER, HintManager.HIDE_BY_ANY_KEY |
                                                                                        HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0, false);
      return true;
    }
    if (!secondPass) {
      offset = isForward ? 0 : editor.getDocument().getTextLength();
      return highlightNextHighlighter(highlighters, editor, offset, isForward, true);
    }

    return false;
  }

  private static void expandFoldRegionsIfNecessary(@NotNull Editor editor, final int startOffset, int endOffset) {
    final FoldingModel foldingModel = editor.getFoldingModel();
    final FoldRegion[] regions;
    if (foldingModel instanceof FoldingModelEx) {
      regions = ((FoldingModelEx)foldingModel).fetchTopLevel();
    }
    else {
      regions = foldingModel.getAllFoldRegions();
    }
    if (regions == null) {
      return;
    }
    int i = Arrays.binarySearch(regions, null, (o1, o2) -> {
      // Find the first region that ends after the given start offset
      if (o1 == null) {
        return startOffset - o2.getEndOffset();
      }
      return o1.getEndOffset() - startOffset;
    });
    if (i < 0) {
      i = -i - 1;
    }
    else {
      i++; // Don't expand fold region that ends at the start offset.
    }
    if (i >= regions.length) {
      return;
    }
    final List<FoldRegion> toExpand = new ArrayList<>();
    for (; i < regions.length; i++) {
      final FoldRegion region = regions[i];
      if (region.getStartOffset() >= endOffset) {
        break;
      }
      if (!region.isExpanded()) {
        toExpand.add(region);
      }
    }
    if (toExpand.isEmpty()) {
      return;
    }
    foldingModel.runBatchFoldingOperation(() -> {
      for (FoldRegion region : toExpand) {
        region.setExpanded(true);
      }
    });
  }

  @NotNull
  public FindUsagesManager getFindUsagesManager() {
    return myFindUsagesManager;
  }
}
