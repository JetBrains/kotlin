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

import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.ex.JpsElementCollectionRole;
import org.jetbrains.jps.model.impl.runConfiguration.JpsRunConfigurationImpl;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryCollection;
import org.jetbrains.jps.model.library.JpsLibraryType;
import org.jetbrains.jps.model.library.impl.JpsLibraryCollectionImpl;
import org.jetbrains.jps.model.library.impl.JpsLibraryRole;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleType;
import org.jetbrains.jps.model.module.JpsSdkReferencesTable;
import org.jetbrains.jps.model.module.JpsTypedModule;
import org.jetbrains.jps.model.module.impl.JpsModuleImpl;
import org.jetbrains.jps.model.module.impl.JpsModuleRole;
import org.jetbrains.jps.model.module.impl.JpsSdkReferencesTableImpl;
import org.jetbrains.jps.model.runConfiguration.JpsRunConfiguration;
import org.jetbrains.jps.model.runConfiguration.JpsRunConfigurationType;
import org.jetbrains.jps.model.runConfiguration.JpsTypedRunConfiguration;

import java.util.List;

/**
 * @author nik
 */
public class JpsProjectImpl extends JpsRootElementBase<JpsProjectImpl> implements JpsProject {
  private static final JpsElementCollectionRole<JpsElementReference<?>> EXTERNAL_REFERENCES_COLLECTION_ROLE =
    JpsElementCollectionRole.create(JpsElementChildRoleBase.create("external reference"));
  private static final JpsElementCollectionRole<JpsRunConfiguration> RUN_CONFIGURATIONS_ROLE = JpsElementCollectionRole.create(JpsElementChildRoleBase.create("run configuration"));
  private final JpsLibraryCollection myLibraryCollection;
  private String myName = "";

  public JpsProjectImpl(@NotNull JpsModel model, JpsEventDispatcher eventDispatcher) {
    super(model, eventDispatcher);
    myContainer.setChild(JpsModuleRole.MODULE_COLLECTION_ROLE);
    myContainer.setChild(EXTERNAL_REFERENCES_COLLECTION_ROLE);
    myContainer.setChild(JpsSdkReferencesTableImpl.ROLE);
    myContainer.setChild(RUN_CONFIGURATIONS_ROLE);
    myLibraryCollection = new JpsLibraryCollectionImpl(myContainer.setChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE));
  }

  public JpsProjectImpl(JpsProjectImpl original, JpsModel model, JpsEventDispatcher eventDispatcher) {
    super(original, model, eventDispatcher);
    myLibraryCollection = new JpsLibraryCollectionImpl(myContainer.getChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE));
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void setName(@NotNull String name) {
    if (!Comparing.equal(myName, name)) {
      myName = name;
      fireElementChanged();
    }
  }

  public void addExternalReference(@NotNull JpsElementReference<?> reference) {
    myContainer.getChild(EXTERNAL_REFERENCES_COLLECTION_ROLE).addChild(reference);
  }

  @NotNull
  @Override
  public
  <P extends JpsElement, ModuleType extends JpsModuleType<P> & JpsElementTypeWithDefaultProperties<P>>
  JpsModule addModule(@NotNull final String name, @NotNull ModuleType moduleType) {
    final JpsElementCollection<JpsModule> collection = myContainer.getChild(JpsModuleRole.MODULE_COLLECTION_ROLE);
    return collection.addChild(new JpsModuleImpl<>(moduleType, name, moduleType.createDefaultProperties()));
  }

  @NotNull
  @Override
  public <P extends JpsElement, LibraryType extends JpsLibraryType<P> & JpsElementTypeWithDefaultProperties<P>>
  JpsLibrary addLibrary(@NotNull String name, @NotNull LibraryType libraryType) {
    return myLibraryCollection.addLibrary(name, libraryType);
  }

  @NotNull
  @Override
  public List<JpsModule> getModules() {
    return myContainer.getChild(JpsModuleRole.MODULE_COLLECTION_ROLE).getElements();
  }

  @Override
  @NotNull
  public <P extends JpsElement> Iterable<JpsTypedModule<P>> getModules(JpsModuleType<P> type) {
    return myContainer.getChild(JpsModuleRole.MODULE_COLLECTION_ROLE).getElementsOfType(type);
  }
  
  @Override
  public void addModule(@NotNull JpsModule module) {
    myContainer.getChild(JpsModuleRole.MODULE_COLLECTION_ROLE).addChild(module);
  }

  @Override
  public void removeModule(@NotNull JpsModule module) {
    myContainer.getChild(JpsModuleRole.MODULE_COLLECTION_ROLE).removeChild(module);
  }

  @NotNull
  @Override
  public JpsLibraryCollection getLibraryCollection() {
    return myLibraryCollection;
  }

  @Override
  @NotNull
  public JpsSdkReferencesTable getSdkReferencesTable() {
    return myContainer.getChild(JpsSdkReferencesTableImpl.ROLE);
  }

  @NotNull
  @Override
  public <P extends JpsElement> Iterable<JpsTypedRunConfiguration<P>> getRunConfigurations(JpsRunConfigurationType<P> type) {
    return getRunConfigurationsCollection().getElementsOfType(type);
  }

  @NotNull
  @Override
  public List<JpsRunConfiguration> getRunConfigurations() {
    return getRunConfigurationsCollection().getElements();
  }

  @NotNull
  @Override
  public <P extends JpsElement> JpsTypedRunConfiguration<P> addRunConfiguration(@NotNull String name,
                                                                                @NotNull JpsRunConfigurationType<P> type,
                                                                                @NotNull P properties) {
    return getRunConfigurationsCollection().addChild(new JpsRunConfigurationImpl<>(name, type, properties));
  }

  private JpsElementCollection<JpsRunConfiguration> getRunConfigurationsCollection() {
    return myContainer.getChild(RUN_CONFIGURATIONS_ROLE);
  }

  @NotNull
  @Override
  public JpsElementReference<JpsProject> createReference() {
    return new JpsProjectElementReference();
  }
}
