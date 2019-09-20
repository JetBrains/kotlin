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
package org.jetbrains.jps.model.artifact.impl.elements;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.artifact.elements.JpsCompositePackagingElement;
import org.jetbrains.jps.model.artifact.elements.JpsPackagingElement;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.ex.JpsElementCollectionRole;

import java.util.List;

/**
 * @author nik
 */
public abstract class JpsCompositePackagingElementBase<Self extends JpsCompositePackagingElementBase<Self>> extends JpsCompositeElementBase<Self>
  implements JpsCompositePackagingElement {
  private static final JpsElementCollectionRole<JpsPackagingElement> CHILDREN_ROLE = JpsElementCollectionRole.create(JpsElementChildRoleBase.create("child"));

  protected JpsCompositePackagingElementBase() {
    myContainer.setChild(CHILDREN_ROLE);
  }

  protected JpsCompositePackagingElementBase(JpsCompositePackagingElementBase<Self> original) {
    super(original);
  }

  @NotNull
  @Override
  public List<JpsPackagingElement> getChildren() {
    return myContainer.getChild(CHILDREN_ROLE).getElements();
  }


  @Override
  public <E extends JpsPackagingElement> E addChild(@NotNull E child) {
    return myContainer.getChild(CHILDREN_ROLE).addChild(child);
  }

  @Override
  public void removeChild(@NotNull JpsPackagingElement child) {
    myContainer.getChild(CHILDREN_ROLE).removeChild(child);
  }
}
