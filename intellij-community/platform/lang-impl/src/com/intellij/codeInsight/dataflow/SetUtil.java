package com.intellij.codeInsight.dataflow;

import java.util.HashSet;
import java.util.Set;

/**
 * @author: oleg
 */
public class SetUtil {
  private SetUtil() {
  }

  /**
   * Intersects two sets
   */
  public static <T> Set<T> intersect(final Set<T> set1, final Set<T> set2) {
    if (set1.equals(set2)){
      return set1;
    }
    final HashSet<T> result = new HashSet<>();
    Set<T> minSet;
    Set<T> otherSet;
    if (set1.size() < set2.size()){
      minSet = set1;
      otherSet = set2;
    } else {
      minSet = set2;
      otherSet = set1;
    }
    for (T s : minSet) {
      if (otherSet.contains(s)){
        result.add(s);
      }
    }
    return result;
  }
}
