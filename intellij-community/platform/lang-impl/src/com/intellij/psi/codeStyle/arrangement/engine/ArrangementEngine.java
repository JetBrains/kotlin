// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle.arrangement.engine;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.actions.FormatChangedTextUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.arrangement.*;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementSectionRule;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.CustomArrangementOrderToken;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.Stack;
import com.intellij.util.text.CharArrayUtil;
import gnu.trove.TIntArrayList;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Section.END_SECTION;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Section.START_SECTION;

/**
 * Encapsulates generic functionality of arranging file elements by the predefined rules.
 * <p/>
 * I.e. the general idea is to have a language-specific rules hidden by generic arrangement API and common arrangement
 * engine which works on top of that API and performs the arrangement.
 */
@Service
public final class ArrangementEngine {
  private boolean myCodeChanged;

  public static ArrangementEngine getInstance() {
    return ServiceManager.getService(ArrangementEngine.class);
  }

  @Nullable
  public String getUserNotificationInfo() {
    if (myCodeChanged) {
      return "rearranged code";
    }
    return null;
  }

  /**
   * Arranges given PSI root contents that belong to the given ranges.
   * <b>Note:</b> After arrangement editor foldings we'll be preserved.
   *
   * @param editor
   * @param file   target PSI root
   * @param ranges target ranges to use within the given root
   */
  public void arrange(@NotNull final Editor editor, @NotNull PsiFile file, Collection<TextRange> ranges) {
    arrange(file, ranges, new RestoreFoldArrangementCallback(editor));
  }

  /**
   * Arranges given PSI root contents that belong to the given ranges.
   * <b>Note:</b> Editor foldings are not expected to be preserved.
   *
   * @param file   target PSI root
   * @param ranges target ranges to use within the given root
   */
  public void arrange(@NotNull PsiFile file, @NotNull Collection<TextRange> ranges) {
    arrange(file, ranges, null);
  }

  /**
   * Arranges given PSI root contents that belong to the given ranges.
   *
   * @param file    target PSI root
   * @param ranges  target ranges to use within the given root
   */
  public void arrange(@NotNull PsiFile file, @NotNull Collection<TextRange> ranges, @Nullable final ArrangementCallback callback) {
    myCodeChanged = false;

    final Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
    if (document == null || !document.isWritable()) {
      return;
    }

    final Rearranger<?> rearranger = Rearranger.EXTENSION.forLanguage(file.getLanguage());
    if (rearranger == null) {
      return;
    }

    final CodeStyleSettings settings = CodeStyle.getSettings(file);
    if (settings.getExcludedFiles().contains(file)) {
      return;
    }

    ArrangementSettings arrangementSettings = ArrangementUtil.getArrangementSettings(settings, file.getLanguage());

    if (arrangementSettings == null) {
      return;
    }

    final Context<? extends ArrangementEntry> context;
    DumbService.getInstance(file.getProject()).setAlternativeResolveEnabled(true);
    try {
      context = Context.from(rearranger, document, file, ranges, arrangementSettings, settings);
    }
    finally {
      DumbService.getInstance(file.getProject()).setAlternativeResolveEnabled(false);
    }

    ApplicationManager.getApplication().runWriteAction(() -> FormatChangedTextUtil.getInstance().runHeavyModificationTask(file.getProject(), document, () -> {
      doArrange(context);
      if (callback != null) {
        callback.afterArrangement(context.moveInfos);
      }
    }));
  }

