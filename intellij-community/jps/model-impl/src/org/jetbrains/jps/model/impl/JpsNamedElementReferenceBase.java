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
import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

import java.util.List;

/**
 * @author nik
 */
public abstract class JpsNamedElementReferenceBase<S extends JpsNamedElement, T extends JpsNamedElement, Self extends JpsNamedElementReferenceBase<S, T, Self>>
  extends JpsCompositeElementBase<Self> implements JpsElementReference<T> {
  private static final JpsElementChildRole<JpsElementReference<? extends JpsCompositeElement>> PARENT_REFERENCE_ROLE = JpsElementChildRoleBase
    .create("parent");
  protected final String myElementName;

  protected JpsNamedElementReferenceBase(@NotNull String elementName, @NotNull JpsElementReference<? extends JpsCompositeElement> parentReference) {
    myElementName = elementName;
    myContainer.setChild(PARENT_REFERENCE_ROLE, parentReference);
  }

  protected JpsNamedElementReferenceBase(JpsNamedElementReferenceBase<S, T, Self> original) {
    super(original);
    myElementName = original.myElementName;
  }

  @Override
  public T resolve() {
    final JpsCompositeElement parent = getParentReference().resolve();
    if (parent == null) return null;

    JpsElementCollection<? extends S> collection = getCollection(parent);
    if (collection == null) return null;

    final List<? extends S> elements = collection.getElements();
    for (S element : elements) {
      if (element.getName().equals(myElementName)) {
        T resolved = resolve(element);
        if (resolved != null) {
          return resolved;
        }
      }
    }
    return null;
  }

  @Nullable
  protected abstract JpsElementCollection<? extends S> getCollection(@NotNull JpsCompositeElement parent);

  @Nullable
  protected abstract T resolve(S element);

  public JpsElementReference<? extends JpsCompositeElement> getParentReference() {
    return myContainer.getChild(PARENT_REFERENCE_ROLE);
  }

  @Override
  public JpsElementReference<T> asExternal(@NotNull JpsModel model) {
    model.registerExternalReference(this);
    return this;
  }
}
