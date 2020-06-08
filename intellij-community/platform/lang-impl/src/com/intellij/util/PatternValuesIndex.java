// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util;

import com.intellij.patterns.*;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author Gregory.Shrago
 */
public final class PatternValuesIndex {

  public static Set<String> buildStringIndex(Collection<? extends ElementPattern<?>> patterns) {
    final THashSet<String> result = new THashSet<>();
    processStringValues(patterns, (elementPattern, value) -> {
      for (Object o : value) {
        if (o instanceof String) {
          result.add((String)o);
        }
      }
      return true;
    });
    return result;
  }

  public static boolean processStringValues(Collection<? extends ElementPattern<?>> patterns, final PairProcessor<? super ElementPattern<?>, ? super Collection<Object>> valueProcessor) {
    final LinkedList<ElementPattern<?>> stack = new LinkedList<>();
    for (final ElementPattern<?> next : patterns) {
      stack.add(next);
      while (!stack.isEmpty()) {
        final ElementPattern<?> pattern = stack.removeFirst();
        final ElementPatternCondition<?> patternCondition = pattern.getCondition();
        final InitialPatternCondition<?> initialCondition = patternCondition.getInitialCondition();
        if (initialCondition instanceof InitialPatternConditionPlus) {
          ContainerUtil.addAllNotNull(stack, ((InitialPatternConditionPlus<?>)initialCondition).getPatterns());
        }
        for (PatternCondition<?> condition : patternCondition.getConditions()) {
          if (condition instanceof PatternConditionPlus) {
            stack.add(((PatternConditionPlus)condition).getValuePattern());
          }
          else if (condition instanceof ValuePatternCondition) {
            Collection<Object> values = ((ValuePatternCondition)condition).getValues();
            if (!valueProcessor.process(next, values)) return false;
          }
        }
      }
    }
    return true;
  }
}
