// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.ex;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public class JpsElementContainerImpl extends JpsElementContainerEx implements JpsElementContainer {
  private final Object myDataLock = new Object();
  private final Map<JpsElementChildRole<?>, JpsElement> myElements = new Object2ObjectOpenHashMap<>(1);
  private final @NotNull JpsCompositeElementBase<?> myParent;

  public JpsElementContainerImpl(@NotNull JpsCompositeElementBase<?> parent) {
    myParent = parent;
  }

  public JpsElementContainerImpl(@NotNull JpsElementContainerEx original, @NotNull JpsCompositeElementBase<?> parent) {
    myParent = parent;
    synchronized (original.getDataLock()) {
      for (Map.Entry<JpsElementChildRole<?>, JpsElement> entry : original.getElementsMap().entrySet()) {
        final JpsElementChildRole role = entry.getKey();
        final JpsElement copy = entry.getValue().getBulkModificationSupport().createCopy();
        JpsElementBase.setParent(copy, myParent);
        myElements.put(role, copy);
      }
    }
  }

  @Override
  public <T extends JpsElement> T getChild(@NotNull JpsElementChildRole<T> role) {
    synchronized (myDataLock) {
      //noinspection unchecked
      return (T)myElements.get(role);
    }
  }

  @NotNull
  @Override
  public <T extends JpsElement, P, K extends JpsElementChildRole<T> & JpsElementParameterizedCreator<T, P>> T setChild(@NotNull K role, @NotNull P param) {
    final T child = role.create(param);
    return setChild(role, child);
  }

  @NotNull
  @Override
  public <T extends JpsElement, K extends JpsElementChildRole<T> & JpsElementCreator<T>> T setChild(@NotNull K role) {
    final T child = role.create();
    return setChild(role, child);
  }

  @NotNull
  @Override
  public <T extends JpsElement, K extends JpsElementChildRole<T> & JpsElementCreator<T>> T getOrSetChild(@NotNull K role) {
    T added = null;
    try {
      synchronized (myDataLock) {
        final T cached = (T)myElements.get(role);
        if (cached != null) {
          return cached;
        }
        return added = putChild(role, role.create());
      }
    }
    finally {
      if (added != null) {
        fireChildSet(role, added);
      }
    }
  }

  @Override
  public <T extends JpsElement, P, K extends JpsElementChildRole<T> & JpsElementParameterizedCreator<T, P>> T getOrSetChild(@NotNull K role, @NotNull Supplier<P> param) {
    T added = null;
    try {
      synchronized (myDataLock) {
        final T cached = (T)myElements.get(role);
        if (cached != null) {
          return cached;
        }
        return added = putChild(role, role.create(param.get()));
      }
    }
    finally {
      if (added != null) {
        fireChildSet(role, added);
      }
    }
  }

  @Override
  public <T extends JpsElement> T setChild(JpsElementChildRole<T> role, T child) {
    try {
      synchronized (myDataLock) {
        return putChild(role, child);
      }
    }
    finally {
      fireChildSet(role, child);
    }
  }

  @NotNull
  private <T extends JpsElement> T putChild(JpsElementChildRole<T> role, T child) {
    JpsElementBase.setParent(child, myParent);
    myElements.put(role, child);
    return child;
  }

  private <T extends JpsElement> void fireChildSet(JpsElementChildRole<T> role, T child) {
    final JpsEventDispatcher eventDispatcher = getEventDispatcher();
    if (eventDispatcher != null) {
      eventDispatcher.fireElementAdded(child, role);
    }
  }

  @Override
  public <T extends JpsElement> void removeChild(@NotNull JpsElementChildRole<T> role) {
    //noinspection unchecked
    final T removed;
    synchronized (myDataLock) {
      removed = (T)myElements.remove(role);
    }
    if (removed == null) return;
    final JpsEventDispatcher eventDispatcher = getEventDispatcher();
    if (eventDispatcher != null) {
      eventDispatcher.fireElementRemoved(removed, role);
    }
    JpsElementBase.setParent(removed, null);
  }

  @Override
  protected final Object getDataLock() {
    return myDataLock;
  }

  @Override
  protected final Map<JpsElementChildRole<?>, JpsElement> getElementsMap() {
    return myElements;
  }

  @Override
  public void applyChanges(@NotNull JpsElementContainerEx modified) {
    final Collection<JpsElementChildRole<?>> roles = new ArrayList<>();

    synchronized (myDataLock) {
      roles.addAll(myElements.keySet());
    }
    for (JpsElementChildRole<?> role : roles) {
      applyChanges(role, modified);
    }

    roles.clear();
    synchronized (modified.getDataLock()) {
      roles.addAll(modified.getElementsMap().keySet());
    }
    synchronized (myDataLock) {
      roles.removeAll(myElements.keySet());
    }

    for (JpsElementChildRole<?> role : roles) {
      applyChanges(role, modified);
    }
  }

  private <T extends JpsElement> void applyChanges(JpsElementChildRole<T> role, JpsElementContainerEx modified) {
    final T child = getChild(role);
    final T modifiedChild = modified.getChild(role);
    if (child != null && modifiedChild != null) {
      final JpsElement.BulkModificationSupport modificationSupport = child.getBulkModificationSupport();
      //noinspection unchecked
      modificationSupport.applyChanges(modifiedChild);
    }
    else if (modifiedChild == null) {
      removeChild(role);
    }
    else {
      //noinspection unchecked
      setChild(role, (T)modifiedChild.getBulkModificationSupport().createCopy());
    }
  }

  @Nullable
  private JpsEventDispatcher getEventDispatcher() {
    return myParent.getEventDispatcher();
  }
}
