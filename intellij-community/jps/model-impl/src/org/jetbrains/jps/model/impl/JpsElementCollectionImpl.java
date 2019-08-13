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
package org.jetbrains.jps.model.impl;

import com.intellij.util.SmartList;
import com.intellij.util.containers.FilteringIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.ex.JpsElementBase;

import java.util.*;

/**
 * @author nik
 */
public class JpsElementCollectionImpl<E extends JpsElement> extends JpsElementBase<JpsElementCollectionImpl<E>> implements JpsElementCollection<E> {
  private final List<E> myElements;
  private final Map<E, E> myCopyToOriginal;
  private final JpsElementChildRole<E> myChildRole;

  JpsElementCollectionImpl(JpsElementChildRole<E> role) {
    myChildRole = role;
    myElements = new SmartList<>();
    myCopyToOriginal = null;
  }

  private JpsElementCollectionImpl(JpsElementCollectionImpl<E> original) {
    myChildRole = original.myChildRole;
    myElements = new SmartList<>();
    myCopyToOriginal = new HashMap<>();
    for (E e : original.myElements) {
      //noinspection unchecked
      final E copy = (E)e.getBulkModificationSupport().createCopy();
      setParent(copy, this);
      myElements.add(copy);
      myCopyToOriginal.put(copy, e);
    }
  }

  @Override
  public List<E> getElements() {
    return myElements;
  }

  @Override
  public <X extends JpsTypedElement<P>, P extends JpsElement> Iterable<X> getElementsOfType(@NotNull final JpsElementType<P> type) {
    return new JpsElementIterable<>(type);
  }

  @NotNull
  @Override
  public E addChild(@NotNull JpsElementCreator<E> creator) {
    return addChild(creator.create());
  }

  @Override
  public <X extends E> X addChild(X element) {
    myElements.add(element);
    setParent(element, this);
    final JpsEventDispatcher eventDispatcher = getEventDispatcher();
    if (eventDispatcher != null) {
      eventDispatcher.fireElementAdded(element, myChildRole);
    }
    return element;
  }

  @Override
  public void removeChild(@NotNull E element) {
    final boolean removed = myElements.remove(element);
    if (removed) {
      final JpsEventDispatcher eventDispatcher = getEventDispatcher();
      if (eventDispatcher != null) {
        eventDispatcher.fireElementRemoved(element, myChildRole);
      }
      setParent(element, null);
    }
  }

  @Override
  public void removeAllChildren() {
    List<E> elements = new ArrayList<>(myElements);
    for (E element : elements) {
      removeChild(element);
    }
  }

  @NotNull
  @Override
  public JpsElementCollectionImpl<E> createCopy() {
    return new JpsElementCollectionImpl<>(this);
  }

  @Override
  public void applyChanges(@NotNull JpsElementCollectionImpl<E> modified) {
    Set<E> toRemove = new LinkedHashSet<>(myElements);
    List<E> toAdd = new ArrayList<>();
    final Map<E, E> copyToOriginal = modified.myCopyToOriginal;
    for (E element : modified.myElements) {
      final E original = copyToOriginal != null ? copyToOriginal.get(element) : null;
      if (original != null) {
        //noinspection unchecked
        ((BulkModificationSupport<E>)original.getBulkModificationSupport()).applyChanges(element);
        toRemove.remove(original);
      }
      else {
        //noinspection unchecked
        final E copy = (E)element.getBulkModificationSupport().createCopy();
        toAdd.add(copy);
      }
    }
    for (E e : toRemove) {
      removeChild(e);
    }
    for (E e : toAdd) {
      addChild(e);
    }
  }

  private class JpsElementIterable<X extends JpsTypedElement<P>, P extends JpsElement> implements Iterable<X> {
    private final JpsElementType<? extends JpsElement> myType;

    JpsElementIterable(JpsElementType<P> type) {
      myType = type;
    }

    @Override
    public Iterator<X> iterator() {
      //noinspection unchecked
      Iterator<JpsTypedElement<?>> iterator = (Iterator<JpsTypedElement<?>>)myElements.iterator();
      return new FilteringIterator<>(iterator, e -> e.getType().equals(myType));
    }
  }
}
