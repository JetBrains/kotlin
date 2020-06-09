// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.options;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.BiConsumer;

public class CompoundScheme<E extends SchemeElement> implements ExternalizableScheme {
  protected String myName;
  protected final ArrayList<E> myElements = new ArrayList<>();

  public CompoundScheme(final String name) {
    setName(name);
  }

  public final void addElement(E t) {
    myElements.add(t);
  }

  public final @NotNull List<E> getElements() {
    if (myElements.isEmpty()) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(new ArrayList<>(myElements));
  }

  @Override
  public @NotNull String getName() {
    return myName;
  }

  @Override
  public final void setName(final @NotNull String name) {
    myName = name;
    for (E template : myElements) {
      template.setGroupName(name);
    }
  }

  public final void removeElement(final E template) {
    myElements.remove(template);
  }

  public final boolean isEmpty() {
    return myElements.isEmpty();
  }

  private @NotNull CompoundScheme<E> createNewInstance(String name) {
    try {
      Constructor<? extends CompoundScheme> constructor =  getClass().getConstructor(String.class);
      try {
        constructor.setAccessible(true);
      }
      catch (Exception ignored) {
      }
      //noinspection unchecked
      return constructor.newInstance(name);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  CompoundScheme<E> copy() {
    CompoundScheme<E> result = createNewInstance(getName());
    result.resetFrom(this);
    return result;
  }

   void resetFrom(@NotNull CompoundScheme<? extends E> template) {
    myElements.clear();
    myElements.ensureCapacity(template.myElements.size());
    for (E element : template.myElements) {
      //noinspection unchecked
      myElements.add((E)element.copy());
    }
  }

  public boolean containsElement(@NotNull String key) {
    return ContainerUtil.exists(myElements, e -> key.equals(e.getKey()));
  }

  public static final class MutatorHelper<T extends CompoundScheme<E>, E extends SchemeElement> {
    private final Map<T, T> copiedToOriginal = new IdentityHashMap<>();

    public @NotNull T copy(@NotNull T scheme) {
      //noinspection unchecked
      T copied = (T)scheme.copy();
      copiedToOriginal.put(copied, scheme);
      return copied;
    }

    public @NotNull List<T> apply(final @NotNull List<? extends T> copiedSchemes) {
      return apply(copiedSchemes, null);
    }

    public @NotNull List<T> apply(@NotNull List<? extends T> copiedSchemes, @Nullable BiConsumer<? super T, ? super T> changedConsumer) {
      copiedToOriginal.values().removeIf(it -> !ContainerUtil.containsIdentity(copiedSchemes, it));
      List<T> originals = new ArrayList<>(copiedSchemes.size());
      for (T copied : copiedSchemes) {
        T original = copiedToOriginal.remove(copied);
        if (original == null) {
          //noinspection unchecked
          original = (T)copied.copy();
          copiedToOriginal.put(copied, original);
        }
        else {
          if (changedConsumer != null) {
            changedConsumer.accept(original, copied);
          }
          original.resetFrom(copied);
        }

        originals.add(original);
      }
      return originals;
    }

    public void clear() {
      copiedToOriginal.clear();
    }
  }
}
