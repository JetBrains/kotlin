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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsCompositeElement;
import org.jetbrains.jps.model.JpsElementCollection;
import org.jetbrains.jps.model.JpsElementReference;
import org.jetbrains.jps.model.JpsNamedElement;
import org.jetbrains.jps.model.ex.JpsElementCollectionRole;

/**
 * @author nik
 */
public abstract class JpsNamedElementReferenceImpl<T extends JpsNamedElement, Self extends JpsNamedElementReferenceImpl<T, Self>> extends JpsNamedElementReferenceBase<T, T, Self> {
  protected final JpsElementCollectionRole<? extends T> myCollectionRole;

  protected JpsNamedElementReferenceImpl(@NotNull JpsElementCollectionRole<? extends T> role, @NotNull String elementName,
                                         @NotNull JpsElementReference<? extends JpsCompositeElement> parentReference) {
    super(elementName, parentReference);
    myCollectionRole = role;
  }

  protected JpsNamedElementReferenceImpl(JpsNamedElementReferenceImpl<T, Self> original) {
    super(original);
    myCollectionRole = original.myCollectionRole;
  }

  @Override
  protected T resolve(T element) {
    return element;
  }

  @Override
  @Nullable
  protected JpsElementCollection<? extends T> getCollection(@NotNull JpsCompositeElement parent) {
    return parent.getContainer().getChild(myCollectionRole);
  }
}
