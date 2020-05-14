/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.formatting;

import com.intellij.formatting.engine.BlockRangesMap;
import org.jetbrains.annotations.NonNls;

/**
 * Extends {@link Spacing} in order to keep number of additional settings like {@code 'minSpaces'}, {@code 'minLineFeeds'},
 * {@code 'prefLineFeeds'} etc.
 */
public class SpacingImpl extends Spacing {
  private int myMinSpaces;
  private int myKeepBlankLines;
  private int myMaxSpaces;
  private int myMinLineFeeds;
  private int myPrefLineFeeds = 0;
  protected int myFlags;

  private static final int READ_ONLY_MASK = 1;
  private static final int SAFE_MASK = 2;
  private static final int SHOULD_KEEP_LINE_BREAKS_MASK = 4;
  private static final int SHOULD_KEEP_FIRST_COLUMN_MASK = 8;

  public SpacingImpl(final int minSpaces,
                     final int maxSpaces,
                     final int minLineFeeds,
                     final boolean isReadOnly,
                     final boolean safe,
                     final boolean shouldKeepLineBreaks,
                     final int keepBlankLines,
                     final boolean keepFirstColumn,
                     final int prefLineFeeds) {
    init(minSpaces, maxSpaces, minLineFeeds, isReadOnly, safe, shouldKeepLineBreaks, keepBlankLines, keepFirstColumn, prefLineFeeds);
  }

  void init(final int minSpaces, final int maxSpaces, final int minLineFeeds, final boolean isReadOnly, final boolean safe,
            final boolean shouldKeepLineBreaks, final int keepBlankLines, final boolean keepFirstColumn, final int prefLineFeeds)
  {
    myMinSpaces = minSpaces;

    myMaxSpaces = Math.max(minSpaces, maxSpaces);
    myMinLineFeeds = minLineFeeds;
    myPrefLineFeeds = prefLineFeeds;
    if (minLineFeeds > 1 && (minLineFeeds - 1) > keepBlankLines) {
      myKeepBlankLines = minLineFeeds - 1;
    } else {
      myKeepBlankLines = keepBlankLines;
    }
    myFlags = (isReadOnly ? READ_ONLY_MASK:0) | (safe ? SAFE_MASK:0) | (shouldKeepLineBreaks ? SHOULD_KEEP_LINE_BREAKS_MASK :0) |
      (keepFirstColumn ? SHOULD_KEEP_FIRST_COLUMN_MASK:0);
  }

  public int getMinSpaces() {
    return myMinSpaces;
  }

  public int getMaxSpaces() {
    return myMaxSpaces;
  }

  public int getMinLineFeeds() {
    return myMinLineFeeds;
  }

  public final boolean isReadOnly(){
    return (myFlags & READ_ONLY_MASK) != 0;
  }

  final boolean containsLineFeeds() {
    return myMinLineFeeds > 0;
  }

  public final boolean isSafe() {
    return (myFlags & SAFE_MASK) != 0;
  }

  /**
   * Allows to ask to refresh current state using given formatter if necessary.
   */
  public void refresh(BlockRangesMap helper) {
  }

  public final boolean shouldKeepLineFeeds() {
    return (myFlags & SHOULD_KEEP_LINE_BREAKS_MASK) != 0;
  }

  public int getKeepBlankLines() {
    return myKeepBlankLines;
  }

  public final boolean shouldKeepFirstColumn() {
    return (myFlags & SHOULD_KEEP_FIRST_COLUMN_MASK) != 0;
  }

  /**
   * <b>Note:</b> current implementation uses soft type check, i.e. it checks that instance of the given object
   * IS-A {@link SpacingImpl} and compares state defined at this class only. That means that sub-classes are assumed
   * not to override this method in order to preserve {@code 'symmetric'} property.
   *
   * @param o   {@inheritDoc}
   * @return    {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SpacingImpl)) return false;
    final SpacingImpl spacing = (SpacingImpl)o;
    return myFlags == spacing.myFlags &&
           myMinSpaces == spacing.myMinSpaces &&
           myMaxSpaces == spacing.myMaxSpaces &&
           myMinLineFeeds == spacing.myMinLineFeeds &&
           myPrefLineFeeds == spacing.myPrefLineFeeds &&
           myKeepBlankLines == spacing.myKeepBlankLines;
  }

  @Override
  public int hashCode() {
    return myMinSpaces + myMaxSpaces * 29 + myMinLineFeeds * 11 + myFlags + myKeepBlankLines + myPrefLineFeeds;
  }

  @NonNls
  @Override
  public String toString() {
    return "<Spacing: minSpaces=" + myMinSpaces + " maxSpaces=" + myMaxSpaces + " minLineFeeds=" + myMinLineFeeds + ">";
  }

  public int getPrefLineFeeds() {
    return Math.max(myPrefLineFeeds, myMinLineFeeds);
  }
}