  private <E extends ArrangementEntry> void doArrange(Context<E> context) {
    // The general idea is to process entries bottom-up where every processed group belongs to the same parent. We may not bother
    // with entries text ranges then. We use a list and a stack for achieving that than.
    //
    // Example:
    //            Entry1              Entry2
    //            /    \              /    \
    //      Entry11   Entry12    Entry21  Entry22
    //
    //    --------------------------
    //    Stage 1:
    //      list: Entry1 Entry2    <-- entries to process
    //      stack: [0, 0, 2]       <-- holds current iteration info at the following format:
    //                                 (start entry index at the auxiliary list (inclusive); current index; end index (exclusive))
    //    --------------------------
    //    Stage 2:
    //      list: Entry1 Entry2 Entry11 Entry12
    //      stack: [0, 1, 2]
    //             [2, 2, 4]
    //    --------------------------
    //    Stage 3:
    //      list: Entry1 Entry2 Entry11 Entry12
    //      stack: [0, 1, 2]
    //             [2, 3, 4]
    //    --------------------------
    //    Stage 4:
    //      list: Entry1 Entry2 Entry11 Entry12
    //      stack: [0, 1, 2]
    //             [2, 4, 4]
    //    --------------------------
    //      arrange 'Entry11 Entry12'
    //    --------------------------
    //    Stage 5:
    //      list: Entry1 Entry2
    //      stack: [0, 1, 2]
    //    --------------------------
    //    Stage 6:
    //      list: Entry1 Entry2 Entry21 Entry22
    //      stack: [0, 2, 2]
    //             [2, 2, 4]
    //    --------------------------
    //    Stage 7:
    //      list: Entry1 Entry2 Entry21 Entry22
    //      stack: [0, 2, 2]
    //             [2, 3, 4]
    //    --------------------------
    //    Stage 8:
    //      list: Entry1 Entry2 Entry21 Entry22
    //      stack: [0, 2, 2]
    //             [2, 4, 4]
    //    --------------------------
    //      arrange 'Entry21 Entry22'
    //    --------------------------
    //    Stage 9:
    //      list: Entry1 Entry2
    //      stack: [0, 2, 2]
    //    --------------------------
    //      arrange 'Entry1 Entry2'
    Stack<StackEntry> stack = new Stack<>();
    List<ArrangementEntryWrapper<E>> entries = new ArrayList<>(context.wrappers);
    stack.push(new StackEntry(0, context.wrappers.size()));
    while (!stack.isEmpty()) {
      StackEntry stackEntry = stack.peek();
      if (stackEntry.current >= stackEntry.end) {
        List<ArrangementEntryWrapper<E>> subEntries = entries.subList(stackEntry.start, stackEntry.end);
        // arrange entries even if subEntries.size() == 1, because we don't want to miss new section comments here
        doArrange(subEntries, context);
        subEntries.clear();
        stack.pop();
      }
      else {
        ArrangementEntryWrapper<E> wrapper = entries.get(stackEntry.current++);
        List<ArrangementEntryWrapper<E>> children = wrapper.getChildren();
        if (!children.isEmpty()) {
          entries.addAll(children);
          stack.push(new StackEntry(stackEntry.end, children.size()));
        }
      }
    }
  }

  /**
   * Arranges (re-orders) given entries according to the given rules.
   *
   * @param entries            entries to arrange
   * @param sectionRules       rules to use for arrangement
   * @param rulesByPriority    rules sorted by priority ('public static' rule will have higher priority than 'public')
   * @param entryToSection     mapping from arrangement entry to the parent section
   * @return                   arranged list of the given rules
   */
  @NotNull
  public static <E extends ArrangementEntry> List<E> arrange(@NotNull Collection<? extends E> entries,
                                                             @NotNull List<? extends ArrangementSectionRule> sectionRules,
                                                             @NotNull List<? extends ArrangementMatchRule> rulesByPriority,
                                                             @Nullable Map<E, ArrangementSectionRule> entryToSection)
  {
    List<E> arranged = new ArrayList<>();
    Set<E> unprocessed = new LinkedHashSet<>();
    List<Pair<Set<ArrangementEntry>, E>> dependent = new ArrayList<>();
    for (E entry : entries) {
      List<? extends ArrangementEntry> dependencies = entry.getDependencies();
      if (dependencies == null) {
        unprocessed.add(entry);
      }
      else {
        if (dependencies.size() == 1 && dependencies.get(0) == entry.getParent()) {
          // Handle a situation when the entry is configured to be at the first parent's children.
          arranged.add(entry);
        }
        else {
          Set<ArrangementEntry> first = new HashSet<>(dependencies);
          dependent.add(Pair.create(first, entry));
        }
      }
    }

    Set<E> matched = new HashSet<>();

    MultiMap<ArrangementMatchRule, E> elementsByRule = new MultiMap<>();
    for (ArrangementMatchRule rule : rulesByPriority) {
      matched.clear();
      for (E entry : unprocessed) {
        if (entry.canBeMatched() && rule.getMatcher().isMatched(entry)) {
          elementsByRule.putValue(rule, entry);
          matched.add(entry);
        }
      }
      unprocessed.removeAll(matched);
    }

    for (ArrangementSectionRule sectionRule : sectionRules) {
      for (ArrangementMatchRule rule : sectionRule.getMatchRules()) {
        final Collection<E> arrangedEntries = arrangeByRule(arranged, elementsByRule, rule);

        if (entryToSection != null && arrangedEntries != null) {
          for (E entry : arrangedEntries) {
            entryToSection.put(entry, sectionRule);
          }
        }
      }
    }
    arranged.addAll(unprocessed);

    for (int i = 0; i < arranged.size() && !dependent.isEmpty(); i++) {
      E e = arranged.get(i);
      List<E> shouldBeAddedAfterCurrentElement = new ArrayList<>();

      for (Iterator<Pair<Set<ArrangementEntry>, E>> iterator = dependent.iterator(); iterator.hasNext(); ) {
        Pair<Set<ArrangementEntry>, E> pair = iterator.next();
        pair.first.remove(e);
        if (pair.first.isEmpty()) {
          iterator.remove();
          shouldBeAddedAfterCurrentElement.add(pair.second);
        }
      }

      // add dependent entries to the same section as main entry
      if (entryToSection != null && entryToSection.containsKey(e)) {
        final ArrangementSectionRule rule = entryToSection.get(e);
        for (E e1 : shouldBeAddedAfterCurrentElement) {
          entryToSection.put(e1, rule);
        }
      }
      arranged.addAll(i + 1, shouldBeAddedAfterCurrentElement);
    }

    return arranged;
  }

