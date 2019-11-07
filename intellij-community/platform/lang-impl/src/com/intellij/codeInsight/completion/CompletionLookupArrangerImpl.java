// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.completion.impl.CompletionSorterImpl;
import com.intellij.codeInsight.lookup.*;
import com.intellij.codeInsight.lookup.impl.EmptyLookupItem;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.codeInsight.template.impl.LiveTemplateLookupElement;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.hash.EqualityPolicy;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CompletionLookupArrangerImpl extends LookupArranger implements CompletionLookupArranger {
  private static final Logger LOG = Logger.getInstance(CompletionLookupArranger.class);
  private static final Key<PresentationInvariant> PRESENTATION_INVARIANT = Key.create("PRESENTATION_INVARIANT");
  public static final Key<Object> FORCE_MIDDLE_MATCH = Key.create("FORCE_MIDDLE_MATCH");
  private final Comparator<LookupElement> BY_PRESENTATION_COMPARATOR = (o1, o2) -> {
    PresentationInvariant invariant = PRESENTATION_INVARIANT.get(o1);
    assert invariant != null;
    return invariant.compareTo(PRESENTATION_INVARIANT.get(o2));
  };
  static final int MAX_PREFERRED_COUNT = 5;
  public static final String OVERFLOW_MESSAGE = "Not all variants are shown, please type more letters to see the rest";
  private static final UISettings ourUISettings = UISettings.getInstance();
  private final List<LookupElement> myFrozenItems = new ArrayList<>();
  private final int myLimit = Registry.intValue("ide.completion.variant.limit");
  private boolean myOverflow;

  @Nullable private CompletionLocation myLocation;
  private final CompletionProcessEx myProcess;
  private final Map<CompletionSorterImpl, Classifier<LookupElement>> myClassifiers = new LinkedHashMap<>();
  private final Key<CompletionSorterImpl> mySorterKey = Key.create("SORTER_KEY");
  private final CompletionFinalSorter myFinalSorter = CompletionFinalSorter.newSorter();
  private int myPrefixChanges;

  private String myLastLookupPrefix;

  public CompletionLookupArrangerImpl(CompletionProcessEx process) {
    myProcess = process;
  }

  private MultiMap<CompletionSorterImpl, LookupElement> groupItemsBySorter(Iterable<? extends LookupElement> source) {
    MultiMap<CompletionSorterImpl, LookupElement> inputBySorter = MultiMap.createLinked();
    for (LookupElement element : source) {
      inputBySorter.putValue(obtainSorter(element), element);
    }
    for (CompletionSorterImpl sorter : inputBySorter.keySet()) {
      inputBySorter.put(sorter, sortByPresentation(inputBySorter.get(sorter)));
    }

    return inputBySorter;
  }

  @NotNull
  private CompletionSorterImpl obtainSorter(LookupElement element) {
    //noinspection ConstantConditions
    return element.getUserData(mySorterKey);
  }

  @NotNull
  @Override
  public Map<LookupElement, List<Pair<String, Object>>> getRelevanceObjects(@NotNull Iterable<LookupElement> items,
                                                                               boolean hideSingleValued) {
    Map<LookupElement, List<Pair<String, Object>>> map = ContainerUtil.newIdentityHashMap();
    MultiMap<CompletionSorterImpl, LookupElement> inputBySorter = groupItemsBySorter(items);
    int sorterNumber = 0;
    for (CompletionSorterImpl sorter : inputBySorter.keySet()) {
      sorterNumber++;
      Collection<LookupElement> thisSorterItems = inputBySorter.get(sorter);
      for (LookupElement element : thisSorterItems) {
        map.put(element, ContainerUtil.newArrayList(new Pair<>("frozen", myFrozenItems.contains(element)),
                                                    new Pair<>("sorter", sorterNumber)));
      }
      ProcessingContext context = createContext();
      Classifier<LookupElement> classifier = myClassifiers.get(sorter);
      while (classifier != null) {
        final THashSet<LookupElement> itemSet = ContainerUtil.newIdentityTroveSet(thisSorterItems);
        List<LookupElement> unsortedItems = ContainerUtil.filter(myItems, lookupElement -> itemSet.contains(lookupElement));
        List<Pair<LookupElement, Object>> pairs = classifier.getSortingWeights(unsortedItems, context);
        if (!hideSingleValued || !haveSameWeights(pairs)) {
          for (Pair<LookupElement, Object> pair : pairs) {
            map.get(pair.first).add(Pair.create(classifier.getPresentableName(), pair.second));
          }
        }
        classifier = classifier.getNext();
      }
    }

    //noinspection unchecked
    Map<LookupElement, List<Pair<String, Object>>> result = new com.intellij.util.containers.hash.LinkedHashMap(EqualityPolicy.IDENTITY);
    Map<LookupElement, List<Pair<String, Object>>> additional = myFinalSorter.getRelevanceObjects(items);
    for (LookupElement item : items) {
      List<Pair<String, Object>> mainRelevance = map.get(item);
      List<Pair<String, Object>> additionalRelevance = additional.get(item);
      result.put(item, additionalRelevance == null ? mainRelevance : ContainerUtil.concat(mainRelevance, additionalRelevance));
    }
    return result;
  }

  void associateSorter(LookupElement element, CompletionSorterImpl sorter) {
    element.putUserData(mySorterKey, sorter);
  }

  private static boolean haveSameWeights(List<? extends Pair<LookupElement, Object>> pairs) {
    if (pairs.isEmpty()) return true;

    for (int i = 1; i < pairs.size(); i++) {
      if (!Comparing.equal(pairs.get(i).second, pairs.get(0).second)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public void addElement(@NotNull LookupElement element,
                         @NotNull CompletionSorter sorter,
                         @NotNull PrefixMatcher prefixMatcher,
                         @NotNull LookupElementPresentation presentation) {
    registerMatcher(element, prefixMatcher);
    associateSorter(element, (CompletionSorterImpl)sorter);
    addElement(element, presentation);
  }

  @Override
  public void addElement(@NotNull CompletionResult result) {
    LookupElementPresentation presentation = new LookupElementPresentation();
    result.getLookupElement().renderElement(presentation);
    addElement(result.getLookupElement(), result.getSorter(), result.getPrefixMatcher(), presentation);
  }

  @Override
  public void addElement(LookupElement element, LookupElementPresentation presentation) {
    StatisticsWeigher.clearBaseStatisticsInfo(element);

    PresentationInvariant invariant = new PresentationInvariant(presentation.getItemText(), presentation.getTailText(), presentation.getTypeText());
    element.putUserData(PRESENTATION_INVARIANT, invariant);

    CompletionSorterImpl sorter = obtainSorter(element);
    Classifier<LookupElement> classifier = myClassifiers.get(sorter);
    if (classifier == null) {
      myClassifiers.put(sorter, classifier = sorter.buildClassifier(new EmptyClassifier()));
    }
    ProcessingContext context = createContext();
    classifier.addElement(element, context);

    super.addElement(element, presentation);

    trimToLimit(context);
  }

  @Override
  public void itemSelected(@Nullable LookupElement lookupItem, char completionChar) {
    myProcess.itemSelected(lookupItem, completionChar);
  }

  private void trimToLimit(ProcessingContext context) {
    if (myItems.size() < myLimit) return;

    List<LookupElement> items = getMatchingItems();
    Iterator<LookupElement> iterator = sortByRelevance(groupItemsBySorter(items)).iterator();

    final Set<LookupElement> retainedSet = ContainerUtil.newIdentityTroveSet();
    retainedSet.addAll(getPrefixItems(true));
    retainedSet.addAll(getPrefixItems(false));
    retainedSet.addAll(myFrozenItems);
    while (retainedSet.size() < myLimit / 2 && iterator.hasNext()) {
      retainedSet.add(iterator.next());
    }

    if (!iterator.hasNext()) return;

    List<LookupElement> removed = retainItems(retainedSet);
    for (LookupElement element : removed) {
      removeItem(element, context);
    }

    if (!myOverflow) {
      myOverflow = true;
      myProcess.addAdvertisement(OVERFLOW_MESSAGE, null);

      // restart completion on any prefix change
      myProcess.addWatchedPrefix(0, StandardPatterns.string());

      if (ApplicationManager.getApplication().isUnitTestMode()) printTestWarning();
    }
  }

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  private void printTestWarning() {
    System.err.println("Your test might miss some lookup items, because only " + (myLimit / 2) + " most relevant items are guaranteed to be shown in the lookup. You can:");
    System.err.println("1. Make the prefix used for completion longer, so that there are less suggestions.");
    System.err.println("2. Increase 'ide.completion.variant.limit' (using RegistryValue#setValue with a test root disposable).");
    System.err.println("3. Ignore this warning.");
  }

  private void removeItem(LookupElement element, ProcessingContext context) {
    CompletionSorterImpl sorter = obtainSorter(element);
    Classifier<LookupElement> classifier = myClassifiers.get(sorter);
    classifier.removeElement(element, context);
  }

  private List<LookupElement> sortByPresentation(Iterable<? extends LookupElement> source) {
    ArrayList<LookupElement> startMatches = new ArrayList<>();
    ArrayList<LookupElement> middleMatches = new ArrayList<>();
    for (LookupElement element : source) {
      (itemMatcher(element).isStartMatch(element) ? startMatches : middleMatches).add(element);
    }
    ContainerUtil.sort(startMatches, BY_PRESENTATION_COMPARATOR);
    ContainerUtil.sort(middleMatches, BY_PRESENTATION_COMPARATOR);
    startMatches.addAll(middleMatches);
    return startMatches;
  }

  private static boolean isAlphaSorted() {
    return ourUISettings.getSortLookupElementsLexicographically();
  }

  @Override
  public Pair<List<LookupElement>, Integer> arrangeItems() {
    LookupElementListPresenter dummyListPresenter = new LookupElementListPresenter() {
      @Override
      public String getAdditionalPrefix() {
        return "";
      }

      @Override
      public LookupElement getCurrentItem() {
        return null;
      }

      @Override
      public LookupElement getCurrentItemOrEmpty() {
        return null;
      }

      @Override
      public boolean isSelectionTouched() {
        return false;
      }

      @Override
      public int getSelectedIndex() {
        return 0;
      }

      @Override
      public int getLastVisibleIndex() {
        return 0;
      }

      @Override
      public LookupImpl.FocusDegree getFocusDegree() {
        return LookupImpl.FocusDegree.FOCUSED;
      }

      @NotNull
      @Override
      public LookupFocusDegree getLookupFocusDegree() {
        return LookupFocusDegree.FOCUSED;
      }

      @Override
      public boolean isShown() {
        return true;
      }
    };
    return doArrangeItems(dummyListPresenter, false);
  }

  @Override
  public Pair<List<LookupElement>, Integer> arrangeItems(@NotNull Lookup lookup, boolean onExplicitAction) {
    return doArrangeItems((LookupElementListPresenter)lookup, onExplicitAction);
  }

  @NotNull
  private Pair<List<LookupElement>, Integer> doArrangeItems(@NotNull LookupElementListPresenter lookup, boolean onExplicitAction) {
    List<LookupElement> items = getMatchingItems();
    Iterable<? extends LookupElement> sortedByRelevance = sortByRelevance(groupItemsBySorter(items));

    if (sortedByRelevance.iterator().hasNext()) {
      sortedByRelevance = myFinalSorter.sort(sortedByRelevance, Objects.requireNonNull(myProcess.getParameters()));
    }

    LookupElement relevantSelection = findMostRelevantItem(sortedByRelevance);
    List<LookupElement> listModel = isAlphaSorted() ?
                                    sortByPresentation(items) :
                                    fillModelByRelevance(lookup, ContainerUtil.newIdentityTroveSet(items), sortedByRelevance, relevantSelection);

    int toSelect = getItemToSelect(lookup, listModel, onExplicitAction, relevantSelection);
    LOG.assertTrue(toSelect >= 0);

    return new Pair<>(listModel, toSelect);
  }

  private List<LookupElement> fillModelByRelevance(LookupElementListPresenter lookup,
                                                   Set<? extends LookupElement> items,
                                                   Iterable<? extends LookupElement> sortedElements,
                                                   @Nullable LookupElement relevantSelection) {
    Iterator<? extends LookupElement> byRelevance = sortedElements.iterator();

    final LinkedHashSet<LookupElement> model = new LinkedHashSet<>();

    addPrefixItems(model);
    addFrozenItems(items, model);
    if (model.size() < MAX_PREFERRED_COUNT) {
      addSomeItems(model, byRelevance, lastAdded -> model.size() >= MAX_PREFERRED_COUNT);
    }
    addCurrentlySelectedItemToTop(lookup, items, model);

    freezeTopItems(lookup, model);

    ensureItemAdded(items, model, byRelevance, lookup.getCurrentItem());
    ensureItemAdded(items, model, byRelevance, relevantSelection);
    ContainerUtil.addAll(model, byRelevance);

    return new ArrayList<>(model);
  }

  private static void ensureItemAdded(Set<? extends LookupElement> items,
                                      LinkedHashSet<? super LookupElement> model,
                                      Iterator<? extends LookupElement> byRelevance, @Nullable final LookupElement item) {
    if (item != null && items.contains(item) && !model.contains(item)) {
      addSomeItems(model, byRelevance, lastAdded -> lastAdded == item);
    }
  }

  private void freezeTopItems(LookupElementListPresenter lookup, LinkedHashSet<? extends LookupElement> model) {
    myFrozenItems.clear();
    if (lookup.isShown()) {
      myFrozenItems.addAll(model);
    }
  }

  private void addFrozenItems(Set<? extends LookupElement> items, LinkedHashSet<? super LookupElement> model) {
    for (Iterator<LookupElement> iterator = myFrozenItems.iterator(); iterator.hasNext(); ) {
      LookupElement element = iterator.next();
      if (!element.isValid() || !items.contains(element)) {
        iterator.remove();
      }
    }
    model.addAll(myFrozenItems);
  }

  private void addPrefixItems(LinkedHashSet<? super LookupElement> model) {
    ContainerUtil.addAll(model, sortByRelevance(groupItemsBySorter(getPrefixItems(true))));
    ContainerUtil.addAll(model, sortByRelevance(groupItemsBySorter(getPrefixItems(false))));
  }

  private static void addCurrentlySelectedItemToTop(LookupElementListPresenter lookup, Set<? extends LookupElement> items, LinkedHashSet<? super LookupElement> model) {
    if (!lookup.isSelectionTouched()) {
      LookupElement lastSelection = lookup.getCurrentItem();
      if (items.contains(lastSelection)) {
        model.add(lastSelection);
      }
    }
  }

  private static void addSomeItems(LinkedHashSet<? super LookupElement> model, Iterator<? extends LookupElement> iterator, Condition<? super LookupElement> stopWhen) {
    while (iterator.hasNext()) {
      LookupElement item = iterator.next();
      model.add(item);
      if (stopWhen.value(item)) {
        break;
      }
    }
  }

  private Iterable<LookupElement> sortByRelevance(MultiMap<CompletionSorterImpl, LookupElement> inputBySorter) {
    if (inputBySorter.isEmpty()) return Collections.emptyList();

    final List<Iterable<LookupElement>> byClassifier = new ArrayList<>();
    for (CompletionSorterImpl sorter : myClassifiers.keySet()) {
      ProcessingContext context = createContext();
      byClassifier.add(myClassifiers.get(sorter).classify(inputBySorter.get(sorter), context));
    }
    //noinspection unchecked
    return ContainerUtil.concat(byClassifier.toArray(new Iterable[0]));
  }

  private ProcessingContext createContext() {
    ProcessingContext context = new ProcessingContext();
    context.put(PREFIX_CHANGES, myPrefixChanges);
    context.put(WEIGHING_CONTEXT, this);
    return context;
  }

  void setLastLookupPrefix(String lookupPrefix) {
    myLastLookupPrefix = lookupPrefix;
  }
  public String getLastLookupPrefix() {
    return myLastLookupPrefix;
  }

  @Override
  public LookupArranger createEmptyCopy() {
    return new CompletionLookupArrangerImpl(myProcess);
  }

  private int getItemToSelect(LookupElementListPresenter lookup, List<? extends LookupElement> items, boolean onExplicitAction, @Nullable LookupElement mostRelevant) {
    if (items.isEmpty() || lookup.getLookupFocusDegree() == LookupFocusDegree.UNFOCUSED) {
      return 0;
    }

    if (lookup.isSelectionTouched() || !onExplicitAction) {
      final LookupElement lastSelection = lookup.getCurrentItem();
      int old = ContainerUtil.indexOfIdentity(items, lastSelection);
      if (old >= 0) {
        return old;
      }

      LookupElement selectedValue = lookup.getCurrentItemOrEmpty();
      if (selectedValue instanceof EmptyLookupItem && ((EmptyLookupItem)selectedValue).isLoading()) {
        int index = lookup.getSelectedIndex();
        if (index >= 0 && index < items.size()) {
          return index;
        }
      }

      for (int i = 0; i < items.size(); i++) {
        PresentationInvariant invariant = PRESENTATION_INVARIANT.get(items.get(i));
        if (invariant != null && invariant.equals(PRESENTATION_INVARIANT.get(lastSelection))) {
          return i;
        }
      }
    }

    LookupElement exactMatch = getBestExactMatch(items);
    return Math.max(0, ContainerUtil.indexOfIdentity(items, exactMatch != null ? exactMatch : mostRelevant));
  }

  private List<LookupElement> getExactMatches(List<? extends LookupElement> items) {
    String selectedText = InjectedLanguageUtil.getTopLevelEditor(myProcess.getParameters().getEditor()).getSelectionModel().getSelectedText();
    List<LookupElement> exactMatches = new SmartList<>();
    for (int i = 0; i < items.size(); i++) {
      LookupElement item = items.get(i);
      boolean isSuddenLiveTemplate = isSuddenLiveTemplate(item);
      if (isPrefixItem(item, true) && !isSuddenLiveTemplate || item.getLookupString().equals(selectedText)) {
        if (item instanceof LiveTemplateLookupElement) {
          // prefer most recent live template lookup item
          return Collections.singletonList(item);
        }
        exactMatches.add(item);
      }
      else if (i == 0 && isSuddenLiveTemplate && items.size() > 1 && !CompletionServiceImpl.isStartMatch(items.get(1), this)) {
        return Collections.singletonList(item);
      }
    }
    return exactMatches;
  }

  @Nullable
  private LookupElement getBestExactMatch(List<? extends LookupElement> items) {
    List<LookupElement> exactMatches = getExactMatches(items);
    if (exactMatches.isEmpty()) return null;

    if (exactMatches.size() == 1) {
      return exactMatches.get(0);
    }

    return sortByRelevance(groupItemsBySorter(exactMatches)).iterator().next();
  }

  @Nullable
  private LookupElement findMostRelevantItem(Iterable<? extends LookupElement> sorted) {
    final CompletionPreselectSkipper[] skippers = CompletionPreselectSkipper.EP_NAME.getExtensions();

    for (LookupElement element : sorted) {
      if (!shouldSkip(skippers, element)) {
        return element;
      }
    }

    return null;
  }


  private static boolean isSuddenLiveTemplate(LookupElement element) {
    return element instanceof LiveTemplateLookupElement && ((LiveTemplateLookupElement)element).sudden;
  }

  private boolean shouldSkip(CompletionPreselectSkipper[] skippers, LookupElement element) {
    CompletionLocation location = myLocation;
    if (location == null) {
      location = new CompletionLocation(Objects.requireNonNull(myProcess.getParameters()));
    }
    for (final CompletionPreselectSkipper skipper : skippers) {
      if (skipper.skipElement(element, location)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Skipped element " + element + " by " + skipper);
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public void prefixChanged(Lookup lookup) {
    myPrefixChanges++;
    myFrozenItems.clear();
    super.prefixChanged(lookup);
  }

  @Override
  public void prefixTruncated(@NotNull LookupImpl lookup, int hideOffset) {
    if (hideOffset < lookup.getEditor().getCaretModel().getOffset()) {
      myProcess.scheduleRestart();
      return;
    }
    myProcess.prefixUpdated();
    lookup.hideLookup(false);
  }

  @Override
  public boolean isCompletion() {
    return true;
  }

  private static class EmptyClassifier extends Classifier<LookupElement> {

    private EmptyClassifier() {
      super(null, "empty");
    }

    @NotNull
    @Override
    public List<Pair<LookupElement, Object>> getSortingWeights(@NotNull Iterable<LookupElement> items, @NotNull ProcessingContext context) {
      return Collections.emptyList();
    }

    @NotNull
    @Override
    public Iterable<LookupElement> classify(@NotNull Iterable<LookupElement> source, @NotNull ProcessingContext context) {
      return source;
    }

  }
}
