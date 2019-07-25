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
import org.jetbrains.jps.model.JpsEventDispatcher;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;

/**
 * @author nik
 */
public abstract class JpsRootElementBase<E extends JpsRootElementBase<E>> extends JpsCompositeElementBase<E> {
  private final JpsModel myModel;
  private final JpsEventDispatcher myEventDispatcher;

  protected JpsRootElementBase(@NotNull JpsModel model, JpsEventDispatcher eventDispatcher) {
    super();
    myModel = model;
    myEventDispatcher = eventDispatcher;
  }

  protected JpsRootElementBase(JpsCompositeElementBase<E> original, JpsModel model, JpsEventDispatcher dispatcher) {
    super(original);
    myModel = model;
    myEventDispatcher = dispatcher;
  }

  @Override
  protected JpsEventDispatcher getEventDispatcher() {
    return myEventDispatcher;
  }

  @NotNull
  @Override
  public JpsModel getModel() {
    return myModel;
  }

  @NotNull
  @Override
  public E createCopy() {
    throw new UnsupportedOperationException("'createCopy' not implemented in " + getClass().getName());
  }
}
