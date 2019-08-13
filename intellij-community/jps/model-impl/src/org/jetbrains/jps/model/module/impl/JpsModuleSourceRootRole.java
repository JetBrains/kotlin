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
package org.jetbrains.jps.model.module.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsEventDispatcher;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.ex.JpsElementCollectionRole;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.model.module.JpsModuleSourceRootListener;

/**
 * @author nik
 */
public class JpsModuleSourceRootRole extends JpsElementChildRoleBase<JpsModuleSourceRoot> {
  private static final JpsModuleSourceRootRole INSTANCE = new JpsModuleSourceRootRole();
  public static final JpsElementCollectionRole<JpsModuleSourceRoot> ROOT_COLLECTION_ROLE = JpsElementCollectionRole.create(INSTANCE);

  private JpsModuleSourceRootRole() {
    super("module source root");
  }

  @Override
  public void fireElementAdded(@NotNull JpsEventDispatcher dispatcher, @NotNull JpsModuleSourceRoot element) {
    dispatcher.getPublisher(JpsModuleSourceRootListener.class).sourceRootAdded(element);
  }

  @Override
  public void fireElementRemoved(@NotNull JpsEventDispatcher dispatcher, @NotNull JpsModuleSourceRoot element) {
    dispatcher.getPublisher(JpsModuleSourceRootListener.class).sourceRootRemoved(element);
  }
}
