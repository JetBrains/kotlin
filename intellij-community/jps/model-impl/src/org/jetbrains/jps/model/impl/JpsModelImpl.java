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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementReference;
import org.jetbrains.jps.model.JpsEventDispatcher;
import org.jetbrains.jps.model.JpsModel;

/**
 * @author nik
 */
public class JpsModelImpl implements JpsModel {
  private final JpsProjectImpl myProject;
  private final JpsGlobalImpl myGlobal;
  private JpsModelImpl myOriginalModel;
  private final JpsEventDispatcher myEventDispatcher;

  public JpsModelImpl(JpsEventDispatcher eventDispatcher) {
    myEventDispatcher = eventDispatcher;
    myProject = new JpsProjectImpl(this, eventDispatcher);
    myGlobal = new JpsGlobalImpl(this, eventDispatcher);
  }

  private JpsModelImpl(JpsModelImpl original, JpsEventDispatcher eventDispatcher) {
    myOriginalModel = original;
    myEventDispatcher = eventDispatcher;
    myProject = new JpsProjectImpl(original.myProject, this, eventDispatcher);
    myGlobal = new JpsGlobalImpl(original.myGlobal, this, eventDispatcher);
  }

  @Override
  @NotNull
  public JpsProjectImpl getProject() {
    return myProject;
  }

  @Override
  @NotNull
  public JpsGlobalImpl getGlobal() {
    return myGlobal;
  }

  @NotNull
  @Override
  public JpsModel createModifiableModel(@NotNull JpsEventDispatcher eventDispatcher) {
    return new JpsModelImpl(this, eventDispatcher);
  }

  @Override
  public void registerExternalReference(@NotNull JpsElementReference<?> reference) {
    myProject.addExternalReference(reference);
  }

  @Override
  public void commit() {
    myOriginalModel.applyChanges(this);
  }

  private void applyChanges(@NotNull JpsModelImpl modifiedCopy) {
    myProject.applyChanges(modifiedCopy.myProject);
    myGlobal.applyChanges(modifiedCopy.myGlobal);
  }
}
