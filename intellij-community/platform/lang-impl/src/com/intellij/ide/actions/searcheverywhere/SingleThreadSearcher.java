// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.util.ConcurrencyUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * Single thread implementation of {@link SESearcher}.
 * Being used only as a temporary solution in case of problems with {@link MultiThreadSearcher}.
 */
@Deprecated
class SingleThreadSearcher implements SESearcher {

  private final Executor myNotificationExecutor;
  private final Listener myNotificationListener;
  private final SEResultsEqualityProvider myEqualityProvider;

  SingleThreadSearcher(Listener listener,
                       Executor executor,
                       @NotNull Collection<SEResultsEqualityProvider> equalityProviders) {
    myNotificationExecutor = executor;
    myNotificationListener = listener;
    myEqualityProvider = SEResultsEqualityProvider.composite(equalityProviders);
  }

  @Override
  public ProgressIndicator search(@NotNull Map<? extends SearchEverywhereContributor<?, ?>, Integer> contributorsAndLimits,
                                  @NotNull String pattern,
                                  boolean useNonProjectItems,
                                  @NotNull Function<? super SearchEverywhereContributor<?, ?>, ? extends SearchEverywhereContributorFilter<?>> filterSupplier) {
    ProgressIndicator indicator = new ProgressIndicatorBase();
    Runnable task = new SearchTask(contributorsAndLimits, pattern, useNonProjectItems, filterSupplier, indicator, myNotificationExecutor,
                                   myNotificationListener, myEqualityProvider);
    ApplicationManager.getApplication().executeOnPooledThread(ConcurrencyUtil.underThreadNameRunnable("SE-SingleThread-SearchTask", task));

    return indicator;
  }

  @Override
  public ProgressIndicator findMoreItems(@NotNull Map<? extends SearchEverywhereContributor<?, ?>, Collection<SearchEverywhereFoundElementInfo>> alreadyFound,
                                         @NotNull String pattern,
                                         boolean useNonProjectItems,
                                         @NotNull SearchEverywhereContributor<?, ?> contributor,
                                         int newLimit,
                                         @NotNull Function<? super SearchEverywhereContributor<?, ?>, ? extends SearchEverywhereContributorFilter<?>> filterSupplier) {
    ProgressIndicator indicator = new ProgressIndicatorBase();
    Runnable task = createShowMoreTask(contributor, newLimit, pattern, useNonProjectItems, alreadyFound, filterSupplier, indicator);
    ApplicationManager.getApplication().executeOnPooledThread(ConcurrencyUtil.underThreadNameRunnable("SE-SingleThread-SearchTask", task));

    return indicator;
  }

  @NotNull
  private Runnable createShowMoreTask(SearchEverywhereContributor<?, ?> contributor,
                                                 int newLimit,
                                                 String pattern,
                                                 boolean useNonProjectItems,
                                                 Map<? extends SearchEverywhereContributor<?, ?>, Collection<SearchEverywhereFoundElementInfo>> alreadyFound,
                                                 Function<? super SearchEverywhereContributor<?, ?>, ? extends SearchEverywhereContributorFilter<?>> filterSupplier,
                                                 ProgressIndicator indicator) {
    List<SearchEverywhereFoundElementInfo> alreadyFoundList = alreadyFound.values()
      .stream()
      .collect(Collector.of(() -> new ArrayList<>(), (list, infos) -> list.addAll(infos), (left, right) -> {
        left.addAll(right);
        return left;
      }));
    //noinspection unchecked
    return new ShowMoreTask<>((SearchEverywhereContributor<Object, Object>)contributor, newLimit, pattern, useNonProjectItems,
                              (SearchEverywhereContributorFilter<Object>)filterSupplier.apply(contributor), alreadyFoundList, indicator,
                              myNotificationExecutor, myNotificationListener, myEqualityProvider);
  }

  private static class UpdateInfo {
    private final List<SearchEverywhereFoundElementInfo> addedElements = new ArrayList<>();
    private final List<SearchEverywhereFoundElementInfo> removedElements = new ArrayList<>();
    private boolean hasMore = false;
  }

