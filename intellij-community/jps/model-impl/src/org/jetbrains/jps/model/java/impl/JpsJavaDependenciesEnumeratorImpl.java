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
package org.jetbrains.jps.model.java.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.*;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsSdkDependency;
import org.jetbrains.jps.model.module.impl.JpsDependenciesEnumeratorBase;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class JpsJavaDependenciesEnumeratorImpl extends JpsDependenciesEnumeratorBase<JpsJavaDependenciesEnumeratorImpl> implements JpsJavaDependenciesEnumerator {
  private boolean myProductionOnly;
  private boolean myRuntimeOnly;
  private boolean myCompileOnly;
  private boolean myExportedOnly;
  private boolean myRecursivelyExportedOnly;
  private JpsJavaClasspathKind myClasspathKind;
  private final List<JpsJavaDependenciesEnumerationHandler> myHandlers;

  public JpsJavaDependenciesEnumeratorImpl(Collection<JpsModule> rootModules) {
    super(rootModules);
    List<JpsJavaDependenciesEnumerationHandler> handlers = JpsJavaDependenciesEnumerationHandler.createHandlers(rootModules);
    myHandlers = handlers != null ? handlers : Collections.emptyList();
  }

  @NotNull
  @Override
  public JpsJavaDependenciesEnumerator productionOnly() {
    myProductionOnly = true;
    return this;
  }

  @NotNull
  @Override
  public JpsJavaDependenciesEnumerator compileOnly() {
    myCompileOnly = true;
    return this;
  }

  @NotNull
  @Override
  public JpsJavaDependenciesEnumerator runtimeOnly() {
    myRuntimeOnly = true;
    return this;
  }

  @NotNull
  @Override
  public JpsJavaDependenciesEnumerator exportedOnly() {
    if (myRecursively) {
      myRecursivelyExportedOnly = true;
    }
    else {
      myExportedOnly = true;
    }
    return this;
  }

  @NotNull
  @Override
  public JpsJavaDependenciesEnumerator recursivelyExportedOnly() {
    return recursively().exportedOnly();
  }

  @NotNull
  @Override
  public JpsJavaDependenciesEnumerator includedIn(@NotNull JpsJavaClasspathKind classpathKind) {
    myClasspathKind = classpathKind;
    return this;
  }

  @NotNull
  @Override
  public JpsJavaDependenciesRootsEnumerator classes() {
    return new JpsJavaDependenciesRootsEnumeratorImpl(this, JpsOrderRootType.COMPILED);
  }

  @NotNull
  @Override
  public JpsJavaDependenciesRootsEnumerator sources() {
    return new JpsJavaDependenciesRootsEnumeratorImpl(this, JpsOrderRootType.SOURCES);
  }

  @NotNull
  @Override
  public JpsJavaDependenciesRootsEnumerator annotations() {
    return new JpsJavaDependenciesRootsEnumeratorImpl(this, JpsAnnotationRootType.INSTANCE);
  }

  @Override
  protected JpsJavaDependenciesEnumeratorImpl self() {
    return this;
  }

  @Override
  protected boolean shouldProcessDependenciesRecursively() {
    return JpsJavaDependenciesEnumerationHandler.shouldProcessDependenciesRecursively(myHandlers);
  }

  @Override
  protected boolean shouldProcess(JpsModule module, JpsDependencyElement element) {
    boolean exported = !(element instanceof JpsSdkDependency);
    JpsJavaDependencyExtension extension = JpsJavaExtensionService.getInstance().getDependencyExtension(element);
    if (extension != null) {
      exported = extension.isExported();
      JpsJavaDependencyScope scope = extension.getScope();
      boolean forTestCompile = scope.isIncludedIn(JpsJavaClasspathKind.TEST_COMPILE) || scope == JpsJavaDependencyScope.RUNTIME &&
                                                                                        shouldAddRuntimeDependenciesToTestCompilationClasspath();
      if (myCompileOnly && !scope.isIncludedIn(JpsJavaClasspathKind.PRODUCTION_COMPILE) && !forTestCompile
        || myRuntimeOnly && !scope.isIncludedIn(JpsJavaClasspathKind.PRODUCTION_RUNTIME) && !scope.isIncludedIn(JpsJavaClasspathKind.TEST_RUNTIME)
        || myClasspathKind != null && !scope.isIncludedIn(myClasspathKind) && !(myClasspathKind == JpsJavaClasspathKind.TEST_COMPILE && forTestCompile)) {
        return false;
      }
      if (myProductionOnly) {
        if (!scope.isIncludedIn(JpsJavaClasspathKind.PRODUCTION_COMPILE) && !scope.isIncludedIn(JpsJavaClasspathKind.PRODUCTION_RUNTIME)
            || myCompileOnly && !scope.isIncludedIn(JpsJavaClasspathKind.PRODUCTION_COMPILE)
            || myRuntimeOnly && !scope.isIncludedIn(JpsJavaClasspathKind.PRODUCTION_RUNTIME)) {
          return false;
        }
      }
    }
    if (!exported) {
      if (myExportedOnly) return false;
      if ((myRecursivelyExportedOnly || element instanceof JpsSdkDependency) && !isEnumerationRootModule(module)) return false;
    }
    return true;
  }

  public boolean isProductionOnly() {
    return myProductionOnly || myClasspathKind == JpsJavaClasspathKind.PRODUCTION_RUNTIME || myClasspathKind == JpsJavaClasspathKind.PRODUCTION_COMPILE;
  }

  public boolean isProductionOnTests(JpsDependencyElement element) {
    for (JpsJavaDependenciesEnumerationHandler handler : myHandlers) {
      if (handler.isProductionOnTestsDependency(element)) {
        return true;
      }
    }
    return false;
  }

  public boolean shouldIncludeTestsFromDependentModulesToTestClasspath() {
    for (JpsJavaDependenciesEnumerationHandler handler : myHandlers) {
      if (!handler.shouldIncludeTestsFromDependentModulesToTestClasspath()) {
        return false;
      }
    }
    return true;
  }

  public boolean shouldAddRuntimeDependenciesToTestCompilationClasspath() {
    for (JpsJavaDependenciesEnumerationHandler handler : myHandlers) {
      if (handler.shouldAddRuntimeDependenciesToTestCompilationClasspath()) {
        return true;
      }
    }
    return false;
  }
}
