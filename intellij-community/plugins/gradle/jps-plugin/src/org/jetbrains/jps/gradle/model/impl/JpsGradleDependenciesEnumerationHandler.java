/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.jps.gradle.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gradle.model.JpsGradleExtensionService;
import org.jetbrains.jps.gradle.model.JpsGradleModuleExtension;
import org.jetbrains.jps.model.java.impl.JpsJavaDependenciesEnumerationHandler;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;

import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class JpsGradleDependenciesEnumerationHandler extends JpsJavaDependenciesEnumerationHandler {
  private static final JpsGradleDependenciesEnumerationHandler SOURCE_SET_TYPE_INSTANCE = new JpsGradleDependenciesEnumerationHandler(true);
  private static final JpsGradleDependenciesEnumerationHandler NON_SOURCE_SET_TYPE_INSTANCE =
    new JpsGradleDependenciesEnumerationHandler(false);

  private final boolean myResolveModulePerSourceSet;

  public JpsGradleDependenciesEnumerationHandler(boolean resolveModulePerSourceSet) {
    myResolveModulePerSourceSet = resolveModulePerSourceSet;
  }

  @Override
  public boolean shouldAddRuntimeDependenciesToTestCompilationClasspath() {
    return myResolveModulePerSourceSet;
  }

  @Override
  public boolean isProductionOnTestsDependency(JpsDependencyElement element) {
    return JpsGradleExtensionService.getInstance().isProductionOnTestDependency(element);
  }

  @Override
  public boolean shouldIncludeTestsFromDependentModulesToTestClasspath() {
    return !myResolveModulePerSourceSet;
  }

  @Override
  public boolean shouldProcessDependenciesRecursively() {
    return !myResolveModulePerSourceSet;
  }

  public static class GradleFactory extends Factory {
    @Nullable
    @Override
    public JpsJavaDependenciesEnumerationHandler createHandler(@NotNull Collection<JpsModule> modules) {
      JpsGradleExtensionService service = JpsGradleExtensionService.getInstance();
      JpsJavaDependenciesEnumerationHandler handler = null;
      for (JpsModule module : modules) {
        JpsGradleModuleExtension gradleModuleExtension = service.getExtension(module);
        if (gradleModuleExtension != null) {
          if (JpsGradleModuleExtension.GRADLE_SOURCE_SET_MODULE_TYPE_KEY.equals(gradleModuleExtension.getModuleType())) {
            handler = SOURCE_SET_TYPE_INSTANCE;
            break;
          }
          else {
            handler = NON_SOURCE_SET_TYPE_INSTANCE;
          }
        }
      }
      return handler;
    }
  }
}
