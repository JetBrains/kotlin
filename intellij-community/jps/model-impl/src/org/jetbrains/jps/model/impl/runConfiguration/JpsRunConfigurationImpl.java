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
package org.jetbrains.jps.model.impl.runConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.ex.JpsNamedCompositeElementBase;
import org.jetbrains.jps.model.runConfiguration.JpsRunConfigurationType;
import org.jetbrains.jps.model.runConfiguration.JpsTypedRunConfiguration;

/**
 * @author nik
 */
public class JpsRunConfigurationImpl<P extends JpsElement> extends JpsNamedCompositeElementBase<JpsRunConfigurationImpl<P>> implements
                                                                                                                            JpsTypedRunConfiguration<P> {
  private final JpsRunConfigurationType<P> myType;
  
  public JpsRunConfigurationImpl(@NotNull String name, JpsRunConfigurationType<P> type, P properties) {
    super(name);
    myType = type;
    myContainer.setChild(myType.getPropertiesRole(), properties);
  }

  private JpsRunConfigurationImpl(JpsRunConfigurationImpl<P> original) {
    super(original);
    myType = original.myType;
  }

  @NotNull
  @Override
  public JpsRunConfigurationImpl<P> createCopy() {
    return new JpsRunConfigurationImpl<>(this);
  }

  @NotNull
  @Override
  public P getProperties() {
    return myContainer.getChild(myType.getPropertiesRole());
  }

  @Override
  public JpsRunConfigurationType<P> getType() {
    return myType;
  }
}