  @Nullable
  private static <E extends ArrangementEntry> Collection<E> arrangeByRule(@NotNull List<? super E> arranged,
                                                                          @NotNull MultiMap<ArrangementMatchRule, E> elementsByRule,
                                                                          @NotNull ArrangementMatchRule rule) {
    if (elementsByRule.containsKey(rule)) {
      List<E> arrangedEntries = (List<E>)elementsByRule.remove(rule);
      assert arrangedEntries != null;

      ArrangementSettingsToken order = rule.getOrderType();
      if (order instanceof CustomArrangementOrderToken) {
        arrangedEntries.sort(((CustomArrangementOrderToken)order).getEntryComparator());
      }
      else if (rule.getOrderType().equals(StdArrangementTokens.Order.BY_NAME)) {
        sortByName(arrangedEntries);
      }

      arranged.addAll(arrangedEntries);
      return arrangedEntries;
    }
    return null;
  }

  private static <E extends ArrangementEntry> void sortByName(@NotNull List<E> entries) {
    if (entries.size() < 2) {
      return;
    }
    final TObjectIntHashMap<E> weights = new TObjectIntHashMap<>();
    int i = 0;
    for (E e : entries) {
      weights.put(e, ++i);
    }
    ContainerUtil.sort(entries, (e1, e2) -> {
      String name1 = e1 instanceof NameAwareArrangementEntry ? ((NameAwareArrangementEntry)e1).getName() : null;
      String name2 = e2 instanceof NameAwareArrangementEntry ? ((NameAwareArrangementEntry)e2).getName() : null;
      if (name1 != null && name2 != null) {
        return name1.compareTo(name2);
      }
      else if (name1 == null && name2 == null) {
        return weights.get(e1) - weights.get(e2);
      }
      else if (name2 == null) {
        return -1;
      }
      else {
        return 1;
      }
    });
  }

  @SuppressWarnings("unchecked")
  private <E extends ArrangementEntry> void doArrange(@NotNull List<? extends ArrangementEntryWrapper<E>> wrappers,
                                                      @NotNull Context<E> context) {
    if (wrappers.isEmpty()) {
      return;
    }

    Map<E, ArrangementSectionRule> entryToSection = new HashMap<>();
    Map<E, ArrangementEntryWrapper<E>> map = new HashMap<>();
    List<E> arranged = new ArrayList<>();
    List<E> toArrange = new ArrayList<>();
    for (ArrangementEntryWrapper<E> wrapper : wrappers) {
      E entry = wrapper.getEntry();
      map.put(wrapper.getEntry(), wrapper);
      if (!entry.canBeMatched()) {
        // Split entries to arrange by 'can not be matched' rules.
        // See IDEA-104046 for a problem use-case example.
        arranged.add(entry);
        toArrange.clear();
      }
      else {
        toArrange.add(entry);
      }
    }
    if (!toArrange.isEmpty()) {
      E contextEntry = toArrange.get(0);
      Language language = contextEntry instanceof LanguageAwareArrangementEntry ?
                          ((LanguageAwareArrangementEntry)contextEntry).getLanguage() : null;
      ArrangementSettings settings = context.getArrangementSettings(language);
      List<? extends ArrangementMatchRule> rulesByPriority = settings.getRulesSortedByPriority();
      List<ArrangementSectionRule> sectionRules = ArrangementUtil.getExtendedSectionRules(settings);
      arranged.addAll(arrange(toArrange, sectionRules, rulesByPriority, entryToSection));
    }

    final NewSectionInfo<E> newSectionsInfo = NewSectionInfo.create(arranged, entryToSection);
    context.changer.prepare(wrappers, context);
    // We apply changes from the last position to the first position in order not to bother with offsets shifts.
    for (int i = arranged.size() - 1; i >= 0; i--) {
      ArrangementEntryWrapper<E> arrangedWrapper = map.get(arranged.get(i));
      ArrangementEntryWrapper<E> initialWrapper = wrappers.get(i);

      ArrangementEntryWrapper<E> previous = i > 0 ? map.get(arranged.get(i - 1)) : null;
      ArrangementEntryWrapper<E> previousInitial = i > 0 ? wrappers.get(i - 1) : null;

      final ArrangementEntryWrapper<E> parentWrapper = initialWrapper.getParent();
      if (arrangedWrapper.equals(initialWrapper)) {
        if (previous != null && previous.equals(previousInitial) || previous == null && previousInitial == null) {
          final int beforeOffset = arrangedWrapper.getStartOffset();
          final int afterOffset = arrangedWrapper.getEndOffset();

          boolean isInserted = context.changer.insertSection(context, arranged.get(i), newSectionsInfo, parentWrapper, beforeOffset, afterOffset);
          myCodeChanged = isInserted || myCodeChanged;
          continue;
        }
      }

      ArrangementEntryWrapper<E> next = i < arranged.size() - 1 ? map.get(arranged.get(i + 1)) : null;
      context.changer.replace(arrangedWrapper, initialWrapper, previous, next, context);
      context.changer.insertSection(context, arranged.get(i), newSectionsInfo, arrangedWrapper, initialWrapper, parentWrapper);
      myCodeChanged = true;
    }
  }

