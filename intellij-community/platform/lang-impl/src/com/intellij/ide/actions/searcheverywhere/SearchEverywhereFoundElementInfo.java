// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

/**
 * Class containing info about found elements
 */
public class SearchEverywhereFoundElementInfo {
  public final int priority;
  public final Object element;
  public final SearchEverywhereContributor<?> contributor;

  public SearchEverywhereFoundElementInfo(Object element, int priority, SearchEverywhereContributor<?> contributor) {
    this.priority = priority;
    this.element = element;
    this.contributor = contributor;
  }

  public int getPriority() {
    return priority;
  }

  public Object getElement() {
    return element;
  }

  public SearchEverywhereContributor<?> getContributor() {
    return contributor;
  }
}
