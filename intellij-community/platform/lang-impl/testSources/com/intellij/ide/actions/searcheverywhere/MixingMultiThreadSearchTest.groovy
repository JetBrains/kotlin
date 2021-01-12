// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere

import com.intellij.openapi.application.Experiments
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.Alarm
import com.intellij.util.Processor
import org.jetbrains.annotations.NotNull
import org.junit.Assert

import javax.swing.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Phaser
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class MixingMultiThreadSearchTest extends BasePlatformTestCase {

  private static final String MORE_ITEM = "...MORE"
  private static final Collection<SEResultsEqualityProvider> ourEqualityProviders = Collections.singleton(new TrivialElementsEqualityProvider())

  @Override
  protected void setUp() throws Exception {
    super.setUp()
    Experiments.getInstance().setFeatureEnabled("search.everywhere.mixed.results", true)
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown()
    Experiments.getInstance().setFeatureEnabled("search.everywhere.mixed.results", false)
  }

  void "test simple without collisions"() {
    def contributors = [
      (createTestContributor("test1", 0, "item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", "item1_11", "item1_12", "item1_13", "item1_14", "item1_15")) : 12,
      (createTestContributor("test2", 0, "item2_1", "item2_2", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", "item2_10", "item2_11", "item2_12")) : 10,
      (createTestContributor("test3", 0, "item3_1", "item3_2", "item3_3", "item3_4", "item3_5", "item3_6", "item3_7", "item3_8")) : 10,
      (createTestContributor("test4", 0, "item4_1", "item4_2", "item4_3", "item4_4", "item4_5", "item4_6", "item4_7", "item4_8", "item4_9", "item4_10", "item4_11", "item4_12", "item4_13")) : 11,
      (createTestContributor("test5", 0)) : 10,
      (createTestContributor("test6", 0, "item6_1", "item6_2", "item6_3", "item6_4", "item6_5", "item6_6", "item6_7", "item6_8", "item6_9", "item6_10", "item6_11", "item6_12", "item6_13")) : 10,
      (createTestContributor("test7", 0, "item7_1", "item7_2", "item7_3", "item7_4", "item7_5", "item7_6", "item7_7", "item7_8", "item7_9", "item7_10")) : 10,
      (createTestContributor("test8", 0, "item8_1", "item8_2", "item8_3", "item8_4", "item8_5")) : 10,
      (createTestContributor("test9", 0, "item9_1", "item9_2", "item9_3", "item9_4", "item9_5")) : 3,
      (createTestContributor("test10", 0, "item10_1", "item10_2", "item10_3", "item10_4", "item10_5")) : 5
    ]
    def results = [
      "test1" : ["item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", "item1_11", "item1_12", MORE_ITEM],
      "test2" : ["item2_1", "item2_2", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", "item2_10", MORE_ITEM],
      "test3" : ["item3_1", "item3_2", "item3_3", "item3_4", "item3_5", "item3_6", "item3_7", "item3_8"],
      "test4" : ["item4_1", "item4_2", "item4_3", "item4_4", "item4_5", "item4_6", "item4_7", "item4_8", "item4_9", "item4_10", "item4_11", MORE_ITEM],
      "test5" : [],
      "test6" : ["item6_1", "item6_2", "item6_3", "item6_4", "item6_5", "item6_6", "item6_7", "item6_8", "item6_9", "item6_10", MORE_ITEM],
      "test7" : ["item7_1", "item7_2", "item7_3", "item7_4", "item7_5", "item7_6", "item7_7", "item7_8", "item7_9", "item7_10"],
      "test8" : ["item8_1", "item8_2", "item8_3", "item8_4", "item8_5"],
      "test9" : ["item9_1", "item9_2", "item9_3", MORE_ITEM],
      "test10" : ["item10_1", "item10_2", "item10_3", "item10_4", "item10_5"]
    ]

    testScenario(new Scenario(contributors, results))
  }

  void "test simple without MORE items"() {
    def contributors = [
      (createTestContributor("test1", 0, "item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10")) : 10,
      (createTestContributor("test2", 0, "item2_1", "item2_2", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", "item2_10", "item2_11", "item2_12")) : 20,
      (createTestContributor("test3", 0, "item3_1", "item3_2", "item3_3", "item3_4", "item3_5", "item3_6", "item3_7", "item3_8")) : 10
    ]
    def results = [
      "test1" : ["item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10"],
      "test2" : ["item2_1", "item2_2", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", "item2_10", "item2_11", "item2_12"],
      "test3" : ["item3_1", "item3_2", "item3_3", "item3_4", "item3_5", "item3_6", "item3_7", "item3_8"]
    ]

    testScenario(new Scenario(contributors, results))
  }

  void "test empty results"() {
    def contributors = [
      (createTestContributor("test1", 0)) : 10,
      (createTestContributor("test2", 0)) : 10,
      (createTestContributor("test3", 0)) : 10,
      (createTestContributor("test4", 0)) : 10,
      (createTestContributor("test5", 0)) : 10
    ]
    def results = [
      "test1" : [],
      "test2" : [],
      "test3" : [],
      "test4" : [],
      "test5" : []
    ]

    testScenario(new Scenario(contributors, results))
  }

  void "test one contributor"() {
    testScenario(new Scenario(
      [(createTestContributor("test1", 0, "item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", "item1_11", "item1_12", "item1_13", "item1_14", "item1_15")) : 10],
      ["test1": ["item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", MORE_ITEM]])
    )
  }

  void "test one contributor with no MORE item"() {
    testScenario(new Scenario(
      [(createTestContributor("test1", 0, "item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10")) : 10],
      ["test1": ["item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10"]])
    )
  }

  void "test one contributor with empty results"() {
    testScenario(new Scenario(
      [(createTestContributor("test1", 0)) : 10],
      ["test1":[]])
    )
  }

  void "test collisions scenario"() {
    def contributors = [
      (createTestContributor("test1", 0, "item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", "item1_11", "item1_12", "item1_13", "item1_14", "item1_15")) : 12,
      (createTestContributor("test2", 10, "item2_1", "item2_2", "duplicateItem1", "duplicateItem2", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", "item2_10", "item2_11", "item2_12")) : 10,
      (createTestContributor("test3", 8, "item3_1", "item3_2", "item3_3", "item3_4", "duplicateItem1", "item3_5", "item3_6", "item3_7", "item3_8", "duplicateItem2", "duplicateItem3")) : 10,
      (createTestContributor("test4", 15, "item4_1", "item4_2", "duplicateItem2", "duplicateItem3", "item4_3", "item4_4", "item4_5", "item4_6", "item4_7", "item4_8", "item4_9")) : 10,
      (createTestContributor("test6", 20, "item6_1", "item6_2", "item6_3", "item6_4", "duplicateItem3", "item6_5", "item6_6", "item6_7", "item6_8", "item6_9", "item6_10", "item6_11", "duplicateItem4", "item6_12", "item6_13")) : 10,
      (createTestContributor("test7", 5, "item7_1", "item7_2", "duplicateItem3", "item7_3", "item7_4", "item7_5", "item7_6", "item7_7", "item7_8", "item7_9", "item7_10")) : 10,
      (createTestContributor("test8", 10, "item8_1", "item8_2", "item8_3", "item8_4", "item8_5", "duplicateItem4")) : 10,
      (createTestContributor("test9", 15, "item9_1", "item9_2", "item9_3", "item9_4", "duplicateItem5", "duplicateItem6")) : 5,
      (createTestContributor("test10", 10, "item10_1", "item10_2", "item10_3", "item10_4", "duplicateItem5", "duplicateItem6")) : 5
    ]
    def results = [
      "test1": ["item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", "item1_11", "item1_12", MORE_ITEM],
      "test2": ["item2_1", "item2_2", "duplicateItem1", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", MORE_ITEM],
      "test3": ["item3_1", "item3_2", "item3_3", "item3_4", "item3_5", "item3_6", "item3_7", "item3_8"],
      "test4": ["item4_1", "item4_2", "duplicateItem2", "item4_3", "item4_4", "item4_5", "item4_6", "item4_7", "item4_8", "item4_9"],
      "test6": ["item6_1", "item6_2", "item6_3", "item6_4", "duplicateItem3", "item6_5", "item6_6", "item6_7", "item6_8", "item6_9", MORE_ITEM],
      "test7": ["item7_1", "item7_2", "item7_3", "item7_4", "item7_5", "item7_6", "item7_7", "item7_8", "item7_9", "item7_10"],
      "test8": ["item8_1", "item8_2", "item8_3", "item8_4", "item8_5", "duplicateItem4"],
      "test9": ["item9_1", "item9_2", "item9_3", "item9_4", "duplicateItem5", MORE_ITEM],
      "test10": ["item10_1", "item10_2", "item10_3", "item10_4", "duplicateItem6"]
    ]
    testScenario(new Scenario(contributors, results))
  }

  void "test only collisions"() {
    def contributors = [
      (createTestContributor("test1", 10, "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12")) : 5,
      (createTestContributor("test2", 9, "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12")) : 5,
      (createTestContributor("test3", 8, "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12")) : 5,
      (createTestContributor("test4", 7, "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12")) : 5,
      (createTestContributor("test5", 6, "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12")) : 5
    ]
    def results = [
      "test1" : ["item1", "item2", "item3", "item4", "item5", MORE_ITEM],
      "test2" : ["item6", "item7", "item8", "item9", "item10", MORE_ITEM],
      "test3" : ["item11", "item12"],
      "test4" : [],
      "test5" : []
    ]
    testScenario(new Scenario(contributors, results))
  }

  private void testScenario(Scenario scenario) {
    SearchResultsCollector collector = new SearchResultsCollector()
    Alarm alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, getTestRootDisposable())
    MultiThreadSearcher searcher = new MultiThreadSearcher(collector, { command -> alarm.addRequest(command, 0) }, ourEqualityProviders)
    ProgressIndicator indicator = searcher.search(scenario.contributorsAndLimits, "tst")

    try {
      collector.awaitFinish(1000)
    }
    catch (TimeoutException ignored) {
      Assert.fail("Search timeout exceeded")
    }
    catch (InterruptedException ignored) {
    }
    finally {
      indicator.cancel()
    }
    scenario.results.forEach({ contributorId, results ->
      List<String> values = collector.getContributorValues(contributorId)
      Assert.assertEquals(results, values)
    })
    collector.clear()
  }

  private static SearchEverywhereContributor<Object> createTestContributor(String id, int fixedPriority, String... items) {
    return new SearchEverywhereContributor<Object>() {
      @NotNull
      @Override
      String getSearchProviderId() {
        return id
      }

      @NotNull
      @Override
      String getGroupName() {
        return id
      }

      @Override
      int getSortWeight() {
        return 0
      }

      @Override
      boolean showInFindResults() {
        return false
      }

      @Override
      int getElementPriority(@NotNull Object element, @NotNull String searchPattern) {
        return fixedPriority
      }

      @Override
      void fetchElements(@NotNull String pattern,
                         @NotNull ProgressIndicator progressIndicator,
                         @NotNull Processor<? super Object> consumer) {
        boolean flag = true
        Iterator<String> iterator = Arrays.asList(items).iterator()
        while (flag && iterator.hasNext()) {
          String next = iterator.next()
          flag = consumer.process(next)
        }
      }

      @Override
      boolean processSelectedItem(@NotNull Object selected, int modifiers, @NotNull String searchText) {
        return false
      }

      @NotNull
      @Override
      ListCellRenderer<? super Object> getElementsRenderer() {
        throw new UnsupportedOperationException()
      }

      @Override
      Object getDataForItem(@NotNull Object element, @NotNull String dataId) {
        return null
      }
    }
  }

  static class Scenario {
    private final Map<SearchEverywhereContributor<Object>, Integer> contributorsAndLimits
    private final Map<String, List<String>> results

    Scenario(Map<SearchEverywhereContributor<Object>, Integer> contributorsAndLimits, Map<String, List<String>> results) {
      this.contributorsAndLimits = contributorsAndLimits
      this.results = results
    }
  }

  static class SearchResultsCollector implements SESearcher.Listener {

    private final Map<String, List<String>> myMap = new ConcurrentHashMap<>()
    private final AtomicBoolean myFinished = new AtomicBoolean(false)
    private final Phaser myPhaser = new Phaser(2)

    List<String> getContributorValues(String contributorId) {
      List<String> values = myMap.get(contributorId)
      return values != null ? values : []
    }

    void awaitFinish(long timeout) throws TimeoutException, InterruptedException {
      int phase = myPhaser.arrive()
      myPhaser.awaitAdvanceInterruptibly(phase, timeout, TimeUnit.MILLISECONDS)
    }

    void clear() {
      myMap.clear()
      myFinished.set(false)
    }

    @Override
    void elementsAdded(@NotNull List<? extends SearchEverywhereFoundElementInfo> added) {
      added.forEach({ info ->
        List<String> list = myMap.computeIfAbsent(info.getContributor().getSearchProviderId(), { s -> new ArrayList<>() })
        list.add((String)info.getElement())
      })
    }

    @Override
    void elementsRemoved(@NotNull List<? extends SearchEverywhereFoundElementInfo> removed) {
      removed.forEach({ info ->
        List<String> list = myMap.get(info.getContributor().getSearchProviderId())
        Assert.assertNotNull("Trying to remove object, that wasn't added", list)
        list.remove(info.getElement())
      })
    }

    @Override
    void searchFinished(@NotNull Map<SearchEverywhereContributor<?>, Boolean> hasMoreContributors) {
      hasMoreContributors.entrySet()
        .stream()
        .filter({ entry -> entry.getValue() })
        .map({ entry -> entry.getKey() })
        .forEach({ contributor ->
          List<String> list = myMap.get(contributor.getSearchProviderId())
          Assert.assertNotNull("If section has MORE item it cannot be empty", list)
          list.add(MORE_ITEM)
        })

      boolean set = myFinished.compareAndSet(false, true)
      Assert.assertTrue("More than one finish event", set)

      myPhaser.arrive()
    }
  }
}
