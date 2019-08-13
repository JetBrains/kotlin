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
package org.jetbrains.jps.model.library.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementCollection;
import org.jetbrains.jps.model.JpsElementTypeWithDefaultProperties;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryCollection;
import org.jetbrains.jps.model.library.JpsLibraryType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;

import java.util.List;

/**
 * @author nik
 */
public class JpsLibraryCollectionImpl implements JpsLibraryCollection {
  private final JpsElementCollection<JpsLibrary> myCollection;

  public JpsLibraryCollectionImpl(JpsElementCollection<JpsLibrary> collection) {
    myCollection = collection;
  }

  @NotNull
  @Override
  public <P extends JpsElement, LibraryType extends JpsLibraryType<P> & JpsElementTypeWithDefaultProperties<P>>
  JpsLibrary addLibrary(@NotNull String name, @NotNull LibraryType type) {
    return addLibrary(name, type, type.createDefaultProperties());
  }

  @NotNull
  @Override
  public <P extends JpsElement> JpsTypedLibrary<P> addLibrary(@NotNull String name, @NotNull JpsLibraryType<P> type,
                                                                        @NotNull P properties) {
    return myCollection.addChild(new JpsLibraryImpl<>(name, type, properties));
  }

  @NotNull
  @Override
  public List<JpsLibrary> getLibraries() {
    return myCollection.getElements();
  }

  @NotNull
  @Override
  public <P extends JpsElement> Iterable<JpsTypedLibrary<P>> getLibraries(@NotNull JpsLibraryType<P> type) {
    return myCollection.getElementsOfType(type);
  }

  @Override
  public void addLibrary(@NotNull JpsLibrary library) {
    myCollection.addChild(library);
  }

  @Override
  public void removeLibrary(@NotNull JpsLibrary library) {
    myCollection.removeChild(library);
  }

  @Override
  public JpsLibrary findLibrary(@NotNull String name) {
    for (JpsLibrary library : getLibraries()) {
      if (name.equals(library.getName())) {
        return library;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public <E extends JpsElement> JpsTypedLibrary<E> findLibrary(@NotNull String name, @NotNull JpsLibraryType<E> type) {
    for (JpsTypedLibrary<E> library : getLibraries(type)) {
      if (name.equals(library.getName())) {
        return library;
      }
    }
    return null;
  }


}