  private static class NewSectionInfo<E extends ArrangementEntry> {
    private final Map<E, String> mySectionStarts = new HashMap<>();
    private final Map<E, String> mySectionEnds = new HashMap<>();

    private static <E extends ArrangementEntry> NewSectionInfo create(@NotNull List<? extends E> arranged,
                                                                      @NotNull Map<E, ArrangementSectionRule> entryToSection) {
      final NewSectionInfo<E> info = new NewSectionInfo<>();

      boolean sectionIsOpen = false;
      ArrangementSectionRule prevSection = null;
      E prev = null;
      for (E e : arranged) {
        final ArrangementSectionRule section = entryToSection.get(e);
        if (section != prevSection) {
          closeSection(prevSection, prev, info, sectionIsOpen);
          sectionIsOpen = false;

          if (section != null) {
            final String startComment = section.getStartComment();
            if (StringUtil.isNotEmpty(startComment) && !isSectionEntry(e, startComment)) {
              sectionIsOpen = true;
              info.addSectionStart(e, startComment);
            }
          }
          prevSection = section;
        }
        prev = e;
      }

      closeSection(prevSection, prev, info, sectionIsOpen);
      return info;
    }

    public static boolean isSectionEntry(@NotNull ArrangementEntry entry, @NotNull String sectionText) {
      if (entry instanceof TypeAwareArrangementEntry && entry instanceof TextAwareArrangementEntry) {
        final Set<ArrangementSettingsToken> types = ((TypeAwareArrangementEntry)entry).getTypes();
        if (types.size() == 1) {
          final ArrangementSettingsToken type = types.iterator().next();
          if (type.equals(START_SECTION) || type.equals(END_SECTION)) {
            return StringUtil.equals(((TextAwareArrangementEntry)entry).getText(), sectionText);
          }
        }
      }
      return false;
    }

    private static <E extends ArrangementEntry> void closeSection(@Nullable ArrangementSectionRule section,
                                                                  @Nullable E entry,
                                                                  @NotNull NewSectionInfo<E> info,
                                                                  boolean sectionIsOpen) {
      if (sectionIsOpen) {
        assert section != null && entry != null;
        if (StringUtil.isNotEmpty(section.getEndComment())) {
          info.addSectionEnd(entry, section.getEndComment());
        }
      }
    }

    private void addSectionStart(E entry, String comment) {
      mySectionStarts.put(entry, comment);
    }

    private void addSectionEnd(E entry, String comment) {
      mySectionEnds.put(entry, comment);
    }

    @Nullable
    public String getStartComment(E entry) {
      return mySectionStarts.get(entry);
    }

    @Nullable
    public String getEndComment(E entry) {
      return mySectionEnds.get(entry);
    }
  }

  private static class Context<E extends ArrangementEntry> {

    @NotNull public final List<ArrangementMoveInfo> moveInfos = new ArrayList<>();

    @NotNull private final Rearranger<E>                         rearranger;
    @NotNull public final Collection<ArrangementEntryWrapper<E>> wrappers;
    @NotNull public final Document                               document;
    @NotNull private final ArrangementSettings                   arrangementSettings;
    @NotNull public final CodeStyleSettings                      settings;
    @NotNull public final Changer                                changer;

    private Context(@NotNull Rearranger<E> rearranger,
                    @NotNull Collection<ArrangementEntryWrapper<E>> wrappers,
                    @NotNull Document document,
                    @NotNull ArrangementSettings arrangementSettings,
                    @NotNull CodeStyleSettings settings, @NotNull Changer changer)
    {
      this.rearranger = rearranger;
      this.wrappers = wrappers;
      this.document = document;
      this.arrangementSettings = arrangementSettings;
      this.settings = settings;
      this.changer = changer;
    }

    public void addMoveInfo(int oldStart, int oldEnd, int newStart) {
      moveInfos.add(new ArrangementMoveInfo(oldStart, oldEnd, newStart));
    }

