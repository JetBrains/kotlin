/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.formatting.alignment;

import com.intellij.formatting.Alignment;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Arrays.asList;

/** {@code GoF 'Strategy'} for {@link Alignment} retrieval. */
public abstract class AlignmentStrategy {

  private static final AlignmentStrategy NULL_STRATEGY = wrap(null);

  /** @return shared strategy instance that returns {@code null} all the time */
  public static AlignmentStrategy getNullStrategy() {
    return NULL_STRATEGY;
  }

  /**
   * Delegates the processing to {@link #wrap(Alignment, boolean, IElementType...)} with {@code 'true'} as the second argument
   *
   * @param alignment     target alignment to wrap
   * @param filterTypes   types to use as a filter
   * @return              alignment strategy for the given parameters
   */
  public static AlignmentStrategy wrap(@Nullable Alignment alignment, IElementType... filterTypes) {
    return new SharedAlignmentStrategy(alignment, true, filterTypes);
  }

  /**
   * Constructs strategy that returns given alignment for all elements which types pass through the target filter.
   *
   * @param alignment         target alignment to wrap
   * @param ignoreFilterTypes flag that defines if given alignment should be returned for all elements with given types or
   *                          all elements except those with the given types
   * @param filterTypes       element types that should be used for filtering on subsequent calls
   *                          to {@link #getAlignment(IElementType)}
   * @return strategy that returns given alignment all the time for elements which types pass through the target
   *         filter; {@code null} otherwise
   */
  public static AlignmentStrategy wrap(Alignment alignment, boolean ignoreFilterTypes, IElementType... filterTypes) {
    return new SharedAlignmentStrategy(alignment, ignoreFilterTypes, filterTypes);
  }

  /**
   * Delegates to {@link #createAlignmentPerTypeStrategy(Collection, IElementType, boolean, Alignment.Anchor)} with no parent type
   * check ({@code null} is delivered as a parent type) and {@link Alignment.Anchor#LEFT left anchor}.
   *
   * @param targetTypes           target child types
   * @param allowBackwardShift    flag that defines if backward alignment shift is allowed
   * @return                      alignment strategy for the given arguments
   */
  public static AlignmentPerTypeStrategy createAlignmentPerTypeStrategy(@NotNull Collection<IElementType> targetTypes,
                                                                        boolean allowBackwardShift) {
    return new AlignmentPerTypeStrategy(targetTypes, null, allowBackwardShift, Alignment.Anchor.LEFT);
  }

  /**
   * Delegates the processing to {@link #createAlignmentPerTypeStrategy(Collection, IElementType, boolean, Alignment.Anchor)}
   * with the given arguments and {@link Alignment.Anchor#LEFT left anchor}.
   *
   * @param targetTypes        target types for which cached alignment should be returned
   * @param parentType         target parent type
   * @param allowBackwardShift flag that specifies if former aligned element may be shifted to right in order to align
   *                           to subsequent element
   * @return                   alignment retrieval strategy that follows the rules described above
   */
  public static AlignmentPerTypeStrategy createAlignmentPerTypeStrategy(
    @NotNull Collection<IElementType> targetTypes, @Nullable IElementType parentType, boolean allowBackwardShift) {
    return createAlignmentPerTypeStrategy(targetTypes, parentType, allowBackwardShift, Alignment.Anchor.LEFT);
  }

