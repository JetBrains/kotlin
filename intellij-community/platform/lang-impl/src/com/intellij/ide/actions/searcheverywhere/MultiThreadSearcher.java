// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.intellij.ide.actions.searcheverywhere.SEResultsEqualityProvider.SEEqualElementsActionType.REPLACE;
import static com.intellij.ide.actions.searcheverywhere.SEResultsEqualityProvider.SEEqualElementsActionType.SKIP;

/**
 * @author msokolov
 */
class MultiThreadSearcher implements SESearcher {

  private static final Logger LOG = Logger.getInstance(MultiThreadSearcher.class);

  @NotNull private final Listener myListener;
  @NotNull private final Executor myNotificationExecutor;
  @NotNull private final SEResultsEqualityProvider myEqualityProvider;

  /**
   * Creates MultiThreadSearcher with search results {@link Listener} and specifies executor which going to be used to call listener methods.
   * Use this constructor when you for example need to receive listener events only in AWT thread
   * @param listener {@link Listener} to get notifications about searching process
   * @param notificationExecutor searcher guarantees that all listener methods will be called only through this executor
   * @param equalityProviders collection of equality providers that checks if found elements are already in the search results
   */
  MultiThreadSearcher(@NotNull Listener listener,
                      @NotNull Executor notificationExecutor,
                      @NotNull Collection<? extends SEResultsEqualityProvider> equalityProviders) {
    myListener = listener;
    myNotificationExecutor = notificationExecutor;
    myEqualityProvider = SEResultsEqualityProvider.composite(equalityProviders);
  }

  /**
   * Starts searching process with given search parameters
   * @param contributorsAndLimits map of used searching contributors and maximum elements limit for them
   * @param pattern search pattern
   * @param useNonProjectItems flags indicating if non-projects items should be included in search results
   * @param filterSupplier supplier of {@link SearchEverywhereContributorFilter}'s for different search contributors
   * @return {@link ProgressIndicator} that could be used to track and/or cancel searching process
   */
  @Override
  public ProgressIndicator search(@NotNull Map<? extends SearchEverywhereContributor<?, ?>, Integer> contributorsAndLimits,
                                  @NotNull String pattern,
                                  boolean useNonProjectItems,
                                  @NotNull Function<? super SearchEverywhereContributor<?, ?>, ? extends SearchEverywhereContributorFilter<?>> filterSupplier) {
    LOG.debug("Search started for pattern [", pattern, "]");

    Collection<? extends SearchEverywhereContributor<?, ?>> contributors = contributorsAndLimits.keySet();
    if (pattern.isEmpty()) {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        contributors = Collections.emptySet(); //empty search string is not allowed for tests
      }
      else {
        contributors = ContainerUtil.filter(contributors, contributor -> contributor.isEmptyPatternSupported());
      }
    }

    ProgressIndicator indicator;
    FullSearchResultsAccumulator accumulator;
    if (!contributors.isEmpty()) {
      CountDownLatch latch = new CountDownLatch(contributors.size());
      ProgressIndicatorWithCancelListener indicatorWithCancelListener = new ProgressIndicatorWithCancelListener();
      accumulator = new FullSearchResultsAccumulator(contributorsAndLimits, myEqualityProvider, myListener,
                                                                                  myNotificationExecutor, indicatorWithCancelListener);

      for (SearchEverywhereContributor<?, ?> contributor : contributors) {
        SearchEverywhereContributorFilter<?> filter = filterSupplier.apply(contributor);
        Runnable task = createSearchTask(pattern, useNonProjectItems, accumulator,
                                         indicatorWithCancelListener, contributor, filter, () -> latch.countDown());
        ApplicationManager.getApplication().executeOnPooledThread(task);
      }

      Runnable finisherTask = createFinisherTask(latch, accumulator, indicatorWithCancelListener);
      Future<?> finisherFeature = ApplicationManager.getApplication().executeOnPooledThread(finisherTask);
      indicatorWithCancelListener.setCancelCallback(() -> {
        accumulator.stop();
        finisherFeature.cancel(true);
      });
      indicator = indicatorWithCancelListener;
    }
    else {
      indicator = new ProgressIndicatorBase();
      accumulator = new FullSearchResultsAccumulator(contributorsAndLimits, myEqualityProvider, myListener, myNotificationExecutor, indicator);
    }

