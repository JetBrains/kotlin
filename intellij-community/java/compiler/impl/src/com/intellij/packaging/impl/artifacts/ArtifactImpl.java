/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.packaging.impl.artifacts;

import com.intellij.openapi.roots.ProjectModelExternalSource;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public class ArtifactImpl extends UserDataHolderBase implements ModifiableArtifact {
  private CompositePackagingElement<?> myRootElement;
  private String myName;
  private boolean myBuildOnMake;
  private String myOutputPath;
  private final EventDispatcher<? extends ArtifactListener> myDispatcher;
  private ArtifactType myArtifactType;
  private Map<ArtifactPropertiesProvider, ArtifactProperties<?>> myProperties;
  private final ProjectModelExternalSource myExternalSource;

  public ArtifactImpl(@NotNull String name, @NotNull ArtifactType artifactType, boolean buildOnMake,
                      @NotNull CompositePackagingElement<?> rootElement, String outputPath,
                      @Nullable ProjectModelExternalSource externalSource) {
    this(name, artifactType, buildOnMake, rootElement, outputPath, externalSource, null);
  }

  public ArtifactImpl(@NotNull String name, @NotNull ArtifactType artifactType, boolean buildOnMake,
                      @NotNull CompositePackagingElement<?> rootElement, String outputPath,
                      @Nullable ProjectModelExternalSource externalSource, EventDispatcher<? extends ArtifactListener> dispatcher) {
    myName = name;
    myArtifactType = artifactType;
    myBuildOnMake = buildOnMake;
    myRootElement = rootElement;
    myOutputPath = outputPath;
    myDispatcher = dispatcher;
    myExternalSource = externalSource;
    myProperties = new HashMap<>();
    resetProperties();
  }

  private void resetProperties() {
    myProperties.clear();
    for (ArtifactPropertiesProvider provider : ArtifactPropertiesProvider.getProviders()) {
      if (provider.isAvailableFor(myArtifactType)) {
        myProperties.put(provider, provider.createProperties(myArtifactType));
      }
    }
  }

  @Override
  @NotNull
  public ArtifactType getArtifactType() {
    return myArtifactType;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public boolean isBuildOnMake() {
    return myBuildOnMake;
  }

  @Override
  @NotNull
  public CompositePackagingElement<?> getRootElement() {
    return myRootElement;
  }

  @Override
  public String getOutputPath() {
    return myOutputPath;
  }

  @Override
  public Collection<? extends ArtifactPropertiesProvider> getPropertiesProviders() {
    return Collections.unmodifiableCollection(myProperties.keySet());
  }

  @Nullable
  @Override
  public ProjectModelExternalSource getExternalSource() {
    return myExternalSource;
  }

  public ArtifactImpl createCopy(EventDispatcher<? extends ArtifactListener> dispatcher) {
    final ArtifactImpl artifact = new ArtifactImpl(myName, myArtifactType, myBuildOnMake, myRootElement, myOutputPath, myExternalSource,
                                                   dispatcher);
    for (Map.Entry<ArtifactPropertiesProvider, ArtifactProperties<?>> entry : myProperties.entrySet()) {
      final ArtifactProperties newProperties = artifact.myProperties.get(entry.getKey());
      //noinspection unchecked
      newProperties.loadState(entry.getValue().getState());
    }
    return artifact;
  }

  @Override
  public void setName(@NotNull String name) {
    String oldName = myName;
    myName = name;
    if (myDispatcher != null) {
      myDispatcher.getMulticaster().artifactChanged(this, oldName);
    }
  }

  @NonNls @Override
  public String toString() {
    return "artifact:" + myName;
  }

  @Override
  public void setRootElement(CompositePackagingElement<?> root) {
    myRootElement = root;
  }

  @Override
  public void setProperties(ArtifactPropertiesProvider provider, ArtifactProperties<?> properties) {
    if (properties != null) {
      myProperties.put(provider, properties);
    }
    else {
      myProperties.remove(provider);
    }
  }

  @Override
  public void setArtifactType(@NotNull ArtifactType selected) {
    myArtifactType = selected;
    resetProperties();
  }

  @Override
  public void setBuildOnMake(boolean buildOnMake) {
    myBuildOnMake = buildOnMake;
  }

  @Override
  public void setOutputPath(String outputPath) {
    myOutputPath = outputPath;
  }

  @Override
  public ArtifactProperties<?> getProperties(@NotNull ArtifactPropertiesProvider provider) {
    return myProperties.get(provider);
  }

  @Override
  public VirtualFile getOutputFile() {
    String filePath = getOutputFilePath();
    return !StringUtil.isEmpty(filePath) ? LocalFileSystem.getInstance().findFileByPath(filePath) : null;
  }

  @Override
  public String getOutputFilePath() {
    if (StringUtil.isEmpty(myOutputPath)) return null;

    String filePath;
    if (myRootElement instanceof ArchivePackagingElement) {
      filePath = myOutputPath + "/" + ((ArchivePackagingElement)myRootElement).getArchiveFileName();
    }
    else {
      filePath = myOutputPath;
    }
    return filePath;
  }

  public void copyFrom(ArtifactImpl modified) {
    myName = modified.getName();
    myOutputPath = modified.getOutputPath();
    myBuildOnMake = modified.isBuildOnMake();
    myRootElement = modified.getRootElement();
    myProperties = modified.myProperties;
    myArtifactType = modified.getArtifactType();
  }
}