  private static UpdateInfo calculateUpdates(SearchEverywhereContributor<?, ?> contributor,
                                             String pattern,
                                             int limit,
                                             boolean everywhere,
                                             SearchEverywhereContributorFilter<?> filter,
                                             ProgressIndicator progressIndicator,
                                             Collection<SearchEverywhereFoundElementInfo> alreadyFound,
                                             SEResultsEqualityProvider equalityProvider) {
    //noinspection unchecked
    return doCalculateUpdates(((SearchEverywhereContributor<Object, Object>)contributor), pattern, limit, everywhere,
                              (SearchEverywhereContributorFilter<Object>)filter, progressIndicator, alreadyFound, equalityProvider);
  }

  private static <Item, Filter> UpdateInfo doCalculateUpdates(SearchEverywhereContributor<Item, Filter> contributor,
                                                              String pattern,
                                                              int limit,
                                                              boolean everywhere,
                                                              SearchEverywhereContributorFilter<Filter> filter,
                                                              ProgressIndicator progressIndicator,
                                                              Collection<SearchEverywhereFoundElementInfo> alreadyFound,
                                                              SEResultsEqualityProvider equalityProvider) {
    UpdateInfo res = new UpdateInfo();

    contributor.fetchElements(pattern, everywhere, filter, progressIndicator, newElement -> {
      if (newElement == null) {
        return true;
      }

      if (res.addedElements.size() >= limit) {
        res.hasMore = true;
        return false;
      }

      int priority = contributor.getElementPriority(newElement, pattern);
      SearchEverywhereFoundElementInfo newInfo = new SearchEverywhereFoundElementInfo(newElement, priority, contributor);
      boolean shouldBeAdded = processSameElements(newInfo, alreadyFound, res, equalityProvider);
      if (!shouldBeAdded) {
        return true;
      }
      res.addedElements.add(newInfo);
      alreadyFound.add(newInfo);

      return true;
    });

    return res;
  }

  /**
   * @return true if new element should be added to result or false if it should be skipped
   */
  private static boolean processSameElements(SearchEverywhereFoundElementInfo newInfo,
                                             Collection<SearchEverywhereFoundElementInfo> alreadyFound,
                                             UpdateInfo res,
                                             SEResultsEqualityProvider equalityProvider) {
    Map<SEResultsEqualityProvider.SEEqualElementsActionType, Collection<SearchEverywhereFoundElementInfo>> sameItemsMap = new EnumMap<>(
      SEResultsEqualityProvider.SEEqualElementsActionType.class);
    sameItemsMap.put(SEResultsEqualityProvider.SEEqualElementsActionType.SKIP, new ArrayList<>());
    sameItemsMap.put(SEResultsEqualityProvider.SEEqualElementsActionType.REPLACE, new ArrayList<>());

    alreadyFound.forEach(info -> {
      SEResultsEqualityProvider.SEEqualElementsActionType action = equalityProvider.compareItems(newInfo, info);
      if (action != SEResultsEqualityProvider.SEEqualElementsActionType.DO_NOTHING) {
        sameItemsMap.get(action).add(info);
      }
    });

    Collection<SearchEverywhereFoundElementInfo> toReplace = sameItemsMap.get(SEResultsEqualityProvider.SEEqualElementsActionType.REPLACE);
    if (!toReplace.isEmpty()) {
      toReplace.forEach(info -> {
        res.removedElements.add(info);
        alreadyFound.remove(info);
      });
      return true;
    }

    return sameItemsMap.get(SEResultsEqualityProvider.SEEqualElementsActionType.SKIP).isEmpty();
  }

  private static class SearchTask implements Runnable {
    private final Map<? extends SearchEverywhereContributor<?, ?>, Integer> myContributorsAndLimits;
    private final String myPattern;
    private final boolean myUseNonProjectItems;
    private final Function<? super SearchEverywhereContributor<?, ?>, ? extends SearchEverywhereContributorFilter<?>> myFilterSupplier;
    private final SEResultsEqualityProvider myEqualityProvider;