    @NotNull
    public ArrangementSettings getArrangementSettings(@Nullable Language languageOverride) {
      if (languageOverride != null) {
        ArrangementSettings languageSettings = ArrangementUtil.getArrangementSettings(this.settings, languageOverride);
        if (languageSettings != null) {
          return languageSettings;
        }
      }
      return arrangementSettings;
    }

    @NotNull
    public Rearranger<E> getRearranger(@Nullable Language language) {
      if (language != null) {
        Rearranger<?> forLanguage = Rearranger.EXTENSION.forLanguage(language);
        if (forLanguage != null) {
          //noinspection unchecked
          return (Rearranger<E>)forLanguage;
        }
      }
      return rearranger;
    }

    public static <T extends ArrangementEntry> Context<T> from(@NotNull Rearranger<T> rearranger,
                                                               @NotNull Document document,
                                                               @NotNull PsiElement root,
                                                               @NotNull Collection<TextRange> ranges,
                                                               @NotNull ArrangementSettings arrangementSettings,
                                                               @NotNull CodeStyleSettings codeStyleSettings)
    {
      Collection<T> entries = rearranger.parse(root, document, ranges, arrangementSettings);
      Collection<ArrangementEntryWrapper<T>> wrappers = new ArrayList<>();
      ArrangementEntryWrapper<T> previous = null;
      for (T entry : entries) {
        ArrangementEntryWrapper<T> wrapper = new ArrangementEntryWrapper<>(entry);
        if (previous != null) {
          previous.setNext(wrapper);
          wrapper.setPrevious(previous);
        }
        wrappers.add(wrapper);
        previous = wrapper;
      }
      Changer changer;
      if (document instanceof DocumentEx) {
        changer = new RangeMarkerAwareChanger<T>((DocumentEx)document);
      }
      else {
        changer = new DefaultChanger();
      }
      return new Context<>(rearranger, wrappers, document, arrangementSettings, codeStyleSettings, changer);
    }
  }

  private static class StackEntry {

    public int start;
    public int current;
    public int end;

    StackEntry(int start, int count) {
      this.start = start;
      current = start;
      end = start + count;
    }
  }

  private abstract static class Changer<E extends ArrangementEntry> {
    public abstract void prepare(@NotNull List<? extends ArrangementEntryWrapper<E>> toArrange, @NotNull Context<E> context);

    /**
     * Replaces given 'old entry' by the given 'new entry'.
     *
     * @param newWrapper  wrapper for an entry which text should replace given 'old entry' range
     * @param oldWrapper  wrapper for an entry which range should be replaced by the given 'new entry'
     * @param previous    wrapper which will be previous for the entry referenced via the given 'new wrapper'
     * @param next        wrapper which will be next for the entry referenced via the given 'new wrapper'
     * @param context     current context
     */
    public abstract void replace(@NotNull ArrangementEntryWrapper<E> newWrapper,
                                 @NotNull ArrangementEntryWrapper<E> oldWrapper,
                                 @Nullable ArrangementEntryWrapper<E> previous,
                                 @Nullable ArrangementEntryWrapper<E> next,
                                 @NotNull Context<E> context);

    public abstract void insert(@NotNull Context<E> context, int startOffset, @NotNull String text);

    public abstract void insertSection(@NotNull Context<E> context,
                                       @NotNull E entry,
                                       @NotNull NewSectionInfo<E> newSectionsInfo,
                                       @NotNull ArrangementEntryWrapper<E> arranged,
                                       @NotNull ArrangementEntryWrapper<E> initial,
                                       @Nullable ArrangementEntryWrapper<E> parent);

    protected abstract boolean insertSection(@NotNull Context<E> context,
                                          @NotNull E entry,
                                          @NotNull NewSectionInfo<E> newSectionsInfo,
                                          @Nullable ArrangementEntryWrapper<E> parent, int beforeOffset, int afterOffset);

    protected int getBlankLines(@NotNull Context<E> context,
                                @Nullable ArrangementEntryWrapper<E> parentWrapper,
                                @NotNull ArrangementEntryWrapper<E> targetWrapper,
                                @Nullable ArrangementEntryWrapper<E> previousWrapper,
                                @Nullable ArrangementEntryWrapper<E> nextWrapper) {
      final E target = targetWrapper.getEntry();
      final E previous = previousWrapper == null ? null : previousWrapper.getEntry();
      if (isTypeOf(target, END_SECTION) || isTypeOf(previous, START_SECTION)) {
        return 0;
      }
      final E next = nextWrapper == null ? null : nextWrapper.getEntry();
      final E parentEntry = parentWrapper == null ? null : parentWrapper.getEntry();
      final Language language = parentEntry instanceof LanguageAwareArrangementEntry ?
                                ((LanguageAwareArrangementEntry)parentEntry).getLanguage() : null;
      final Rearranger<E> rearranger = context.getRearranger(language);
      if (next != null && isTypeOf(target, START_SECTION)) {
        return rearranger.getBlankLines(context.settings, parentEntry, previous, next);
      }
      return rearranger.getBlankLines(context.settings, parentEntry, previous, target);
    }

