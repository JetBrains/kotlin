// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.Classifier;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.statistics.StatisticsInfo;
import com.intellij.psi.statistics.StatisticsManager;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.MultiMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author peter
*/
public final class StatisticsWeigher extends CompletionWeigher {
  private static final Logger LOG = Logger.getInstance(LookupStatisticsWeigher.class);
  private static final Key<StatisticsInfo> BASE_STATISTICS_INFO = Key.create("Base statistics info");

  @Override
  public Comparable weigh(@NotNull final LookupElement item, @NotNull final CompletionLocation location) {
    throw new UnsupportedOperationException();
  }

  public static class LookupStatisticsWeigher extends Classifier<LookupElement> {
    private final CompletionLocation myLocation;
    private final Map<LookupElement, StatisticsComparable> myWeights = new IdentityHashMap<>();
    private final Set<String> myStringsWithWeights = new ObjectOpenHashSet<>();
    private final Set<LookupElement> myNoStats = new ReferenceOpenHashSet<>();

    public LookupStatisticsWeigher(CompletionLocation location, Classifier<LookupElement> next) {
      super(next, "stats");
      myLocation = location;
    }

    @Override
    public void addElement(@NotNull LookupElement element, @NotNull ProcessingContext context) {
      StatisticsInfo baseInfo = getBaseStatisticsInfo(element, myLocation);
      int weight = weigh(baseInfo);
      if (weight != 0) {
        myWeights.put(element, new StatisticsComparable(weight, baseInfo));
        myStringsWithWeights.add(element.getLookupString());
      }
      if (baseInfo == StatisticsInfo.EMPTY) {
        myNoStats.add(element);
      }
      super.addElement(element, context);
    }

    @NotNull
    @Override
    public Iterable<LookupElement> classify(@NotNull Iterable<? extends LookupElement> source, @NotNull final ProcessingContext context) {
      List<LookupElement> initialList = getInitialNoStatElements(source, context);
      Iterable<LookupElement> rest = withoutInitial(source, initialList);
      Collection<List<LookupElement>> byWeight = buildMapByWeight(rest).descendingMap().values();

      return JBIterable.from(initialList).append(JBIterable.from(byWeight).flatten(group -> myNext.classify(group, context)));
    }

    private static Iterable<LookupElement> withoutInitial(Iterable<? extends LookupElement> allItems, List<? extends LookupElement> initial) {
      Set<LookupElement> initialSet = new ReferenceOpenHashSet<>(initial);
      return JBIterable.<LookupElement>from(allItems).filter(element -> !initialSet.contains(element));
    }

    private List<LookupElement> getInitialNoStatElements(Iterable<? extends LookupElement> source, ProcessingContext context) {
      List<LookupElement> initialList = new ArrayList<>();
      for (LookupElement next : myNext.classify(source, context)) {
        if (myNoStats.contains(next)) {
          initialList.add(next);
        }
        else {
          break;
        }
      }
      return initialList;
    }

    private TreeMap<Integer, List<LookupElement>> buildMapByWeight(Iterable<? extends LookupElement> source) {
      MultiMap<String, LookupElement> byName = MultiMap.create();
      List<LookupElement> noStats = new ArrayList<>();
      for (LookupElement element : source) {
        String string = element.getLookupString();
        if (myStringsWithWeights.contains(string)) {
          byName.putValue(string, element);
        } else {
          noStats.add(element);
        }
      }

      TreeMap<Integer, List<LookupElement>> map = new TreeMap<>();
      map.put(0, noStats);
      for (String s : byName.keySet()) {
        List<LookupElement> group = (List<LookupElement>)byName.get(s);
        group.sort(Comparator.comparing(this::getScalarWeight).reversed());
        map.computeIfAbsent(getMaxWeight(group), __ -> new ArrayList<>()).addAll(group);
      }
      return map;
    }

    private int getMaxWeight(List<? extends LookupElement> group) {
      int max = 0;
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < group.size(); i++) {
        max = Math.max(max, getScalarWeight(group.get(i)));
      }
      return max;
    }

    private int getScalarWeight(LookupElement e) {
      StatisticsComparable comparable = myWeights.get(e);
      return comparable == null ? 0 : comparable.getScalar();
    }

    private StatisticsComparable getWeight(LookupElement t) {
      StatisticsComparable w = myWeights.get(t);
      if (w == null) {
        StatisticsInfo info = getBaseStatisticsInfo(t, myLocation);
        myWeights.put(t, w = new StatisticsComparable(weigh(info), info));
      }
      return w;
    }

    private static int weigh(final StatisticsInfo baseInfo) {
      if (baseInfo == StatisticsInfo.EMPTY) {
        return 0;
      }
      int minRecency = StatisticsManager.getInstance().getLastUseRecency(baseInfo);
      return minRecency == Integer.MAX_VALUE ? 0 : StatisticsManager.RECENCY_OBLIVION_THRESHOLD - minRecency;
    }

    @NotNull
    @Override
    public List<Pair<LookupElement, Object>> getSortingWeights(@NotNull Iterable<? extends LookupElement> items, @NotNull final ProcessingContext context) {
      return ContainerUtil.map(items, lookupElement -> new Pair<>(lookupElement, getWeight(lookupElement)));
    }

    @Override
    public void removeElement(@NotNull LookupElement element, @NotNull ProcessingContext context) {
      myWeights.remove(element);
      myNoStats.remove(element);
      super.removeElement(element, context);
    }
  }

  public static void clearBaseStatisticsInfo(LookupElement item) {
    item.putUserData(BASE_STATISTICS_INFO, null);
  }

  @NotNull
  public static StatisticsInfo getBaseStatisticsInfo(LookupElement item, @Nullable CompletionLocation location) {
    StatisticsInfo info = BASE_STATISTICS_INFO.get(item);
    if (info == null) {
      if (location == null) {
        return StatisticsInfo.EMPTY;
      }
      BASE_STATISTICS_INFO.set(item, info = calcBaseInfo(item, location));
    }
    return info;
  }

  @NotNull
  private static StatisticsInfo calcBaseInfo(LookupElement item, @NotNull CompletionLocation location) {
    if (!ApplicationManager.getApplication().isUnitTestMode() && !location.getCompletionParameters().isTestingMode()) {
      LOG.assertTrue(!ApplicationManager.getApplication().isDispatchThread());
    }
    StatisticsInfo info = StatisticsManager.serialize(CompletionService.STATISTICS_KEY, item, location);
    return info == null ? StatisticsInfo.EMPTY : info;
  }

}
