// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.java.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.java.JpsProductionModuleSourcePackagingElement;
import org.jetbrains.jps.model.module.JpsModuleReference;

public class JpsProductionModuleSourcePackagingElementImpl extends JpsCompositeElementBase<JpsProductionModuleSourcePackagingElementImpl>
  implements JpsProductionModuleSourcePackagingElement {

  private static final JpsElementChildRole<JpsModuleReference>
    MODULE_REFERENCE_CHILD_ROLE = JpsElementChildRoleBase.create("module reference");

  public JpsProductionModuleSourcePackagingElementImpl(JpsModuleReference moduleReference) {
    myContainer.setChild(MODULE_REFERENCE_CHILD_ROLE, moduleReference);
  }

  @Override
  @NotNull
  public JpsModuleReference getModuleReference() {
    return myContainer.getChild(MODULE_REFERENCE_CHILD_ROLE);
  }

  private JpsProductionModuleSourcePackagingElementImpl(JpsProductionModuleSourcePackagingElementImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsProductionModuleSourcePackagingElementImpl createCopy() {
    return new JpsProductionModuleSourcePackagingElementImpl(this);
  }

}