    indicator.start();
    if (contributors.isEmpty()) {
      indicator.stop();
      accumulator.searchFinished();
    }

    return indicator;
  }

  /**
   * Starts process of expanding (search for more elements) specified contributors section (when user chose "more" item)
   * @param alreadyFound map of already found items for all used search contributors
   * @param pattern search pattern
   * @param useNonProjectItems flags indicating if non-projects items should be included in search results
   * @param contributor specifies {@link SearchEverywhereContributor} element which going to be expanded
   * @param newLimit new maximum elements limit for expanded contributor
   * @param filterSupplier supplier of {@link SearchEverywhereContributorFilter}'s for different search contributors
   * @return {@link ProgressIndicator} that could be used to track and/or cancel searching process
   */
  @Override
  public ProgressIndicator findMoreItems(@NotNull Map<? extends SearchEverywhereContributor<?, ?>, Collection<SearchEverywhereFoundElementInfo>> alreadyFound,
                                         @NotNull String pattern,
                                         boolean useNonProjectItems,
                                         @NotNull SearchEverywhereContributor<?, ?> contributor,
                                         int newLimit,
                                         @NotNull Function<? super SearchEverywhereContributor<?, ?>, ? extends SearchEverywhereContributorFilter<?>> filterSupplier) {
    SearchEverywhereContributorFilter<?> filter = filterSupplier.apply(contributor);
    ProgressIndicator indicator = new ProgressIndicatorBase();
    ResultsAccumulator accumulator = new ShowMoreResultsAccumulator(alreadyFound, myEqualityProvider, contributor, newLimit,
                                                                    myListener, myNotificationExecutor, indicator);
    indicator.start();
    Runnable task = createSearchTask(pattern, useNonProjectItems, accumulator, indicator, contributor, filter, () -> indicator.stop());
    ApplicationManager.getApplication().executeOnPooledThread(task);

    return indicator;
  }

  @NotNull
  private static Runnable createSearchTask(String pattern,
                                           boolean useNonProjectItems,
                                           ResultsAccumulator accumulator,
                                           ProgressIndicator indicator,
                                           SearchEverywhereContributor<?, ?> contributor,
                                           SearchEverywhereContributorFilter<?> filter,
                                           Runnable finalCallback) {
    //noinspection unchecked
    ContributorSearchTask<?, ?> task = new ContributorSearchTask<>(
      (SearchEverywhereContributor<Object, Object>)contributor, pattern,
      (SearchEverywhereContributorFilter<Object>)filter, useNonProjectItems, accumulator, indicator, finalCallback);
    return ConcurrencyUtil.underThreadNameRunnable("SE-SearchTask", task);
  }

  private static Runnable createFinisherTask(CountDownLatch latch, FullSearchResultsAccumulator accumulator, ProgressIndicator indicator) {
    return ConcurrencyUtil.underThreadNameRunnable("SE-FinisherTask", () -> {
      try {
        latch.await();
        if (!indicator.isCanceled()) {
          accumulator.searchFinished();
        }
        indicator.stop();
      }
      catch (InterruptedException e) {
        LOG.debug("Finisher interrupted before search process is finished");
      }
    });
  }

  private static class ContributorSearchTask<Item, Filter> implements Runnable {

    private final ResultsAccumulator myAccumulator;
    private final Runnable finishCallback;

    private final SearchEverywhereContributor<Item, Filter> myContributor;
    private final SearchEverywhereContributorFilter<Filter> filter;
    private final String myPattern;
    private final boolean myUseNonProjectItems;
    private final ProgressIndicator myIndicator;

    private ContributorSearchTask(SearchEverywhereContributor<Item, Filter> contributor,
                                  String pattern,
                                  SearchEverywhereContributorFilter<Filter> filter,
                                  boolean everywhere,
                                  ResultsAccumulator accumulator, ProgressIndicator indicator, Runnable callback) {
      myContributor = contributor;
      myPattern = pattern;
      this.filter = filter;
      myUseNonProjectItems = everywhere;
      myAccumulator = accumulator;
      myIndicator = indicator;
      finishCallback = callback;
    }


    @Override
    public void run() {
      LOG.debug("Search task started for contributor ", myContributor);
      try {
        boolean repeat;
        do {
          ProgressIndicator wrapperIndicator = new SensitiveProgressWrapper(myIndicator);
          try {
            ProgressManager.getInstance().runProcess(() -> myContributor.fetchElements(
              myPattern, myUseNonProjectItems, filter, wrapperIndicator,
              element -> {
                try {
                  if (element == null) {
                    LOG.debug("Skip null element");
                    return true;
                  }

                  int priority = myContributor.getElementPriority(element, myPattern);
                  boolean added = myAccumulator.addElement(element, myContributor, priority, wrapperIndicator);
                  if (!added) {
                    myAccumulator.setContributorHasMore(myContributor, true);
                  }
                  return added;
                }
                catch (InterruptedException e) {
                  LOG.warn("Search task was interrupted");
                  return false;
                }
              }), wrapperIndicator);
          }
          catch (ProcessCanceledException ignore) {}
          repeat = !myIndicator.isCanceled() && wrapperIndicator.isCanceled();
        }
        while (repeat);

        if (myIndicator.isCanceled()) {
          return;
        }
        myAccumulator.contributorFinished(myContributor);
      }
      finally {
        finishCallback.run();
      }
      LOG.debug("Search task finished for contributor ", myContributor);
    }
  }

  private static abstract class ResultsAccumulator {
    protected final Map<SearchEverywhereContributor<?, ?>, Collection<SearchEverywhereFoundElementInfo>> sections;
    protected final MultiThreadSearcher.Listener myListener;
    protected final Executor myNotificationExecutor;
    protected final SEResultsEqualityProvider myEqualityProvider;
    protected final ProgressIndicator myProgressIndicator;

    ResultsAccumulator(Map<SearchEverywhereContributor<?, ?>, Collection<SearchEverywhereFoundElementInfo>> sections,
                       SEResultsEqualityProvider equalityProvider,
                       Listener listener,
                       Executor notificationExecutor,
                       ProgressIndicator progressIndicator) {
      this.sections = sections;
      myEqualityProvider = equalityProvider;
      myListener = listener;
      myNotificationExecutor = notificationExecutor;
      myProgressIndicator = progressIndicator;
    }

    protected Map<SEResultsEqualityProvider.SEEqualElementsActionType, Collection<SearchEverywhereFoundElementInfo>> getActionsWithOtherElements(
      SearchEverywhereFoundElementInfo newElement) {
      Map<SEResultsEqualityProvider.SEEqualElementsActionType, Collection<SearchEverywhereFoundElementInfo>> res = new EnumMap<>(
        SEResultsEqualityProvider.SEEqualElementsActionType.class);
      res.put(REPLACE, new ArrayList<>());
      res.put(SKIP, new ArrayList<>());
      sections.values()
        .stream()
        .flatMap(Collection::stream)
        .forEach(info -> {
          SEResultsEqualityProvider.SEEqualElementsActionType action = myEqualityProvider.compareItems(newElement, info);
          if (action != SEResultsEqualityProvider.SEEqualElementsActionType.DO_NOTHING) {
            res.get(action).add(info);
          }
        });

      return res;
    }

    protected void runInNotificationExecutor(Runnable runnable) {
      myNotificationExecutor.execute(() -> {
        if (!myProgressIndicator.isCanceled()) {
          runnable.run();
        }
      });
    }

    public abstract boolean addElement(Object element, SearchEverywhereContributor<?, ?> contributor, int priority, ProgressIndicator indicator) throws InterruptedException;
    public abstract void contributorFinished(SearchEverywhereContributor<?, ?> contributor);
    public abstract void setContributorHasMore(SearchEverywhereContributor<?, ?> contributor, boolean hasMore);
  }

  private static class ShowMoreResultsAccumulator extends ResultsAccumulator {
    private final SearchEverywhereContributor<?, ?> myExpandedContributor;
    private final int myNewLimit;
    private volatile boolean hasMore;

    ShowMoreResultsAccumulator(Map<? extends SearchEverywhereContributor<?, ?>, Collection<SearchEverywhereFoundElementInfo>> alreadyFound, SEResultsEqualityProvider equalityProvider,
                               SearchEverywhereContributor<?, ?> contributor, int newLimit, Listener listener, Executor notificationExecutor, ProgressIndicator progressIndicator) {
      super(new ConcurrentHashMap<>(alreadyFound), equalityProvider, listener, notificationExecutor, progressIndicator);
      myExpandedContributor = contributor;
      myNewLimit = newLimit;
    }

    @Override
    public boolean addElement(Object element, SearchEverywhereContributor<?, ?> contributor, int priority, ProgressIndicator indicator) {
      assert contributor == myExpandedContributor; // Only expanded contributor items allowed

      Collection<SearchEverywhereFoundElementInfo> section = sections.get(contributor);
      SearchEverywhereFoundElementInfo newElementInfo = new SearchEverywhereFoundElementInfo(element, priority, contributor);

      if (section.size() >= myNewLimit) {
        return false;
      }

      Map<SEResultsEqualityProvider.SEEqualElementsActionType, Collection<SearchEverywhereFoundElementInfo>> otherElementsMap = getActionsWithOtherElements(newElementInfo);
      if (otherElementsMap.get(REPLACE).isEmpty() && !otherElementsMap.get(SKIP).isEmpty()) {
        LOG.debug(String.format("Element %s for contributor %s was skipped", element.toString(), contributor.getSearchProviderId()));
        return true;
      }

      section.add(newElementInfo);
      runInNotificationExecutor(() -> myListener.elementsAdded(Collections.singletonList(newElementInfo)));

      List<SearchEverywhereFoundElementInfo> toRemove = new ArrayList<>(otherElementsMap.get(REPLACE));
      toRemove.forEach(info -> {
        Collection<SearchEverywhereFoundElementInfo> list = sections.get(info.getContributor());
            list.remove(info);
            LOG.debug(String.format("Element %s for contributor %s is removed", info.getElement().toString(), info.getContributor().getSearchProviderId()));
      });
      runInNotificationExecutor(() -> myListener.elementsRemoved(toRemove));
      return true;
    }

    @Override
    public void setContributorHasMore(SearchEverywhereContributor<?, ?> contributor, boolean hasMore) {
      assert contributor == myExpandedContributor; // Only expanded contributor items allowed
      this.hasMore = hasMore;

    }

    @Override
    public void contributorFinished(SearchEverywhereContributor<?, ?> contributor) {
      runInNotificationExecutor(() -> myListener.searchFinished(Collections.singletonMap(contributor, hasMore)));
    }
  }

  private static class FullSearchResultsAccumulator extends ResultsAccumulator {

    private final Map<? extends SearchEverywhereContributor<?, ?>, Integer> sectionsLimits;
    private final Map<? extends SearchEverywhereContributor<?, ?>, Condition> conditionsMap;
    private final Map<SearchEverywhereContributor<?, ?>, Boolean> hasMoreMap = new ConcurrentHashMap<>();
    private final Set<SearchEverywhereContributor<?, ?>> finishedContributorsSet = ContainerUtil.newConcurrentSet();
    private final Lock lock = new ReentrantLock();
    private volatile boolean mySearchFinished = false;

    FullSearchResultsAccumulator(Map<? extends SearchEverywhereContributor<?, ?>, Integer> contributorsAndLimits,
                                 SEResultsEqualityProvider equalityProvider, Listener listener, Executor notificationExecutor,
                                 ProgressIndicator progressIndicator) {
      super(contributorsAndLimits.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> new ArrayList<>(entry.getValue()))),
            equalityProvider, listener, notificationExecutor, progressIndicator);
      sectionsLimits = contributorsAndLimits;
      conditionsMap = contributorsAndLimits.keySet().stream().collect(Collectors.toMap(Function.identity(), c -> lock.newCondition()));
    }

    @Override
    public void setContributorHasMore(SearchEverywhereContributor<?, ?> contributor, boolean hasMore) {
      hasMoreMap.put(contributor, hasMore);
    }

    @Override
    public boolean addElement(Object element, SearchEverywhereContributor<?, ?> contributor, int priority, ProgressIndicator indicator) throws InterruptedException {
      SearchEverywhereFoundElementInfo newElementInfo = new SearchEverywhereFoundElementInfo(element, priority, contributor);
      Condition condition = conditionsMap.get(contributor);
      Collection<SearchEverywhereFoundElementInfo> section = sections.get(contributor);
      int limit = sectionsLimits.get(contributor);

      lock.lock();
      try {
        while (section.size() >= limit && !mySearchFinished) {
          indicator.checkCanceled();
          condition.await(100, TimeUnit.MILLISECONDS);
        }

        if (mySearchFinished) {
          return false;
        }

        Map<SEResultsEqualityProvider.SEEqualElementsActionType, Collection<SearchEverywhereFoundElementInfo>> otherElementsMap = getActionsWithOtherElements(newElementInfo);
        if (otherElementsMap.get(REPLACE).isEmpty() && !otherElementsMap.get(SKIP).isEmpty()) {
          LOG.debug(String.format("Element %s for contributor %s was skipped", element.toString(), contributor.getSearchProviderId()));
          return true;
        }

        section.add(newElementInfo);
        runInNotificationExecutor(() -> myListener.elementsAdded(Collections.singletonList(newElementInfo)));

        List<SearchEverywhereFoundElementInfo> toRemove = new ArrayList<>(otherElementsMap.get(REPLACE));
        toRemove.forEach(info -> {
          Collection<SearchEverywhereFoundElementInfo> list = sections.get(info.getContributor());
          Condition listCondition = conditionsMap.get(info.getContributor());
          list.remove(info);
          LOG.debug(String.format("Element %s for contributor %s is removed", info.getElement().toString(), info.getContributor().getSearchProviderId()));
          listCondition.signal();
        });
        runInNotificationExecutor(() -> myListener.elementsRemoved(toRemove));

        if (section.size() >= limit) {
          stopSearchIfNeeded();
        }
        return true;
      }
      finally {
        lock.unlock();
      }
    }

    @Override
    public void contributorFinished(SearchEverywhereContributor<?, ?> contributor) {
      lock.lock();
      try {
        finishedContributorsSet.add(contributor);
        stopSearchIfNeeded();
      }
      finally {
        lock.unlock();
      }
    }

    public void searchFinished() {
      runInNotificationExecutor(() -> myListener.searchFinished(hasMoreMap));
    }

    public void stop() {
      lock.lock();
      try {
        mySearchFinished = true;
        conditionsMap.values().forEach(Condition::signalAll);
      }
      finally {
        lock.unlock();
      }
    }

    /**
     * could be used only when current thread owns {@link #lock}
     */
    private void stopSearchIfNeeded() {
      if (sections.keySet().stream().allMatch(contributor -> isContributorFinished(contributor))) {
        mySearchFinished = true;
        conditionsMap.values().forEach(Condition::signalAll);
      }
    }

    private boolean isContributorFinished(SearchEverywhereContributor<?, ?> contributor) {
      if (finishedContributorsSet.contains(contributor)) {
        return true;
      }

      return sections.get(contributor).size() >= sectionsLimits.get(contributor);
    }
  }

  private static class ProgressIndicatorWithCancelListener extends ProgressIndicatorBase {

    private volatile Runnable cancelCallback = () -> {};

    private void setCancelCallback(Runnable cancelCallback) {
      this.cancelCallback = cancelCallback;
    }

    @Override
    protected void onRunningChange() {
      if (isCanceled()) {
        cancelCallback.run();
      }
    }
  }
}