    private final ProgressIndicator myProgressIndicator;
    private final Executor notificationExecutor;
    private final Listener notificationListener;

    SearchTask(Map<? extends SearchEverywhereContributor<?, ?>, Integer> contributorsAndLimits,
               String pattern,
               boolean useNonProjectItems,
               Function<? super SearchEverywhereContributor<?, ?>, ? extends SearchEverywhereContributorFilter<?>> filterSupplier,
               ProgressIndicator progressIndicator,
               Executor notificationExecutor,
               Listener notificationListener,
               SEResultsEqualityProvider equalityProvider) {
      myContributorsAndLimits = contributorsAndLimits;
      myPattern = pattern;
      myUseNonProjectItems = useNonProjectItems;
      myFilterSupplier = filterSupplier;
      myEqualityProvider = equalityProvider;
      myProgressIndicator = progressIndicator;
      this.notificationExecutor = notificationExecutor;
      this.notificationListener = notificationListener;
    }

    @Override
    public void run() {
      Map<SearchEverywhereContributor<?, ?>, Boolean> hasMoreContributors = new HashMap<>();
      Collection<SearchEverywhereFoundElementInfo> alreadyFound = new ArrayList<>();

      myContributorsAndLimits.entrySet()
        .stream()
        .sorted(Comparator.comparingInt(entry -> entry.getKey().getSortWeight()))
        .forEach(entry -> {
          SearchEverywhereContributor<?, ?> contributor = entry.getKey();
          UpdateInfo updates = calculateUpdates(contributor, myPattern, entry.getValue(), myUseNonProjectItems,
                                                myFilterSupplier.apply(contributor), myProgressIndicator, alreadyFound, myEqualityProvider);
          notificationExecutor.execute(() -> notificationListener.elementsAdded(updates.addedElements));
          notificationExecutor.execute(() -> notificationListener.elementsRemoved(updates.removedElements));
          hasMoreContributors.put(contributor, updates.hasMore);
        });

      notificationExecutor.execute(() -> notificationListener.searchFinished(hasMoreContributors));
    }
  }

  private static class ShowMoreTask<Filter> implements Runnable {
    private final SearchEverywhereContributor<?, Filter> myContributor;
    private final int myLimit;
    private final String myPattern;
    private final boolean myUseNonProjectItems;
    private final SearchEverywhereContributorFilter<Filter> myFilter;
    private final List<SearchEverywhereFoundElementInfo> myAlreadyFound;

    private final ProgressIndicator myProgressIndicator;
    private final Executor notificationExecutor;
    private final Listener notificationListener;
    private final SEResultsEqualityProvider myEqualityProvider;

    private ShowMoreTask(SearchEverywhereContributor<?, Filter> contributor,
                         int limit,
                         String pattern,
                         boolean useNonProjectItems,
                         SearchEverywhereContributorFilter<Filter> filter,
                         List<SearchEverywhereFoundElementInfo> alreadyFound,
                         ProgressIndicator indicator,
                         Executor executor,
                         Listener listener,
                         SEResultsEqualityProvider equalityProvider) {
      myContributor = contributor;
      myLimit = limit;
      myPattern = pattern;
      myUseNonProjectItems = useNonProjectItems;
      myFilter = filter;
      myAlreadyFound = alreadyFound;
      myProgressIndicator = indicator;
      notificationExecutor = executor;
      notificationListener = listener;
      myEqualityProvider = equalityProvider;
    }

    @Override
    public void run() {
      UpdateInfo updates = calculateUpdates(myContributor, myPattern, myLimit, myUseNonProjectItems, myFilter, myProgressIndicator,
                                            new ArrayList<>(myAlreadyFound), myEqualityProvider);
      notificationExecutor.execute(() -> notificationListener.elementsAdded(updates.addedElements));
      notificationExecutor.execute(() -> notificationListener.elementsRemoved(updates.removedElements));
      notificationExecutor.execute(() -> notificationListener.searchFinished(Collections.singletonMap(myContributor, updates.hasMore)));
    }
  }
}
