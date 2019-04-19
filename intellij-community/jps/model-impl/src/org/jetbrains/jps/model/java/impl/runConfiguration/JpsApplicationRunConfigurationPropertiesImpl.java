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
package org.jetbrains.jps.model.java.impl.runConfiguration;

import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.java.runConfiguration.JpsApplicationRunConfigurationProperties;
import org.jetbrains.jps.model.java.runConfiguration.JpsApplicationRunConfigurationState;

/**
 * @author nik
 */
public class JpsApplicationRunConfigurationPropertiesImpl extends JpsElementBase<JpsApplicationRunConfigurationPropertiesImpl> implements JpsApplicationRunConfigurationProperties {
  private final JpsApplicationRunConfigurationState myState;

  public JpsApplicationRunConfigurationPropertiesImpl(JpsApplicationRunConfigurationState state) {
    myState = state;
  }

  @NotNull
  @Override
  public JpsApplicationRunConfigurationPropertiesImpl createCopy() {
    return new JpsApplicationRunConfigurationPropertiesImpl(XmlSerializerUtil.createCopy(myState));
  }

  @Override
  public void applyChanges(@NotNull JpsApplicationRunConfigurationPropertiesImpl modified) {
    XmlSerializerUtil.copyBean(modified.myState, myState);
  }

  @Override
  public String getMainClass() {
    return myState.MAIN_CLASS_NAME;
  }

  @Override
  public void setMainClass(String value) {
    myState.MAIN_CLASS_NAME = value;
  }
}
