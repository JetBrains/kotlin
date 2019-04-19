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
package org.jetbrains.jps.model;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JpsJavaDependencyExtension;
import org.jetbrains.jps.model.java.JpsJavaDependencyScope;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsSdkDependency;

/**
 * @author nik
 */
public class JpsModuleRootModificationUtil {
  public static void addDependency(JpsModule module, JpsLibrary library) {
    addDependency(module, library, JpsJavaDependencyScope.COMPILE, false);
  }

  public static void addDependency(JpsModule module, JpsLibrary library, final JpsJavaDependencyScope scope, final boolean exported) {
    setDependencyProperties(module.getDependenciesList().addLibraryDependency(library), scope, exported);
  }

  private static void setDependencyProperties(JpsDependencyElement dependency, JpsJavaDependencyScope scope, boolean exported) {
    JpsJavaDependencyExtension extension = JpsJavaExtensionService.getInstance().getOrCreateDependencyExtension(dependency);
    extension.setExported(exported);
    extension.setScope(scope);
  }

  public static void setModuleSdk(JpsModule module, @Nullable JpsSdk<JpsDummyElement> sdk) {
    module.getSdkReferencesTable().setSdkReference(JpsJavaSdkType.INSTANCE, sdk != null ? sdk.createReference() : null);
    addSdkDependencyIfNeeded(module);
  }

  private static void addSdkDependencyIfNeeded(JpsModule module) {
    for (JpsDependencyElement element : module.getDependenciesList().getDependencies()) {
      if (element instanceof JpsSdkDependency) return;
    }
    module.getDependenciesList().addSdkDependency(JpsJavaSdkType.INSTANCE);
  }

  public static void setSdkInherited(JpsModule module) {
    module.getSdkReferencesTable().setSdkReference(JpsJavaSdkType.INSTANCE, null);
  }

  public static void addDependency(final JpsModule from, final JpsModule to) {
    addDependency(from, to, JpsJavaDependencyScope.COMPILE, false);
  }

  public static void addDependency(final JpsModule from, final JpsModule to, final JpsJavaDependencyScope scope, final boolean exported) {
    setDependencyProperties(from.getDependenciesList().addModuleDependency(to), scope, exported);
  }
}