  /**
   * Creates strategy that creates and caches one alignment per given type internally and returns it on subsequent calls
   * to {@link #getAlignment(IElementType, IElementType)} for elements which type is listed at the given collection and parent type
   * (if defined) is the same as the given one; {@code null} is returned from {@link #getAlignment(IElementType, IElementType)} for all
   * other elements.
   * <p/>
   * This strategy is assumed to be used at following situations - suppose we want to align code blocks that doesn't belong
   * to the same parent but have similar structure, e.g. variable declaration assignments like the one below:
   * <pre>
   *     int start  = 1;
   *     int finish = 2;
   * </pre>
   * We can provide parent blocks of that target blocks with the same instance of this alignment strategy and let them eventually
   * reuse the same alignment objects for target sub-blocks of the same type.
   *
   * @param targetTypes        target types for which cached alignment should be returned
   * @param parentType         target parent type
   * @param allowBackwardShift flag that specifies if former aligned element may be shifted to right in order to align
   *                           to subsequent element (e.g. {@code '='} block of {@code 'int start  = 1'} statement
   *                           below is shifted one symbol right in order to align to the {@code '='} block
   *                           of {@code 'int finish  = 1'} statement)
   * @return                   alignment retrieval strategy that follows the rules described above
   */
  public static AlignmentPerTypeStrategy createAlignmentPerTypeStrategy(
    @NotNull Collection<IElementType> targetTypes, @Nullable IElementType parentType, boolean allowBackwardShift,
    @NotNull Alignment.Anchor anchor) {
    return new AlignmentPerTypeStrategy(targetTypes, parentType, allowBackwardShift, anchor);
  }

  /**
   * Delegates the processing to {@link #getAlignment(IElementType, IElementType)} without parent element type
   * filtering ({@code null} is used as parent element type).
   *
   * @param childType     target child type
   * @return              alignment to use
   */
  @Nullable
  public Alignment getAlignment(@Nullable IElementType childType) {
    return getAlignment(null, childType);
  }

  /**
   * Requests current strategy for alignment to use for the child of the given type assuming that parent node has the given type.
   *
   * @param parentType    parent type to use for filtering (if not {@code null})
   * @param childType     child type to use for filtering (if not {@code null})
   * @return              alignment to use for the given arguments
   */
  @Nullable
  public abstract Alignment getAlignment(@Nullable IElementType parentType, @Nullable IElementType childType);

  /**
   * Stands for {@link AlignmentStrategy} implementation that is configured to return single pre-configured {@link Alignment} object
   * or {@code null} for all calls to {@link #getAlignment(IElementType)}.
   */
  private static class SharedAlignmentStrategy extends AlignmentStrategy {

    private final Set<IElementType> myFilterElementTypes = new HashSet<>();

    private final Alignment myAlignment;
    private final boolean   myIgnoreFilterTypes;

    private SharedAlignmentStrategy(Alignment alignment, boolean ignoreFilterTypes, IElementType... disabledElementTypes) {
      myAlignment = alignment;
      myIgnoreFilterTypes = ignoreFilterTypes;
      myFilterElementTypes.addAll(asList(disabledElementTypes));
    }

    @Override
    @Nullable
    public Alignment getAlignment(@Nullable IElementType parentType, @Nullable IElementType childType) {
      return myFilterElementTypes.contains(childType) ^ myIgnoreFilterTypes ? myAlignment : null;
    }
  }

  /**
   * Alignment strategy that creates and caches alignments for target element types and returns them for elements with the
   * same types.
   */
  public static class AlignmentPerTypeStrategy extends AlignmentStrategy {
    private final Map<IElementType, Alignment> myAlignments = new HashMap<>();

    private final IElementType     myParentType;
    private final boolean          myAllowBackwardShift;

    AlignmentPerTypeStrategy(Collection<IElementType> targetElementTypes,
                             IElementType parentType,
                             boolean allowBackwardShift,
                             Alignment.Anchor anchor) {
      myParentType = parentType;
      myAllowBackwardShift = allowBackwardShift;
      for (IElementType elementType : targetElementTypes) {
        myAlignments.put(elementType, Alignment.createAlignment(myAllowBackwardShift, anchor));
      }
    }

    @Override
    public Alignment getAlignment(@Nullable IElementType parentType, @Nullable IElementType childType) {
      if (myParentType != null && parentType != null && myParentType != parentType) {
        return null;
      }
      return myAlignments.get(childType);
    }

    public void renewAlignment(IElementType elementType) {
      myAlignments.put(elementType, Alignment.createAlignment(myAllowBackwardShift));
    }
  }
}