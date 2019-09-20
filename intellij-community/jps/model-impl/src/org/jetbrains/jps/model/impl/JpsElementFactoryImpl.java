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
import org.jetbrains.jps.model.library.JpsLibraryReference;
import org.jetbrains.jps.model.library.JpsLibraryType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.impl.JpsLibraryImpl;
import org.jetbrains.jps.model.library.impl.JpsLibraryReferenceImpl;
import org.jetbrains.jps.model.library.impl.JpsSdkReferenceImpl;
import org.jetbrains.jps.model.library.impl.sdk.JpsSdkImpl;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.module.*;
import org.jetbrains.jps.model.module.impl.JpsModuleImpl;
import org.jetbrains.jps.model.module.impl.JpsModuleReferenceImpl;
import org.jetbrains.jps.model.module.impl.JpsModuleSourceRootImpl;

/**
 * @author nik
 */
public class JpsElementFactoryImpl extends JpsElementFactory {
  @Override
  public JpsModel createModel() {
    return new JpsModelImpl(new JpsEventDispatcherBase() {
      @Override
      public void fireElementRenamed(@NotNull JpsNamedElement element, @NotNull String oldName, @NotNull String newName) {
      }

      @Override
      public void fireElementChanged(@NotNull JpsElement element) {
      }
    });
  }

  @Override
  public <P extends JpsElement> JpsModule createModule(@NotNull String name, @NotNull JpsModuleType<P> type, @NotNull P properties) {
    return new JpsModuleImpl<>(type, name, properties);
  }


  @Override
  public <P extends JpsElement> JpsTypedLibrary<P> createLibrary(@NotNull String name,
                                                                   @NotNull JpsLibraryType<P> type,
                                                                   @NotNull P properties) {
    return new JpsLibraryImpl<>(name, type, properties);
  }

  @Override
  public <P extends JpsElement> JpsTypedLibrary<JpsSdk<P>> createSdk(@NotNull String name, @Nullable String homePath,
                                                                     @Nullable String versionString, @NotNull JpsSdkType<P> type,
                                                                     @NotNull P properties) {
    return createLibrary(name, type, new JpsSdkImpl<>(homePath, versionString, type, properties));
  }

  @NotNull
  @Override
  public <P extends JpsElement> JpsModuleSourceRoot createModuleSourceRoot(@NotNull String url,
                                                                           @NotNull JpsModuleSourceRootType<P> type,
                                                                           @NotNull P properties) {
    return new JpsModuleSourceRootImpl<>(url, type, properties);
  }

  @NotNull
  @Override
  public JpsModuleReference createModuleReference(@NotNull String moduleName) {
    return new JpsModuleReferenceImpl(moduleName);
  }

  @NotNull
  @Override
  public JpsLibraryReference createLibraryReference(@NotNull String libraryName,
                                                    @NotNull JpsElementReference<? extends JpsCompositeElement> parentReference) {
    return new JpsLibraryReferenceImpl(libraryName, parentReference);
  }

  @NotNull
  @Override
  public <P extends JpsElement> JpsSdkReference<P> createSdkReference(@NotNull String sdkName, @NotNull JpsSdkType<P> sdkType) {
    return new JpsSdkReferenceImpl<>(sdkName, sdkType, createGlobalReference());
  }

  @NotNull
  @Override
  public JpsElementReference<JpsProject> createProjectReference() {
    return new JpsProjectElementReference();
  }

  @NotNull
  @Override
  public JpsElementReference<JpsGlobal> createGlobalReference() {
    return new JpsGlobalElementReference();
  }

  @NotNull
  @Override
  public JpsDummyElement createDummyElement() {
    return new JpsDummyElementImpl();
  }

  @NotNull
  @Override
  public <D> JpsSimpleElement<D> createSimpleElement(@NotNull D data) {
    return new JpsSimpleElementImpl<>(data);
  }
}
