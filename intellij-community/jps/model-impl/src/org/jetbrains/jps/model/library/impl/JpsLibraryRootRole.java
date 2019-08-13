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
import org.jetbrains.jps.model.JpsEventDispatcher;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.library.JpsLibraryRoot;
import org.jetbrains.jps.model.library.JpsLibraryRootListener;
import org.jetbrains.jps.model.library.JpsOrderRootType;

/**
 * @author nik
 */
public class JpsLibraryRootRole extends JpsElementChildRoleBase<JpsLibraryRoot> {
  private final JpsOrderRootType myRootType;

  public JpsLibraryRootRole(@NotNull JpsOrderRootType rootType) {
    super("library root");
    myRootType = rootType;
  }

  @Override
  public void fireElementAdded(@NotNull JpsEventDispatcher dispatcher, @NotNull JpsLibraryRoot element) {
    dispatcher.getPublisher(JpsLibraryRootListener.class).rootAdded(element);
  }

  @Override
  public void fireElementRemoved(@NotNull JpsEventDispatcher dispatcher, @NotNull JpsLibraryRoot element) {
    dispatcher.getPublisher(JpsLibraryRootListener.class).rootRemoved(element);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    return myRootType.equals(((JpsLibraryRootRole)o).myRootType);
  }

  @Override
  public int hashCode() {
    return myRootType.hashCode();
  }
}
