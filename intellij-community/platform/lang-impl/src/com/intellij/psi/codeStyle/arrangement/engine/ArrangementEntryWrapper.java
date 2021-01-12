/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.psi.codeStyle.arrangement.engine;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.arrangement.ArrangementEntry;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Auxiliary data structure used {@link ArrangementEngine#arrange(PsiFile, Collection) arrangement}.
 * <p/>
 * The general idea is to provide the following:
 * <pre>
 * <ul>
 *   <li>'parent-child' and 'sibling' relations between the {@link ArrangementEntry entries};</li>
 *   <li>ability to reflect actual entry range (after its arrangement and/or blank lines addition/removal);</li>
 * </ul>
 * </pre>
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 */
public class ArrangementEntryWrapper<E extends ArrangementEntry> {

  @NotNull private final List<ArrangementEntryWrapper<E>> myChildren = new ArrayList<>();
  @NotNull private final E myEntry;

  @Nullable private ArrangementEntryWrapper<E> myParent;
  @Nullable private ArrangementEntryWrapper<E> myPrevious;
  @Nullable private ArrangementEntryWrapper<E> myNext;

  private int myStartOffset;
  private int myEndOffset;
  private int myBlankLinesBefore;

  @SuppressWarnings("unchecked")
  public ArrangementEntryWrapper(@NotNull E entry) {
    myEntry = entry;
    myStartOffset = entry.getStartOffset();
    myEndOffset = entry.getEndOffset();
    ArrangementEntryWrapper<E> previous = null;
    for (ArrangementEntry child : entry.getChildren()) {
      ArrangementEntryWrapper<E> childWrapper = new ArrangementEntryWrapper<>((E)child);
      childWrapper.setParent(this);
      if (previous != null) {
        previous.setNext(childWrapper);
        childWrapper.setPrevious(previous);
      }
      previous = childWrapper;
      myChildren.add(childWrapper);
    }
  }

  @NotNull
  public E getEntry() {
    return myEntry;
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  public int getEndOffset() {
    return myEndOffset;
  }

  public void setEndOffset(int endOffset) {
    myEndOffset = endOffset;
  }

  @Nullable
  public ArrangementEntryWrapper<E> getParent() {
    return myParent;
  }

  public void setParent(@Nullable ArrangementEntryWrapper<E> parent) {
    myParent = parent;
  }

  @Nullable
  public ArrangementEntryWrapper<E> getPrevious() {
    return myPrevious;
  }

  public void setPrevious(@Nullable ArrangementEntryWrapper<E> previous) {
    myPrevious = previous;
  }

  @Nullable
  public ArrangementEntryWrapper<E> getNext() {
    return myNext;
  }

  public int getBlankLinesBefore() {
    return myBlankLinesBefore;
  }

  @SuppressWarnings("AssignmentToForLoopParameter")
  public void updateBlankLines(@NotNull Document document) {
    myBlankLinesBefore = 0;
    int lineFeeds = 0;
    CharSequence text = document.getCharsSequence();
    for (int current = getStartOffset() - 1; current >= 0; current--) {
      current = CharArrayUtil.shiftBackward(text, current, " \t");
      if (current > 0 && text.charAt(current) == '\n') lineFeeds++;
      else break;
    }
    if (lineFeeds > 0) myBlankLinesBefore = lineFeeds - 1;
  }

  public void setNext(@Nullable ArrangementEntryWrapper<E> next) {
    myNext = next;
  }

  @NotNull
  public List<ArrangementEntryWrapper<E>> getChildren() {
    return myChildren;
  }

  public void applyShift(int shift) {
    myStartOffset += shift;
    myEndOffset += shift;
    for (ArrangementEntryWrapper<E> child : myChildren) {
      child.applyShift(shift);
    }
  }

  @Override
  public int hashCode() {
    return myEntry.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ArrangementEntryWrapper wrapper = (ArrangementEntryWrapper)o;
    return myEntry.equals(wrapper.myEntry);
  }

  @Override
  public String toString() {
    return String.format("range: [%d; %d), entry: %s", myStartOffset, myEndOffset, myEntry.toString());
  }
}
