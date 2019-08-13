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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.library.*;
import org.jetbrains.jps.model.library.impl.JpsLibraryCollectionImpl;
import org.jetbrains.jps.model.library.impl.JpsLibraryRole;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;

/**
 * @author nik
 */
public class JpsGlobalImpl extends JpsRootElementBase<JpsGlobalImpl> implements JpsGlobal {
  private final JpsLibraryCollectionImpl myLibraryCollection;

  public JpsGlobalImpl(@NotNull JpsModel model, JpsEventDispatcher eventDispatcher) {
    super(model, eventDispatcher);
    myLibraryCollection = new JpsLibraryCollectionImpl(myContainer.setChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE));
    myContainer.setChild(JpsFileTypesConfigurationImpl.ROLE, new JpsFileTypesConfigurationImpl());
  }

  public JpsGlobalImpl(JpsGlobalImpl original, JpsModel model, JpsEventDispatcher eventDispatcher) {
    super(original, model, eventDispatcher);
    myLibraryCollection = new JpsLibraryCollectionImpl(myContainer.getChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE));
  }

  @NotNull
  @Override
  public
  <P extends JpsElement, LibraryType extends JpsLibraryType<P> & JpsElementTypeWithDefaultProperties<P>>
  JpsLibrary addLibrary(@NotNull LibraryType libraryType, @NotNull final String name) {
    return myLibraryCollection.addLibrary(name, libraryType);
  }

  @Override
  public <P extends JpsElement> JpsTypedLibrary<JpsSdk<P>> addSdk(@NotNull String name, @Nullable String homePath,
                                                                  @Nullable String versionString, @NotNull JpsSdkType<P> type,
                                                                  @NotNull P properties) {
    JpsTypedLibrary<JpsSdk<P>> sdk = JpsElementFactory.getInstance().createSdk(name, homePath, versionString, type, properties);
    myLibraryCollection.addLibrary(sdk);
    return sdk;
  }

  @Override
  public <P extends JpsElement, SdkType extends JpsSdkType<P> & JpsElementTypeWithDefaultProperties<P>> JpsTypedLibrary<JpsSdk<P>>
  addSdk(@NotNull String name, @Nullable String homePath, @Nullable String versionString, @NotNull SdkType type) {
    return addSdk(name, homePath, versionString, type, type.createDefaultProperties());
  }

  @NotNull
  @Override
  public JpsLibraryCollection getLibraryCollection() {
    return myLibraryCollection;
  }

  @NotNull
  @Override
  public JpsFileTypesConfiguration getFileTypesConfiguration() {
    return myContainer.getChild(JpsFileTypesConfigurationImpl.ROLE);
  }

  @NotNull
  @Override
  public JpsElementReference<JpsGlobal> createReference() {
    return new JpsGlobalElementReference();
  }
}
