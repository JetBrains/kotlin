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
package org.jetbrains.jps.gradle.model.impl.artifacts;

import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gradle.model.artifacts.JpsGradleArtifactExtension;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

/**
 * @author Vladislav.Soroka
 */
public class JpsGradleArtifactExtensionImpl extends JpsCompositeElementBase<JpsGradleArtifactExtensionImpl>
  implements JpsGradleArtifactExtension {

  public static final JpsElementChildRole<JpsGradleArtifactExtension> ROLE =
    JpsElementChildRoleBase.create("gradle-properties");
  private final GradleArtifactExtensionProperties myProperties;

  public JpsGradleArtifactExtensionImpl(GradleArtifactExtensionProperties properties) {
    myProperties = properties;
  }

  private JpsGradleArtifactExtensionImpl(JpsGradleArtifactExtensionImpl original) {
    super(original);
    myProperties = XmlSerializerUtil.createCopy(original.myProperties);
  }

  @NotNull
  @Override
  public JpsGradleArtifactExtensionImpl createCopy() {
    return new JpsGradleArtifactExtensionImpl(this);
  }

  @Override
  public GradleArtifactExtensionProperties getProperties() {
    return myProperties;
  }

  private JpsArtifact getArtifact() {
    return (JpsArtifact)myParent;
  }

}
