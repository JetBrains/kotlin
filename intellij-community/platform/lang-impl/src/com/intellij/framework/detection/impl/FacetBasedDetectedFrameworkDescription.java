/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.framework.detection.impl;

import com.intellij.facet.*;
import com.intellij.framework.detection.DetectedFrameworkDescription;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FrameworkDetector;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.UniqueNameGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * @author nik
 */
public abstract class FacetBasedDetectedFrameworkDescription<F extends Facet, C extends FacetConfiguration> extends DetectedFrameworkDescription {
  private static final Logger LOG = Logger.getInstance("#com.intellij.framework.detection.impl.FacetBasedDetectedFrameworkDescription");
  private final FacetBasedFrameworkDetector<F, C> myDetector;
  private final C myConfiguration;
  private final Set<VirtualFile> myRelatedFiles;
  private final FacetType<F,C> myFacetType;

  public FacetBasedDetectedFrameworkDescription(FacetBasedFrameworkDetector<F, C> detector,
                                                @NotNull C configuration,
                                                Set<VirtualFile> files) {
    myDetector = detector;
    myConfiguration = configuration;
    myRelatedFiles = files;
    myFacetType = detector.getFacetType();
  }

  @NotNull
  @Override
  public Collection<? extends VirtualFile> getRelatedFiles() {
    return myRelatedFiles;
  }

  public C getConfiguration() {
    return myConfiguration;
  }

  @NotNull
  @Override
  public String getSetupText() {
    return "'" + myFacetType.getPresentableName() + "' facet will be added to '" + getModuleName() + "' module";
  }

  @NotNull
  @Override
  public FrameworkDetector getDetector() {
    return myDetector;
  }

  protected abstract String getModuleName();

  @Override
  public boolean canSetupFramework(@NotNull Collection<? extends DetectedFrameworkDescription> allDetectedFrameworks) {
    final FacetTypeId<?> underlyingId = myFacetType.getUnderlyingFacetType();
    if (underlyingId == null) {
      return true;
    }

    final Collection<? extends Facet> facets = getExistentFacets(underlyingId);
    for (Facet facet : facets) {
      if (myDetector.isSuitableUnderlyingFacetConfiguration(facet.getConfiguration(), myConfiguration, myRelatedFiles)) {
        return true;
      }
    }
    for (DetectedFrameworkDescription framework : allDetectedFrameworks) {
      if (framework instanceof FacetBasedDetectedFrameworkDescription<?, ?>) {
        final FacetBasedDetectedFrameworkDescription<?, ?> description = (FacetBasedDetectedFrameworkDescription<?, ?>)framework;
        if (underlyingId.equals(description.myFacetType.getId()) &&
            myDetector.isSuitableUnderlyingFacetConfiguration(description.getConfiguration(), myConfiguration, myRelatedFiles)) {
          return true;
        }
      }
    }
    return false;
  }

  @NotNull
  protected abstract Collection<? extends Facet> getExistentFacets(FacetTypeId<?> underlyingFacetType);

  protected void doSetup(ModifiableModelsProvider modifiableModelsProvider, final Module module) {
    final ModifiableFacetModel model = modifiableModelsProvider.getFacetModifiableModel(module);
    final String name = UniqueNameGenerator.generateUniqueName(myFacetType.getDefaultFacetName(),
                                                               s -> FacetManager.getInstance(module).findFacet(myFacetType.getId(), s) == null);
    final F facet = FacetManager.getInstance(module).createFacet(myFacetType, name, myConfiguration,
                                                                 findUnderlyingFacet(module));
    model.addFacet(facet);
    modifiableModelsProvider.commitFacetModifiableModel(module, model);
    final ModifiableRootModel rootModel = modifiableModelsProvider.getModuleModifiableModel(module);
    myDetector.setupFacet(facet, rootModel);
    modifiableModelsProvider.commitModuleModifiableModel(rootModel);
  }

  @Nullable
  private Facet findUnderlyingFacet(Module module) {
    final FacetTypeId<?> underlyingTypeId = myFacetType.getUnderlyingFacetType();
    if (underlyingTypeId == null) return null;

    final Collection<? extends Facet> parentFacets = FacetManager.getInstance(module).getFacetsByType(underlyingTypeId);
    for (Facet facet : parentFacets) {
      if (myDetector.isSuitableUnderlyingFacetConfiguration(facet.getConfiguration(), myConfiguration, myRelatedFiles)) {
        return facet;
      }
    }
    LOG.error("Cannot find suitable underlying facet in " + parentFacets);
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof FacetBasedDetectedFrameworkDescription)) {
      return false;
    }
    final FacetBasedDetectedFrameworkDescription other = (FacetBasedDetectedFrameworkDescription)obj;
    return getModuleName().equals(other.getModuleName()) && myFacetType.equals(other.myFacetType) && myRelatedFiles.equals(other.myRelatedFiles);
  }

  @Override
  public int hashCode() {
    return getModuleName().hashCode() + 31*myFacetType.hashCode() + 239*myRelatedFiles.hashCode();
  }
}
