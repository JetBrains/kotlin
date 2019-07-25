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
import org.jetbrains.jps.model.ex.JpsElementBase;

/**
 * @author nik
 */
public class JpsSimpleElementImpl<D> extends JpsElementBase<JpsSimpleElementImpl<D>> implements JpsSimpleElement<D> {
  private D myData;

  public JpsSimpleElementImpl(D data) {
    myData = data;
  }

  private JpsSimpleElementImpl(JpsSimpleElementImpl<D> original) {
    myData = original.myData;
  }

  @NotNull
  @Override
  public D getData() {
    return myData;
  }

  @Override
  public void setData(@NotNull D data) {
    if (!myData.equals(data)) {
      myData = data;
      fireElementChanged();
    }
  }

  @NotNull
  @Override
  public JpsSimpleElementImpl<D> createCopy() {
    return new JpsSimpleElementImpl<>(this);
  }

  @Override
  public void applyChanges(@NotNull JpsSimpleElementImpl<D> modified) {
    setData(modified.getData());
  }
}
