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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WrapImpl extends Wrap {
  private static final int MAX_IS_CHILD_OF_CALCULATION_ITERATIONS = 50;

  /**
   * The block where the wrap needs to happen if the CHOP wrap mode is used and the chain of blocks exceeds the right margin.
   */
  private LeafBlockWrapper myChopStartBlock = null;
  private int myWrapOffset = -1;
  private int myFlags;

  private static final Set<WrapImpl> emptyParentsSet = Collections.emptySet();
  private Set<WrapImpl> myParents = emptyParentsSet;
  private Map<WrapImpl, Collection<LeafBlockWrapper>> myIgnoredWraps;

  private static final int IGNORE_PARENT_WRAPS_MASK = 1;
  private static final int ACTIVE_MASK = 2;
  private static final int WRAP_FIRST_ELEMENT_MASK = 4;
  private static final int TYPE_MASK = 0x18;
  private static final int TYPE_SHIFT = 3;
  private static final Type[] myTypes = Type.values();


  public boolean isChildOf(@Nullable final WrapImpl wrap, LeafBlockWrapper leaf) {
    return isChildOf(wrap, leaf, new FormatterIterationMonitor<>(MAX_IS_CHILD_OF_CALCULATION_ITERATIONS, false));
  }

  public boolean isChildOf(@Nullable final WrapImpl wrap, LeafBlockWrapper leaf, @NotNull FormatterIterationMonitor<Boolean> iterationMonitor) {
    if (getIgnoreParentWraps()) return false;
    if (!iterationMonitor.iterate()) return iterationMonitor.getFallbackValue();
    if (leaf != null && myIgnoredWraps != null) {
      Collection<LeafBlockWrapper> leaves = myIgnoredWraps.get(wrap);
      if (leaves != null && leaves.contains(leaf)) {
        return false;
      }
    }
    for (WrapImpl parent : myParents) {
      if (parent == wrap) return true;
      if (parent.isChildOf(wrap, leaf, iterationMonitor)) return true;
    }
    return false;
  }

  /**
   * Allows to register given wrap as a parent of the current wrap.
   * <p/>
   * {@code 'Parent'} wrap registration here means that {@link #isChildOf(WrapImpl, LeafBlockWrapper)} returns
   * {@code 'true'} if given wrap is used as a {@code 'parent'} argument.
   *
   * @param parent    parent wrap to register for the current wrap
   */
  void registerParent(@Nullable WrapImpl parent) {
    if (parent == this) return;
    if (parent == null) return;
    if (parent.isChildOf(this, null, new FormatterIterationMonitor<>(MAX_IS_CHILD_OF_CALCULATION_ITERATIONS, true))) return;
    if (myParents == emptyParentsSet) myParents = new HashSet<>(5);
    myParents.add(parent);
  }

  /**
   * Resets the following state of the current wrap object:
   * <ul>
   *   <li>'{@link #getChopStartBlock() firstEntry}' property value is set to {@code null};</li>
   *   <li>'{@link #getWrapOffset() firstPosition}' property value is set to {@code '-1'};</li>
   *   <li>'{@link #isActive() isActive}' property value is set to {@code 'false'};</li>
   * </ul>
   */
  public void reset() {
    myChopStartBlock = null;
    myWrapOffset = -1;
    myFlags &=~ ACTIVE_MASK;
  }

  /**
   * Allows to check if single wrap is {@link #registerParent(WrapImpl) registered} for the current wrap and return
   * it in case of success.
   *
   * @return    single wrap registered as a parent of the current wrap if any;
   *            {@code null} if no wraps or more than one wrap is registered as a parent for the current wrap
   */
  public WrapImpl getParent(){
    if (myParents != null && myParents.size() == 1) {
      return myParents.iterator().next();
    }

    return null;
  }

  public final boolean getIgnoreParentWraps() {
    return (myFlags & IGNORE_PARENT_WRAPS_MASK) != 0;
  }

  /**
   * Allows to mark given wrap as {@code 'ignored'} for the given block. I.e. 'false' will be returned
   * for subsequent calls to {@link #isChildOf(WrapImpl, LeafBlockWrapper)} with the same arguments.
   *
   * @param wrap          target wrap
   * @param currentBlock  target block for which given wrap should be ignored
   */
  public void ignoreParentWrap(@Nullable final WrapImpl wrap, final LeafBlockWrapper currentBlock) {
    if (myIgnoredWraps == null) {
      myIgnoredWraps = new HashMap<>(5);
    }
    myIgnoredWraps.putIfAbsent(wrap, new HashSet<>(2));
    myIgnoredWraps.get(wrap).add(currentBlock);
  }

  public enum Type{
    DO_NOT_WRAP, WRAP_AS_NEEDED, CHOP_IF_NEEDED, WRAP_ALWAYS
  }

  public LeafBlockWrapper getChopStartBlock() {
    return myChopStartBlock;
  }

  /**
   * Performs the following changes at wrap object state:
   * <ul>
   *   <li>'{@link #getChopStartBlock() firstEntry}' property value is dropped (set to {@code null})</li>
   *   <li>'{@link #isActive() isActive}' property value is set (to {@code true})</li>
   * </ul>
   */
  public void setActive() {
    myChopStartBlock = null;
    myFlags |= ACTIVE_MASK;
  }

  /**
   * Applies given value to the '{@link #getWrapOffset() firstPosition}' property value if it's value is undefined at the moment
   * (has negative value).
   *
   * @param startOffset   new '{@link #getWrapOffset() firstPosition}' property value to use if current value is undefined (negative)
   */
  public void setWrapOffset(final int startOffset) {
    if (myWrapOffset < 0) {
      myWrapOffset = startOffset;
    }
  }

  /**
   * @return    '{@link #getWrapOffset() firstPosition}' property value defined previously via {@link #setWrapOffset(int)} if any;
   *            {@code '-1'} otherwise
   */
  public int getWrapOffset() {
    return myWrapOffset;
  }

  public WrapImpl(WrapType type, boolean wrapFirstElement) {
    Type myType;

    switch(type) {
        case NORMAL: myType = Type.WRAP_AS_NEEDED;break;
        case NONE: myType= Type.DO_NOT_WRAP;break;
        case ALWAYS: myType = Type.WRAP_ALWAYS; break;
        case CHOP_DOWN_IF_LONG:
        default: myType = Type.CHOP_IF_NEEDED;
    }

    myFlags |= (wrapFirstElement ? WRAP_FIRST_ELEMENT_MASK:0) | (myType.ordinal() << TYPE_SHIFT);
  }

  public final Type getType() {
    return myTypes[(myFlags & TYPE_MASK) >>> TYPE_SHIFT];
  }

  /**
   * Allows to check if current wrap object is configured to wrap first element. This property is defined at
   * {@link #WrapImpl(WrapType, boolean) constructor} during object initialization and can't be changed later.
   *
   * @return    {@code 'wrapFirstElement'} property value
   */
  public final boolean isWrapFirstElement() {
    return (myFlags & WRAP_FIRST_ELEMENT_MASK) != 0;
  }

  public void saveChopBlock(LeafBlockWrapper current) {
    if (myChopStartBlock == null) {
      myChopStartBlock = current;
    }
  }

  public final boolean isActive() {
    return (myFlags & ACTIVE_MASK) != 0;
  }

  public String toString() {
    return getType().toString();
  }
  
  /**
   * Allows to instruct current wrap to ignore all parent wraps, i.e. all calls to {@link #isChildOf(WrapImpl, LeafBlockWrapper)}
   * return {@code 'false'} after invocation of this method.
   */
  @Override
  public void ignoreParentWraps() {
    myFlags |= IGNORE_PARENT_WRAPS_MASK;
  }
}
