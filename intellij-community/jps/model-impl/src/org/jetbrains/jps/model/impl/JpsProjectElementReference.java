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
import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.ex.JpsElementReferenceBase;

/**
 * @author nik
 */
public class JpsProjectElementReference extends JpsElementReferenceBase<JpsProjectElementReference, JpsProject> {
  @Override
  public JpsProject resolve() {
    final JpsModel model = getModel();
    return model != null ? model.getProject() : null;
  }

  @NotNull
  @Override
  public JpsProjectElementReference createCopy() {
    return new JpsProjectElementReference();
  }

  @Override
  public void applyChanges(@NotNull JpsProjectElementReference modified) {
  }

  @Override
  public String toString() {
    return "project ref";
  }
}