    private boolean isTypeOf(@Nullable E element, @NotNull ArrangementSettingsToken token) {
      if (element instanceof TypeAwareArrangementEntry) {
        Set<ArrangementSettingsToken> types = ((TypeAwareArrangementEntry)element).getTypes();
        return types.size() == 1 && token.equals(types.iterator().next());
      }
      return false;
    }
  }

  private static class DefaultChanger<E extends ArrangementEntry> extends Changer<E> {
    @NotNull private String myParentText;
    private          int    myParentShift;

    @Override
    public void prepare(@NotNull List<? extends ArrangementEntryWrapper<E>> toArrange, @NotNull Context<E> context) {
      ArrangementEntryWrapper<E> parent = toArrange.get(0).getParent();
      if (parent == null) {
        myParentText = context.document.getText();
        myParentShift = 0;
      }
      else {
        myParentText = context.document.getCharsSequence().subSequence(parent.getStartOffset(), parent.getEndOffset()).toString();
        myParentShift = parent.getStartOffset();
      }
    }

    @SuppressWarnings("AssignmentToForLoopParameter")
    @Override
    public void replace(@NotNull ArrangementEntryWrapper<E> newWrapper,
                        @NotNull ArrangementEntryWrapper<E> oldWrapper,
                        @Nullable ArrangementEntryWrapper<E> previous,
                        @Nullable ArrangementEntryWrapper<E> next,
                        @NotNull Context<E> context)
    {
      // Calculate blank lines before the arrangement.
      int blankLinesBefore = 0;
      TIntArrayList lineFeedOffsets = new TIntArrayList();
      int oldStartLine = context.document.getLineNumber(oldWrapper.getStartOffset());
      if (oldStartLine > 0) {
        int lastLineFeed = context.document.getLineStartOffset(oldStartLine) - 1;
        lineFeedOffsets.add(lastLineFeed);
        for (int i = lastLineFeed - 1 - myParentShift; i >= 0; i--) {
          i = CharArrayUtil.shiftBackward(myParentText, i, " \t");
          if (myParentText.charAt(i) == '\n') {
            blankLinesBefore++;
            lineFeedOffsets.add(i + myParentShift);
          }
          else {
            break;
          }
        }
      }

      ArrangementEntryWrapper<E> parentWrapper = oldWrapper.getParent();
      int desiredBlankLinesNumber = getBlankLines(context, parentWrapper, newWrapper, previous, next);
      if (desiredBlankLinesNumber == blankLinesBefore && newWrapper.equals(oldWrapper)) {
        return;
      }

      String newEntryText = myParentText.substring(newWrapper.getStartOffset() - myParentShift, newWrapper.getEndOffset() - myParentShift);
      int lineFeedsDiff = desiredBlankLinesNumber - blankLinesBefore;
      if (lineFeedsDiff == 0 || desiredBlankLinesNumber < 0) {
        context.addMoveInfo(newWrapper.getStartOffset() - myParentShift,
                            newWrapper.getEndOffset() - myParentShift,
                            oldWrapper.getStartOffset());
        context.document.replaceString(oldWrapper.getStartOffset(), oldWrapper.getEndOffset(), newEntryText);
        return;
      }

      if (lineFeedsDiff > 0) {
        // Insert necessary number of blank lines.
        StringBuilder buffer = new StringBuilder(StringUtil.repeat("\n", lineFeedsDiff));
        buffer.append(newEntryText);
        context.document.replaceString(oldWrapper.getStartOffset(), oldWrapper.getEndOffset(), buffer);
      }
      else {
        // Cut exceeding blank lines.
        int replacementStartOffset = lineFeedOffsets.get(-lineFeedsDiff) + 1;
        context.document.replaceString(replacementStartOffset, oldWrapper.getEndOffset(), newEntryText);
      }

      // Update wrapper ranges.
      ArrangementEntryWrapper<E> parent = oldWrapper.getParent();
      if (parent == null) {
        return;
      }

      Deque<ArrangementEntryWrapper<E>> parents = new ArrayDeque<>();
      do {
        parents.add(parent);
        parent.setEndOffset(parent.getEndOffset() + lineFeedsDiff);
        parent = parent.getParent();
      }
      while (parent != null);


      while (!parents.isEmpty()) {

        for (ArrangementEntryWrapper<E> wrapper = parents.removeLast().getNext(); wrapper != null; wrapper = wrapper.getNext()) {
          wrapper.applyShift(lineFeedsDiff);
        }
      }
    }

    @Override
    public void insert(@NotNull Context<E> context, int startOffset, @NotNull String text) {
      context.document.insertString(startOffset, text);
    }

