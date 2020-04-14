// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages

import com.intellij.model.Pointer
import com.intellij.navigation.TargetPopupPresentation

/**
 * Represents the search implementation (the usage handler and the text search strings)
 * plus data needed to display it in the UI.
 */
interface SearchTarget {

  /**
   * @return smart pointer used to restore the [SearchTarget] instance in the subsequent read actions
   */
  fun createPointer(): Pointer<out SearchTarget>

  /**
   * @return presentation to be displayed in the disambiguation popup
   * when several [different][equals] targets exist to choose from,
   * or in the Usage View (only [icon][TargetPopupPresentation.getIcon]
   * and [presentable text][TargetPopupPresentation.getPresentableText] are used)
   */
  val presentation: TargetPopupPresentation

  val usageHandler: UsageHandler<*>

  /**
   * Text doesn't contain references by design (e.g. plain text or markdown),
   * but there might exist occurrences which are feasible to find/rename,
   * e.g fully qualified name of a Java class or package.
   *
   * Returning non-empty collection will enable "Search for text occurrences" checkbox in the UI.
   *
   * @return collection of strings to search for text occurrences
   * @see SymbolTextSearcher
   */
  val textSearchStrings: Collection<String>

  /**
   * Several symbols might have the same usage target;
   * the equals/hashCode are used to remove the same targets from the list.
   */
  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int
}
