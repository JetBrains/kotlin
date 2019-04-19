// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.lookup;

import com.intellij.codeInsight.completion.LookupElementListPresenter;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author peter
 */
public abstract class LookupArranger implements WeighingContext {
  protected final List<LookupElement> myItems = new ArrayList<>();
  private final List<LookupElement> myMatchingItems = new ArrayList<>();
  private final List<LookupElement> myExactPrefixItems = new ArrayList<>();
  private final List<LookupElement> myInexactPrefixItems = new ArrayList<>();
  private final Key<PrefixMatcher> myMatcherKey = Key.create("LookupArrangerMatcher");
  private String myAdditionalPrefix = "";

  public void addElement(LookupElement item, LookupElementPresentation presentation) {
    myItems.add(item);
    updateCache(item);
  }

  public void clear() {
    myItems.clear();
    myMatchingItems.clear();
    myExactPrefixItems.clear();
    myInexactPrefixItems.clear();
  }

  private void updateCache(LookupElement item) {
    if (!prefixMatches(item)) {
      return;
    }
    myMatchingItems.add(item);

    if (isPrefixItem(item, true)) {
      myExactPrefixItems.add(item);
    } else if (isPrefixItem(item, false)) {
      myInexactPrefixItems.add(item);
    }
  }

  public void registerMatcher(@NotNull LookupElement item, @NotNull PrefixMatcher matcher) {
    item.putUserData(myMatcherKey, matcher);
  }

  @Override
  @NotNull
  public String itemPattern(@NotNull LookupElement element) {
    String prefix = itemMatcher(element).getPrefix();
    String additionalPrefix = myAdditionalPrefix;
    return additionalPrefix.isEmpty() ? prefix : prefix + additionalPrefix;
  }

  @Override
  @NotNull
  public PrefixMatcher itemMatcher(@NotNull LookupElement item) {
    PrefixMatcher matcher = item.getUserData(myMatcherKey);
    if (matcher == null) {
      throw new AssertionError("Item not in lookup: item=" + item + "; lookup items=" + myItems);
    }
    return matcher;
  }

  private boolean prefixMatches(LookupElement item) {
    PrefixMatcher matcher = itemMatcher(item);
    if (!myAdditionalPrefix.isEmpty()) {
      matcher = matcher.cloneWithPrefix(matcher.getPrefix() + myAdditionalPrefix);
    }
    return matcher.prefixMatches(item);
  }

  public void itemSelected(@Nullable LookupElement lookupItem, char completionChar) {
  }

  public final void prefixReplaced(Lookup lookup, String newPrefix) {
    ArrayList<LookupElement> itemCopy = ContainerUtil.newArrayList(myItems);
    myItems.clear();
    for (LookupElement item : itemCopy) {
      if (item.isValid()) {
        PrefixMatcher matcher = itemMatcher(item).cloneWithPrefix(newPrefix);
        if (matcher.prefixMatches(item)) {
          item.putUserData(myMatcherKey, matcher);
          myItems.add(item);
        }
      }
    }

    prefixChanged(lookup);
  }

  public void prefixChanged(Lookup lookup) {
    myAdditionalPrefix = ((LookupElementListPresenter)lookup).getAdditionalPrefix();
    rebuildItemCache();
  }

  private void rebuildItemCache() {
    myMatchingItems.clear();
    myExactPrefixItems.clear();
    myInexactPrefixItems.clear();

    for (LookupElement item : myItems) {
      updateCache(item);
    }
  }

  protected List<LookupElement> retainItems(final Set<LookupElement> retained) {
    List<LookupElement> filtered = ContainerUtil.newArrayList();
    List<LookupElement> removed = ContainerUtil.newArrayList();
    for (LookupElement item : myItems) {
      (retained.contains(item) ? filtered : removed).add(item);
    }
    myItems.clear();
    myItems.addAll(filtered);

    rebuildItemCache();
    return removed;
  }

  public abstract Pair<List<LookupElement>, Integer> arrangeItems(@NotNull Lookup lookup, boolean onExplicitAction);

  public abstract LookupArranger createEmptyCopy();

  protected List<LookupElement> getPrefixItems(boolean exactly) {
    return Collections.unmodifiableList(exactly ? myExactPrefixItems : myInexactPrefixItems);
  }

  protected boolean isPrefixItem(LookupElement item, final boolean exactly) {
    final String pattern = itemPattern(item);
    for (String s : item.getAllLookupStrings()) {
      if (!s.equalsIgnoreCase(pattern)) continue;

      if (!item.isCaseSensitive() || !exactly || s.equals(pattern)) {
        return true;
      }
    }
    return false;
  }

  public List<LookupElement> getMatchingItems() {
    return myMatchingItems;
  }

  /**
   * @param items the items to give relevance weight for
   * @param hideSingleValued whether criteria that gave same values for all items should be skipped
   * @return for each item, an (ordered) map of criteria used for lookup relevance sorting
   * along with the objects representing the weights in these criteria
   */
  @NotNull
  public Map<LookupElement, List<Pair<String, Object>>> getRelevanceObjects(@NotNull Iterable<LookupElement> items,
                                                                               boolean hideSingleValued) {
    return Collections.emptyMap();
  }

  /**
   * Called when the prefix has been truncated farther than the additional prefix typed while the lookup was visible.
   */
  public void prefixTruncated(@NotNull LookupImpl lookup, int hideOffset) {
    lookup.hideLookup(false);
  }

  public boolean isCompletion() {
    return false;
  }

  public static class DefaultArranger extends LookupArranger {
    @Override
    public Pair<List<LookupElement>, Integer> arrangeItems(@NotNull Lookup lookup, boolean onExplicitAction) {
      LinkedHashSet<LookupElement> result = new LinkedHashSet<>();
      result.addAll(getPrefixItems(true));
      result.addAll(getPrefixItems(false));

      List<LookupElement> items = getMatchingItems();
      for (LookupElement item : items) {
        if (CompletionServiceImpl.isStartMatch(item, this)) {
          result.add(item);
        }
      }
      result.addAll(items);
      ArrayList<LookupElement> list = new ArrayList<>(result);
      int selected = !lookup.isSelectionTouched() && onExplicitAction ? 0 : list.indexOf(lookup.getCurrentItem());
      return new Pair<>(list, selected >= 0 ? selected : 0);
    }

    @Override
    public LookupArranger createEmptyCopy() {
      return new DefaultArranger();
    }
  }
}
