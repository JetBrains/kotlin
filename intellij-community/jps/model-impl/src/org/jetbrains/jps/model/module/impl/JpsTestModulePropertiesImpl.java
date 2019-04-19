/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.VolatileNullableLazyValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleReference;
import org.jetbrains.jps.model.module.JpsTestModuleProperties;

/**
 * @author nik
 */
public class JpsTestModulePropertiesImpl extends JpsCompositeElementBase<JpsTestModulePropertiesImpl> implements JpsTestModuleProperties {
  public static final JpsElementChildRole<JpsTestModuleProperties> ROLE = JpsElementChildRoleBase.create("test module properties");
  private static final JpsElementChildRole<JpsModuleReference> MODULE_REFERENCE_CHILD_ROLE = JpsElementChildRoleBase.create("production module reference");
  private final NullableLazyValue<JpsModule> myCachedProductionModule = new VolatileNullableLazyValue<JpsModule>() {
    @Nullable
    @Override
    protected JpsModule compute() {
      return getProductionModuleReference().resolve();
    }
  };

  public JpsTestModulePropertiesImpl(@NotNull JpsModuleReference productionModuleReference) {
    myContainer.setChild(MODULE_REFERENCE_CHILD_ROLE, productionModuleReference);
  }

  private JpsTestModulePropertiesImpl(JpsTestModulePropertiesImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsModuleReference getProductionModuleReference() {
    return myContainer.getChild(MODULE_REFERENCE_CHILD_ROLE);
  }

  @Nullable
  @Override
  public JpsModule getProductionModule() {
    return myCachedProductionModule.getValue();
  }


  @NotNull
  @Override
  public JpsTestModulePropertiesImpl createCopy() {
    return new JpsTestModulePropertiesImpl(this);
  }
}