    @Override
    public void insertSection(@NotNull Context<E> context,
                              @NotNull E entry,
                              @NotNull NewSectionInfo<E> newSectionsInfo,
                              @NotNull ArrangementEntryWrapper<E> arrangedWrapper,
                              @NotNull ArrangementEntryWrapper<E> initialWrapper,
                              @Nullable ArrangementEntryWrapper<E> parent) {
      final int beforeOffset = arrangedWrapper.equals(initialWrapper) ? arrangedWrapper.getStartOffset() : initialWrapper.getStartOffset();
      final int length = arrangedWrapper.getEndOffset() - arrangedWrapper.getStartOffset();
      int afterOffset = arrangedWrapper.equals(initialWrapper) ? arrangedWrapper.getEndOffset() : beforeOffset + length;

      insertSection(context, entry, newSectionsInfo, parent, beforeOffset, afterOffset);
    }

    @Override
    protected boolean insertSection(@NotNull Context<E> context,
                                 @NotNull E entry,
                                 @NotNull NewSectionInfo<E> newSectionsInfo,
                                 ArrangementEntryWrapper<E> parent, int beforeOffset, int afterOffset) {
      boolean isInserted = false;
      final String afterComment = newSectionsInfo.getEndComment(entry);
      if (afterComment != null) {
        insert(context, afterOffset, "\n" + afterComment);
        isInserted = true;
      }
      final String beforeComment = newSectionsInfo.getStartComment(entry);
      if (beforeComment != null) {
        insert(context, beforeOffset, beforeComment + "\n");
        isInserted = true;
      }
      return isInserted;
    }
  }

  private static class RangeMarkerAwareChanger<E extends ArrangementEntry> extends Changer<E> {

    @NotNull private final List<ArrangementEntryWrapper<E>> myWrappers = new ArrayList<>();
    @NotNull private final DocumentEx myDocument;

    RangeMarkerAwareChanger(@NotNull DocumentEx document) {
      myDocument = document;
    }

    @Override
    public void prepare(@NotNull List<? extends ArrangementEntryWrapper<E>> toArrange, @NotNull Context<E> context) {
      myWrappers.clear();
      myWrappers.addAll(toArrange);
      for (ArrangementEntryWrapper<E> wrapper : toArrange) {
        wrapper.updateBlankLines(myDocument);
      }
    }

    @Override
    public void replace(@NotNull ArrangementEntryWrapper<E> newWrapper,
                        @NotNull ArrangementEntryWrapper<E> oldWrapper,
                        @Nullable ArrangementEntryWrapper<E> previous,
                        @Nullable ArrangementEntryWrapper<E> next,
                        @NotNull Context<E> context)
    {
      // Calculate blank lines before the arrangement.
      int blankLinesBefore = oldWrapper.getBlankLinesBefore();

      ArrangementEntryWrapper<E> parentWrapper = oldWrapper.getParent();
      int desiredBlankLinesNumber = getBlankLines(context, parentWrapper, newWrapper, previous, next);
      if ((desiredBlankLinesNumber < 0 || desiredBlankLinesNumber == blankLinesBefore) && newWrapper.equals(oldWrapper)) {
        return;
      }

      int lineFeedsDiff = desiredBlankLinesNumber - blankLinesBefore;
      int insertionOffset = oldWrapper.getStartOffset();
      if (oldWrapper.getStartOffset() > newWrapper.getStartOffset()) {
        insertionOffset -= newWrapper.getEndOffset() - newWrapper.getStartOffset();
      }
      if (newWrapper.getStartOffset() != oldWrapper.getStartOffset() || !newWrapper.equals(oldWrapper)) {
        context.addMoveInfo(newWrapper.getStartOffset(), newWrapper.getEndOffset(), oldWrapper.getStartOffset());
        myDocument.moveText(newWrapper.getStartOffset(), newWrapper.getEndOffset(), oldWrapper.getStartOffset());
        for (int i = myWrappers.size() - 1; i >= 0; i--) {
          ArrangementEntryWrapper<E> w = myWrappers.get(i);
          if (w == newWrapper) {
            continue;
          }
          if (w.getStartOffset() >= oldWrapper.getStartOffset() && w.getStartOffset() < newWrapper.getStartOffset()) {
            w.applyShift(newWrapper.getEndOffset() - newWrapper.getStartOffset());
          }
          else if (oldWrapper != w && w.getStartOffset() <= oldWrapper.getStartOffset() &&
                   w.getStartOffset() > newWrapper.getStartOffset()) {
            w.applyShift(newWrapper.getStartOffset() - newWrapper.getEndOffset());
          }
        }
      }

      if (desiredBlankLinesNumber >= 0 && lineFeedsDiff > 0) {
        myDocument.insertString(insertionOffset, StringUtil.repeat("\n", lineFeedsDiff));
        shiftOffsets(lineFeedsDiff, insertionOffset);
      }

      if (desiredBlankLinesNumber >= 0 && lineFeedsDiff < 0) {
        // Cut exceeding blank lines.
        int replacementStartOffset = getBlankLineOffset(-lineFeedsDiff, insertionOffset);
        myDocument.deleteString(replacementStartOffset, insertionOffset);
        shiftOffsets(replacementStartOffset - insertionOffset, insertionOffset);
      }

      if (desiredBlankLinesNumber < 0) {
        return;
      }

      updateAllWrapperRanges(parentWrapper, lineFeedsDiff);
    }

