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
import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.ex.JpsElementBase;

/**
 * @author nik
 */
public class JpsDummyElementImpl extends JpsElementBase<JpsDummyElementImpl> implements JpsDummyElement {
  @NotNull
  @Override
  public JpsDummyElementImpl createCopy() {
    return new JpsDummyElementImpl();
  }

  @Override
  public void applyChanges(@NotNull JpsDummyElementImpl modified) {
  }
}
