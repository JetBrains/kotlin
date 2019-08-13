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
package org.jetbrains.jps.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.impl.JpsEventDispatcherBase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author nik
 */
class TestJpsEventDispatcher extends JpsEventDispatcherBase implements JpsEventDispatcher {
  private final List<JpsElement> myAdded = new ArrayList<>();
  private final List<JpsElement> myRemoved = new ArrayList<>();
  private final List<JpsElement> myChanged = new ArrayList<>();

  @Override
  public <T extends JpsElement> void fireElementAdded(@NotNull T element, @NotNull JpsElementChildRole<T> role) {
    super.fireElementAdded(element, role);
    myAdded.add(element);
  }

  @Override
  public <T extends JpsElement> void fireElementRemoved(@NotNull T element, @NotNull JpsElementChildRole<T> role) {
    super.fireElementRemoved(element, role);
    myRemoved.add(element);
  }

  @Override
  public void fireElementChanged(@NotNull JpsElement element) {
    myChanged.add(element);
  }

  @Override
  public void fireElementRenamed(@NotNull JpsNamedElement element, @NotNull String oldName, @NotNull String newName) {
  }

  public <T extends JpsElement> List<T> retrieveAdded(Class<T> type) {
    return retrieve(type, myAdded);
  }

  public <T extends JpsElement> List<T> retrieveRemoved(Class<T> type) {
    return retrieve(type, myRemoved);
  }

  public <T extends JpsElement> List<T> retrieveChanged(Class<T> type) {
    return retrieve(type, myChanged);
  }

  public void clear() {
    myAdded.clear();
    myRemoved.clear();
    myChanged.clear();
  }


  private static <T extends JpsElement> List<T> retrieve(Class<T> type, List<JpsElement> list) {
    final List<T> result = new ArrayList<>();
    final Iterator<JpsElement> iterator = list.iterator();
    while (iterator.hasNext()) {
      JpsElement element = iterator.next();
      if (type.isInstance(element)) {
        result.add(type.cast(element));
        iterator.remove();
      }
    }
    return result;
  }
}
