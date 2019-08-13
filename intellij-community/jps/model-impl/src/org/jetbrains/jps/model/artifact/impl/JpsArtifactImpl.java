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
package org.jetbrains.jps.model.artifact.impl;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.artifact.JpsArtifactType;
import org.jetbrains.jps.model.artifact.elements.JpsArchivePackagingElement;
import org.jetbrains.jps.model.artifact.elements.JpsCompositePackagingElement;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.ex.JpsNamedCompositeElementBase;

/**
 * @author nik
 */
public class JpsArtifactImpl<P extends JpsElement> extends JpsNamedCompositeElementBase<JpsArtifactImpl<P>> implements JpsArtifact {
  private static final JpsElementChildRole<JpsCompositePackagingElement>
    ROOT_ELEMENT_CHILD_ROLE = JpsElementChildRoleBase.create("root element");
  private final JpsArtifactType<P> myArtifactType;
  private String myOutputPath;
  private boolean myBuildOnMake;


  public JpsArtifactImpl(@NotNull String name, @NotNull JpsCompositePackagingElement rootElement, @NotNull JpsArtifactType<P> type, @NotNull P properties) {
    super(name);
    myArtifactType = type;
    myContainer.setChild(ROOT_ELEMENT_CHILD_ROLE, rootElement);
    myContainer.setChild(type.getPropertiesRole(), properties);
  }

  private JpsArtifactImpl(JpsArtifactImpl<P> original) {
    super(original);
    myArtifactType = original.myArtifactType;
    myOutputPath = original.myOutputPath;
  }

  @NotNull
  @Override
  public JpsArtifactImpl<P> createCopy() {
    return new JpsArtifactImpl<>(this);
  }

  @Override
  public String getOutputPath() {
    return myOutputPath;
  }

  @Override
  public void setOutputPath(@Nullable String outputPath) {
    if (!Comparing.equal(myOutputPath, outputPath)) {
      myOutputPath = outputPath;
      fireElementChanged();
    }
  }

  @Nullable
  @Override
  public String getOutputFilePath() {
    if (StringUtil.isEmpty(myOutputPath)) return null;
    JpsCompositePackagingElement root = getRootElement();
    return root instanceof JpsArchivePackagingElement ? myOutputPath + "/" + ((JpsArchivePackagingElement)root).getArchiveName() : myOutputPath;
  }

  @NotNull
  @Override
  public JpsArtifactType<P> getArtifactType() {
    return myArtifactType;
  }

  @NotNull
  @Override
  public JpsArtifactReferenceImpl createReference() {
    return new JpsArtifactReferenceImpl(getName());
  }

  @NotNull
  @Override
  public JpsCompositePackagingElement getRootElement() {
    return myContainer.getChild(ROOT_ELEMENT_CHILD_ROLE);
  }

  @Override
  public void setRootElement(@NotNull JpsCompositePackagingElement rootElement) {
    myContainer.setChild(ROOT_ELEMENT_CHILD_ROLE, rootElement);
  }

  @Override
  public P getProperties() {
    return myContainer.getChild(myArtifactType.getPropertiesRole());
  }

  @Override
  public boolean isBuildOnMake() {
    return myBuildOnMake;
  }

  @Override
  public void setBuildOnMake(boolean buildOnMake) {
    if (myBuildOnMake != buildOnMake) {
      myBuildOnMake = buildOnMake;
      fireElementChanged();
    }
  }
}
