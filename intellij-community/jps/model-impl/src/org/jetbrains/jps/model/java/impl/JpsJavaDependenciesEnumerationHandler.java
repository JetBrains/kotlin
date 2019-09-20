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

import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.service.JpsServiceManager;

import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public abstract class JpsJavaDependenciesEnumerationHandler {
  public static List<JpsJavaDependenciesEnumerationHandler> createHandlers(Collection<JpsModule> rootModules) {
    List<JpsJavaDependenciesEnumerationHandler> handlers = null;
    for (Factory factory : JpsServiceManager.getInstance().getExtensions(Factory.class)) {
      JpsJavaDependenciesEnumerationHandler handler = factory.createHandler(rootModules);
      if (handler != null) {
        if (handlers == null) {
          handlers = new SmartList<>();
        }
        handlers.add(handler);
      }
    }
    return handlers;
  }

  public static boolean shouldProcessDependenciesRecursively(final List<? extends JpsJavaDependenciesEnumerationHandler> handlers) {
    if (handlers != null) {
      for (JpsJavaDependenciesEnumerationHandler handler : handlers) {
        if (!handler.shouldProcessDependenciesRecursively()) {
          return false;
        }
      }
    }
    return true;
  }

  public static abstract class Factory {

    @Nullable
    public abstract JpsJavaDependenciesEnumerationHandler createHandler(@NotNull Collection<JpsModule> modules);
  }
  public boolean shouldAddRuntimeDependenciesToTestCompilationClasspath() {
    return false;
  }

  public boolean isProductionOnTestsDependency(JpsDependencyElement element) {
    return false;
  }

  public boolean shouldIncludeTestsFromDependentModulesToTestClasspath() {
    return true;
  }

  public boolean shouldProcessDependenciesRecursively() {
    return true;
  }
}
