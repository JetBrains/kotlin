// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Pair;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.Alarm;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author mikhail.sokolov
 */
public class MultiThreadSearchTest extends BasePlatformTestCase {

  private static final String MORE_ITEM = "...MORE";
  private static final Collection<SEResultsEqualityProvider> ourEqualityProviders = Collections.singleton(new TrivialElementsEqualityProvider());

  public void testMultiThread() {
    Collection<Scenario> scenarios = createMultithreadScenarios();
    SearchResultsCollector collector = new SearchResultsCollector();
    Alarm alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, getTestRootDisposable());
    MultiThreadSearcher searcher = new MultiThreadSearcher(collector, command -> alarm.addRequest(command, 0), ourEqualityProviders);

    scenarios.forEach(scenario -> {
      ProgressIndicator indicator = searcher.search(scenario.contributorsAndLimits, "tst");
      try {
        collector.awaitFinish(1000);
      }
      catch (TimeoutException e) {
        Assert.fail("Search timeout exceeded");
      }
      catch (InterruptedException ignored) {
      }
      finally {
        indicator.cancel();
      }
      scenario.results.forEach((contributorId, results) -> {
        List<String> values = collector.getContributorValues(contributorId);
        Assert.assertEquals(String.format("Scenario '%s'. found elements by contributor %s", scenario.description, contributorId), results, values);
      });
      collector.clear();
    });
  }

  private static Collection<Scenario> createMultithreadScenarios() {
    Collection<Scenario> res = new ArrayList<>();

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    String scenarioName = "Simple without collisions";
    Map<SearchEverywhereContributor<Object>, Integer> contributors = ContainerUtil.newHashMap(
      Pair.create(createTestContributor("test1", 0, "item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", "item1_11", "item1_12", "item1_13", "item1_14", "item1_15"), 12),
      Pair.create(createTestContributor("test2", 0, "item2_1", "item2_2", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", "item2_10", "item2_11", "item2_12"), 10),
      Pair.create(createTestContributor("test3", 0, "item3_1", "item3_2", "item3_3", "item3_4", "item3_5", "item3_6", "item3_7", "item3_8"), 10),
      Pair.create(createTestContributor("test4", 0, "item4_1", "item4_2", "item4_3", "item4_4", "item4_5", "item4_6", "item4_7", "item4_8", "item4_9", "item4_10", "item4_11", "item4_12", "item4_13"), 11),
      Pair.create(createTestContributor("test5", 0), 10),
      Pair.create(createTestContributor("test6", 0, "item6_1", "item6_2", "item6_3", "item6_4", "item6_5", "item6_6", "item6_7", "item6_8", "item6_9", "item6_10", "item6_11", "item6_12", "item6_13"), 10),
      Pair.create(createTestContributor("test7", 0, "item7_1", "item7_2", "item7_3", "item7_4", "item7_5", "item7_6", "item7_7", "item7_8", "item7_9", "item7_10"), 10),
      Pair.create(createTestContributor("test8", 0, "item8_1", "item8_2", "item8_3", "item8_4", "item8_5"), 10),
      Pair.create(createTestContributor("test9", 0, "item9_1", "item9_2", "item9_3", "item9_4", "item9_5"), 3),
      Pair.create(createTestContributor("test10", 0, "item10_1", "item10_2", "item10_3", "item10_4", "item10_5"), 5)
    );
    Map<String, List<String>> results = ContainerUtil.newHashMap(
      Pair.create("test1", Arrays.asList("item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", "item1_11", "item1_12", MORE_ITEM)),
      Pair.create("test2", Arrays.asList("item2_1", "item2_2", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", "item2_10", MORE_ITEM)),
      Pair.create("test3", Arrays.asList("item3_1", "item3_2", "item3_3", "item3_4", "item3_5", "item3_6", "item3_7", "item3_8")),
      Pair.create("test4", Arrays.asList("item4_1", "item4_2", "item4_3", "item4_4", "item4_5", "item4_6", "item4_7", "item4_8", "item4_9", "item4_10", "item4_11", MORE_ITEM)),
      Pair.create("test5", Collections.emptyList()),
      Pair.create("test6", Arrays.asList("item6_1", "item6_2", "item6_3", "item6_4", "item6_5", "item6_6", "item6_7", "item6_8", "item6_9", "item6_10", MORE_ITEM)),
      Pair.create("test7", Arrays.asList("item7_1", "item7_2", "item7_3", "item7_4", "item7_5", "item7_6", "item7_7", "item7_8", "item7_9", "item7_10")),
      Pair.create("test8", Arrays.asList("item8_1", "item8_2", "item8_3", "item8_4", "item8_5")),
      Pair.create("test9", Arrays.asList("item9_1", "item9_2", "item9_3", MORE_ITEM)),
      Pair.create("test10", Arrays.asList("item10_1", "item10_2", "item10_3", "item10_4", "item10_5"))
    );
    res.add(new Scenario(contributors, results, scenarioName));    
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    scenarioName = "Simple without MORE items";
    contributors = ContainerUtil.newHashMap(
      Pair.create(createTestContributor("test1", 0, "item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10"), 10),
      Pair.create(createTestContributor("test2", 0, "item2_1", "item2_2", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", "item2_10", "item2_11", "item2_12"), 20),
      Pair.create(createTestContributor("test3", 0, "item3_1", "item3_2", "item3_3", "item3_4", "item3_5", "item3_6", "item3_7", "item3_8"), 10)
    );
    results = ContainerUtil.newHashMap(
      Pair.create("test1", Arrays.asList("item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10")),
      Pair.create("test2", Arrays.asList("item2_1", "item2_2", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", "item2_10", "item2_11", "item2_12")),
      Pair.create("test3", Arrays.asList("item3_1", "item3_2", "item3_3", "item3_4", "item3_5", "item3_6", "item3_7", "item3_8"))
    );
    res.add(new Scenario(contributors, results, scenarioName));
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    scenarioName = "Empty results";
    contributors = ContainerUtil.newHashMap(
      Pair.create(createTestContributor("test1", 0), 10),
      Pair.create(createTestContributor("test2", 0), 10),
      Pair.create(createTestContributor("test3", 0), 10),
      Pair.create(createTestContributor("test4", 0), 10),
      Pair.create(createTestContributor("test5", 0), 10)
    );
    results = ContainerUtil.newHashMap(
      Pair.create("test1", Collections.emptyList()),
      Pair.create("test2", Collections.emptyList()),
      Pair.create("test3", Collections.emptyList()),
      Pair.create("test4", Collections.emptyList()),
      Pair.create("test5", Collections.emptyList())      
    );
    res.add(new Scenario(contributors, results, scenarioName));    
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    scenarioName = "One contributor";
    contributors = ContainerUtil.newHashMap(
      Pair.create(createTestContributor("test1", 0, "item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", "item1_11", "item1_12", "item1_13", "item1_14", "item1_15"), 10)
    );
    results = ContainerUtil.newHashMap(
      Pair.create("test1", Arrays.asList("item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", MORE_ITEM))
    );
    res.add(new Scenario(contributors, results, scenarioName));
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    scenarioName = "One contributor with no MORE item";
    contributors = ContainerUtil.newHashMap(
      Pair.create(createTestContributor("test1", 0, "item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10"), 10)
    );
    results = ContainerUtil.newHashMap(
      Pair.create("test1", Arrays.asList("item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10"))
    );
    res.add(new Scenario(contributors, results, scenarioName));
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    scenarioName = "One contributor with empty results";
    contributors = ContainerUtil.newHashMap(Pair.create(createTestContributor("test1", 0), 10));
    results = ContainerUtil.newHashMap(Pair.create("test1", Collections.emptyList()));
    res.add(new Scenario(contributors, results, scenarioName));
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    scenarioName = "Collisions scenario";
    contributors = ContainerUtil.newHashMap(
      Pair.create(createTestContributor("test1", 0, "item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", "item1_11", "item1_12", "item1_13", "item1_14", "item1_15"), 12),
      Pair.create(createTestContributor("test2", 10, "item2_1", "item2_2", "duplicateItem1", "duplicateItem2", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", "item2_10", "item2_11", "item2_12"), 10),
      Pair.create(createTestContributor("test3", 8, "item3_1", "item3_2", "item3_3", "item3_4", "duplicateItem1", "item3_5", "item3_6", "item3_7", "item3_8", "duplicateItem2", "duplicateItem3"), 10),
      Pair.create(createTestContributor("test4", 15, "item4_1", "item4_2", "duplicateItem2", "duplicateItem3", "item4_3", "item4_4", "item4_5", "item4_6", "item4_7", "item4_8", "item4_9"), 10),
      Pair.create(createTestContributor("test6", 20, "item6_1", "item6_2", "item6_3", "item6_4", "duplicateItem3", "item6_5", "item6_6", "item6_7", "item6_8", "item6_9", "item6_10", "item6_11", "duplicateItem4", "item6_12", "item6_13"), 10),
      Pair.create(createTestContributor("test7", 5, "item7_1", "item7_2", "duplicateItem3", "item7_3", "item7_4", "item7_5", "item7_6", "item7_7", "item7_8", "item7_9", "item7_10"), 10),
      Pair.create(createTestContributor("test8", 10, "item8_1", "item8_2", "item8_3", "item8_4", "item8_5", "duplicateItem4"), 10),
      Pair.create(createTestContributor("test9", 15, "item9_1", "item9_2", "item9_3", "item9_4", "duplicateItem5", "duplicateItem6"), 5),
      Pair.create(createTestContributor("test10", 10, "item10_1", "item10_2", "item10_3", "item10_4", "duplicateItem5", "duplicateItem6"), 5)
    );
    results = ContainerUtil.newHashMap(
      Pair.create("test1", Arrays.asList("item1_1", "item1_2", "item1_3", "item1_4", "item1_5", "item1_6", "item1_7", "item1_8", "item1_9", "item1_10", "item1_11", "item1_12", MORE_ITEM)),
      Pair.create("test2", Arrays.asList("item2_1", "item2_2", "duplicateItem1", "item2_3", "item2_4", "item2_5", "item2_6", "item2_7", "item2_8", "item2_9", MORE_ITEM)),
      Pair.create("test3", Arrays.asList("item3_1", "item3_2", "item3_3", "item3_4", "item3_5", "item3_6", "item3_7", "item3_8")),
      Pair.create("test4", Arrays.asList("item4_1", "item4_2", "duplicateItem2", "item4_3", "item4_4", "item4_5", "item4_6", "item4_7", "item4_8", "item4_9")),
      Pair.create("test6", Arrays.asList("item6_1", "item6_2", "item6_3", "item6_4", "duplicateItem3", "item6_5", "item6_6", "item6_7", "item6_8", "item6_9", MORE_ITEM)),
      Pair.create("test7", Arrays.asList("item7_1", "item7_2", "item7_3", "item7_4", "item7_5", "item7_6", "item7_7", "item7_8", "item7_9", "item7_10")),
      Pair.create("test8", Arrays.asList("item8_1", "item8_2", "item8_3", "item8_4", "item8_5", "duplicateItem4")),
      Pair.create("test9", Arrays.asList("item9_1", "item9_2", "item9_3", "item9_4", "duplicateItem5", MORE_ITEM)),
      Pair.create("test10", Arrays.asList("item10_1", "item10_2", "item10_3", "item10_4", "duplicateItem6"))
      );
    res.add(new Scenario(contributors, results, scenarioName));
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    scenarioName = "Only collisions";
    contributors = ContainerUtil.newHashMap(
      Pair.create(createTestContributor("test1", 10, "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12"), 5),
      Pair.create(createTestContributor("test2", 9, "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12"), 5),
      Pair.create(createTestContributor("test3", 8, "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12"), 5),
      Pair.create(createTestContributor("test4", 7, "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12"), 5),
      Pair.create(createTestContributor("test5", 6, "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12"), 5)
    );
    results = ContainerUtil.newHashMap(
      Pair.create("test1", Arrays.asList("item1", "item2", "item3", "item4", "item5", MORE_ITEM)),
      Pair.create("test2", Arrays.asList("item6", "item7", "item8", "item9", "item10", MORE_ITEM)),
      Pair.create("test3", Arrays.asList("item11", "item12")),
      Pair.create("test4", Collections.emptyList()),
      Pair.create("test5", Collections.emptyList())
    );
    res.add(new Scenario(contributors, results, scenarioName));
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    
    return res;
  }

  private static SearchEverywhereContributor<Object> createTestContributor(String id, int fixedPriority, String... items) {
    return new SearchEverywhereContributor<Object>() {
      @NotNull
      @Override
      public String getSearchProviderId() {
        return id;
      }

      @NotNull
      @Override
      public String getGroupName() {
        return id;
      }

      @Override
      public int getSortWeight() {
        return 0;
      }

      @Override
      public boolean showInFindResults() {
        return false;
      }

      @Override
      public int getElementPriority(@NotNull Object element, @NotNull String searchPattern) {
        return fixedPriority;
      }

      @Override
      public void fetchElements(@NotNull String pattern,
                                @NotNull ProgressIndicator progressIndicator,
                                @NotNull Processor<? super Object> consumer) {
        boolean flag = true;
        Iterator<String> iterator = Arrays.asList(items).iterator();
        while (flag && iterator.hasNext()) {
          String next = iterator.next();
          flag = consumer.process(next);
        }
      }

      @Override
      public boolean processSelectedItem(@NotNull Object selected, int modifiers, @NotNull String searchText) {
        return false;
      }

      @NotNull
      @Override
      public ListCellRenderer<? super Object> getElementsRenderer() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Object getDataForItem(@NotNull Object element, @NotNull String dataId) {
        return null;
      }
    };
  }

  private static class Scenario {
    private final Map<SearchEverywhereContributor<Object>, Integer> contributorsAndLimits;
    private final Map<String, List<String>> results;
    private final String description;

    Scenario(Map<SearchEverywhereContributor<Object>, Integer> contributorsAndLimits,
                    Map<String, List<String>> results, String description) {
      this.contributorsAndLimits = contributorsAndLimits;
      this.results = results;
      this.description = description;
    }
  }

  private static class SearchResultsCollector implements MultiThreadSearcher.Listener {

    private final Map<String, List<String>> myMap = new ConcurrentHashMap<>();
    private final AtomicBoolean myFinished = new AtomicBoolean(false);
    private final Phaser myPhaser = new Phaser(2);

    public List<String> getContributorValues(String contributorId) {
      List<String> values = myMap.get(contributorId);
      return values != null ? values : Collections.emptyList();
    }

    public void awaitFinish(long timeout) throws TimeoutException, InterruptedException {
      int phase = myPhaser.arrive();
      myPhaser.awaitAdvanceInterruptibly(phase, timeout, TimeUnit.MILLISECONDS);
    }

    public void clear() {
      myMap.clear();
      myFinished.set(false);
    }

    @Override
    public void elementsAdded(@NotNull List<? extends SearchEverywhereFoundElementInfo> added) {
      added.forEach(info -> {
        List<String> list = myMap.computeIfAbsent(info.getContributor().getSearchProviderId(), s -> new ArrayList<>());
        list.add((String) info.getElement());
      });
    }

    @Override
    public void elementsRemoved(@NotNull List<? extends SearchEverywhereFoundElementInfo> removed) {
      removed.forEach(info -> {
        List<String> list = myMap.get(info.getContributor().getSearchProviderId());
        Assert.assertNotNull("Trying to remove object, that wasn't added", list);
        list.remove(info.getElement());
      });
    }

    @Override
    public void searchFinished(@NotNull Map<SearchEverywhereContributor<?>, Boolean> hasMoreContributors) {
      hasMoreContributors.entrySet()
        .stream()
        .filter(entry -> entry.getValue())
        .map(entry -> entry.getKey())
        .forEach(contributor -> {
          List<String> list = myMap.get(contributor.getSearchProviderId());
          Assert.assertNotNull("If section has MORE item it cannot be empty", list);
          list.add(MORE_ITEM);
        });

      boolean set = myFinished.compareAndSet(false, true);
      Assert.assertTrue("More than one finish event", set);

      myPhaser.arrive();
    }
  }
}
