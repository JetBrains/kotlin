// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup;

import com.intellij.openapi.util.Pair;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FlatteningIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * @author peter
 */
public abstract class ComparingClassifier<T> extends Classifier<T> {
  private final boolean myNegated;

  protected ComparingClassifier(Classifier<T> next, String name, boolean negated) {
    super(next, name);
    myNegated = negated;
  }

  @Nullable
  public abstract Comparable getWeight(T t, ProcessingContext context);

  @NotNull
  @Override
  public Iterable<T> classify(@NotNull final Iterable<T> source, @NotNull final ProcessingContext context) {
    List<T> nulls = null;
    TreeMap<Comparable, List<T>> map = new TreeMap<>();
    for (T t : source) {
      final Comparable weight = getWeight(t, context);
      if (weight == null) {
        if (nulls == null) nulls = new SmartList<>();
        nulls.add(t);
      } else {
        List<T> list = map.get(weight);
        if (list == null) {
          map.put(weight, list = new SmartList<>());
        }
        list.add(t);
      }
    }

    final List<List<T>> values = new ArrayList<>(myNegated ? map.descendingMap().values() : map.values());
    ContainerUtil.addIfNotNull(values, nulls);

    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new FlatteningIterator<List<T>, T>(values.iterator()) {
          @Override
          protected Iterator<T> createValueIterator(List<T> group) {
            return myNext.classify(group, context).iterator();
          }
        };
      }
    };
  }

  @NotNull
  @Override
  public List<Pair<T, Object>> getSortingWeights(@NotNull Iterable<T> items, @NotNull final ProcessingContext context) {
    return ContainerUtil.map(items, t -> new Pair<>(t, getWeight(t, context)));
  }
}
