/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.options;

import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import gnu.trove.TObjectObjectProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

public class CompoundScheme<E extends SchemeElement> implements ExternalizableScheme {
  protected String myName;
  protected final ArrayList<E> myElements = new ArrayList<>();

  public CompoundScheme(final String name) {
    setName(name);
  }

  public final void addElement(E t) {
    if (!contains(t)) {
      myElements.add(t);
    }
  }

  @NotNull
  public final List<E> getElements() {
    if (myElements.isEmpty()) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(new ArrayList<>(myElements));
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public final void setName(@NotNull final String name) {
    myName = name;
    for (E template : myElements) {
      template.setGroupName(name);
    }
  }

  public final void removeElement(final E template) {
    for (Iterator<E> templateIterator = myElements.iterator(); templateIterator.hasNext();) {
      E t = templateIterator.next();
      if (t.getKey() != null && t.getKey().equals(template.getKey())) {
        templateIterator.remove();
      }
    }
  }

  public final boolean isEmpty() {
    return myElements.isEmpty();
  }

  @NotNull
  private CompoundScheme<E> createNewInstance(String name) {
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

  public boolean contains(@NotNull E element) {
    for (E t : myElements) {
      String key = t.getKey();
      if (key != null && key.equals(element.getKey())) {
        return true;
      }
    }
    return false;
  }

  public static final class MutatorHelper<T extends CompoundScheme<E>, E extends SchemeElement> {
    private final THashMap<T, T> copiedToOriginal = ContainerUtil.newIdentityTroveMap();

    @NotNull
    public T copy(@NotNull T scheme) {
      //noinspection unchecked
      T copied = (T)scheme.copy();
      copiedToOriginal.put(copied, scheme);
      return copied;
    }

    @NotNull
    public List<T> apply(@NotNull final List<? extends T> copiedSchemes) {
      return apply(copiedSchemes, null);
    }

    @NotNull
    public List<T> apply(@NotNull final List<? extends T> copiedSchemes, @Nullable BiConsumer<? super T, ? super T> changedConsumer) {
      copiedToOriginal.retainEntries(new TObjectObjectProcedure<T, T>() {
        @Override
        public boolean execute(T copied, T original) {
          return ContainerUtil.containsIdentity(copiedSchemes, copied);
        }
      });

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
