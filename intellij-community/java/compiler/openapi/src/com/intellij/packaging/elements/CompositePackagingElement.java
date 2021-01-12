// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.elements;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class CompositePackagingElement<S> extends PackagingElement<S> implements RenameablePackagingElement {
  private final List<PackagingElement<?>> myChildren = new ArrayList<>();
  private List<PackagingElement<?>> myUnmodifiableChildren;

  protected CompositePackagingElement(PackagingElementType type) {
    super(type);
  }

  public <T extends PackagingElement<?>> T addOrFindChild(@NotNull T child) {
    for (PackagingElement<?> element : myChildren) {
      if (element.isEqualTo(child)) {
        if (element instanceof CompositePackagingElement) {
          final List<PackagingElement<?>> children = ((CompositePackagingElement<?>)child).getChildren();
          ((CompositePackagingElement<?>)element).addOrFindChildren(children);
        }
        //noinspection unchecked
        return (T) element;
      }
    }
    myChildren.add(child);
    return child;
  }

  public void addFirstChild(@NotNull PackagingElement<?> child) {
    myChildren.add(0, child);
    for (int i = 1; i < myChildren.size(); i++) {
      PackagingElement<?> element = myChildren.get(i);
      if (element.isEqualTo(child)) {
        if (element instanceof CompositePackagingElement<?>) {
          ((CompositePackagingElement<?>)child).addOrFindChildren(((CompositePackagingElement<?>)element).getChildren());
        }
        myChildren.remove(i);
        break;
      }
    }
  }

  public List<? extends PackagingElement<?>> addOrFindChildren(Collection<? extends PackagingElement<?>> children) {
    List<PackagingElement<?>> added = new ArrayList<>();
    for (PackagingElement<?> child : children) {
      added.add(addOrFindChild(child));
    }
    return added;
  }

  @Nullable
  public PackagingElement<?> moveChild(int index, int direction) {
    int target = index + direction;
    if (0 <= index && index < myChildren.size() && 0 <= target && target < myChildren.size()) {
      final PackagingElement<?> element1 = myChildren.get(index);
      final PackagingElement<?> element2 = myChildren.get(target);
      myChildren.set(index, element2);
      myChildren.set(target, element1);
      return element1;
    }
    return null;
  }

  public void removeChild(@NotNull PackagingElement<?> child) {
    myChildren.remove(child);
  }

  public void removeChildren(@NotNull Collection<? extends PackagingElement<?>> children) {
    myChildren.removeAll(children);
  }

  @NotNull
  public List<PackagingElement<?>> getChildren() {
    if (myUnmodifiableChildren == null) {
      myUnmodifiableChildren = Collections.unmodifiableList(myChildren);
    }
    return myUnmodifiableChildren;
  }

  @Override
  public boolean canBeRenamed() {
    return true;
  }

  public void removeAllChildren() {
    myChildren.clear();
  }

  @Nullable
  public CompositePackagingElement<?> findCompositeChild(@NotNull String name) {
    for (PackagingElement<?> child : myChildren) {
      if (child instanceof CompositePackagingElement && name.equals(((CompositePackagingElement)child).getName())) {
        return (CompositePackagingElement)child;
      }
    }
    return null;
  }
}