    protected void updateAllWrapperRanges(@Nullable ArrangementEntryWrapper<E> parentWrapper, int lineFeedsDiff) {
      // Update wrapper ranges.
      if (lineFeedsDiff == 0 || parentWrapper == null) {
        return;
      }

      Deque<ArrangementEntryWrapper<E>> parents = new ArrayDeque<>();
      do {
        parents.add(parentWrapper);
        parentWrapper.setEndOffset(parentWrapper.getEndOffset() + lineFeedsDiff);
        parentWrapper = parentWrapper.getParent();
      }
      while (parentWrapper != null);


      while (!parents.isEmpty()) {
        for (ArrangementEntryWrapper<E> wrapper = parents.removeLast().getNext(); wrapper != null; wrapper = wrapper.getNext()) {
          wrapper.applyShift(lineFeedsDiff);
        }
      }
    }

    @Override
    public void insert(@NotNull Context<E> context, int startOffset, @NotNull String text) {
      myDocument.insertString(startOffset, text);
      int shift = text.length();
      for (int i = myWrappers.size() - 1; i >= 0; i--) {
        ArrangementEntryWrapper<E> wrapper = myWrappers.get(i);
        if (wrapper.getStartOffset() >= startOffset) {
          wrapper.applyShift(shift);
        }
      }
    }

    @Override
    public void insertSection(@NotNull Context<E> context,
                              @NotNull E entry,
                              @NotNull NewSectionInfo<E> newSectionsInfo,
                              @NotNull ArrangementEntryWrapper<E> arrangedWrapper,
                              @NotNull ArrangementEntryWrapper<E> initialWrapper,
                              @Nullable ArrangementEntryWrapper<E> parent) {
      final int afterOffset = arrangedWrapper.equals(initialWrapper) ? arrangedWrapper.getEndOffset() : initialWrapper.getStartOffset();
      final int length = arrangedWrapper.getEndOffset() - arrangedWrapper.getStartOffset();
      final int beforeOffset = arrangedWrapper.equals(initialWrapper) ? arrangedWrapper.getStartOffset() : afterOffset - length;
      insertSection(context, entry, newSectionsInfo, parent, beforeOffset, afterOffset);
    }

    @Override
    protected boolean insertSection(@NotNull Context<E> context,
                                 @NotNull E entry,
                                 @NotNull NewSectionInfo<E> newSectionsInfo,
                                 @Nullable ArrangementEntryWrapper<E> parent,
                                 int beforeOffset, int afterOffset) {
      boolean isInserted = false;
      int diff = 0;
      final String afterComment = newSectionsInfo.getEndComment(entry);
      if (afterComment != null) {
        insert(context, afterOffset, "\n" + afterComment);
        diff += afterComment.length() + 1;
        isInserted = true;
      }
      final String beforeComment = newSectionsInfo.getStartComment(entry);
      if (beforeComment != null) {
        insert(context, beforeOffset, beforeComment + "\n");
        diff += beforeComment.length() + 1;
        isInserted = true;
      }

      updateAllWrapperRanges(parent, diff);

      return isInserted;
    }

    /**
     * @return position {@code x} for which {@code myDocument.getText().substring(x, startOffset)} contains
     * {@code blankLinesNumber} line feeds and {@code myDocument.getText.charAt(x-1) == '\n'}
     */
    private int getBlankLineOffset(int blankLinesNumber, int startOffset) {
      int startLine = myDocument.getLineNumber(startOffset);
      if (startLine <= 0) {
        return 0;
      }
      CharSequence text = myDocument.getCharsSequence();
      for (int i = myDocument.getLineStartOffset(startLine - 1) - 1; i >= 0; i = CharArrayUtil.lastIndexOf(text, "\n", i - 1)) {
        if (--blankLinesNumber <= 0) {
          return i + 1;
        }
      }
      return 0;
    }

    private void shiftOffsets(int shift, int changeOffset) {
      for (int i = myWrappers.size() - 1; i >= 0; i--) {
        ArrangementEntryWrapper<E> wrapper = myWrappers.get(i);
        if (wrapper.getStartOffset() < changeOffset) {
          break;
        }
        wrapper.applyShift(shift);
      }
    }
  }
}
