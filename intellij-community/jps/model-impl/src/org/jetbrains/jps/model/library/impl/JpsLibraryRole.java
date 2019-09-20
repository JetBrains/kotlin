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
import org.jetbrains.jps.model.ex.JpsElementCollectionRole;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryListener;

/**
 * @author nik
 */
public class JpsLibraryRole extends JpsElementChildRoleBase<JpsLibrary> {
  private static final JpsLibraryRole INSTANCE = new JpsLibraryRole();
  public static final JpsElementCollectionRole<JpsLibrary> LIBRARIES_COLLECTION_ROLE = JpsElementCollectionRole.create(INSTANCE);

  private JpsLibraryRole() {
    super("library");
  }

  @Override
  public void fireElementAdded(@NotNull JpsEventDispatcher dispatcher, @NotNull JpsLibrary element) {
    dispatcher.getPublisher(JpsLibraryListener.class).libraryAdded(element);
  }

  @Override
  public void fireElementRemoved(@NotNull JpsEventDispatcher dispatcher, @NotNull JpsLibrary element) {
    dispatcher.getPublisher(JpsLibraryListener.class).libraryRemoved(element);
  }
}
