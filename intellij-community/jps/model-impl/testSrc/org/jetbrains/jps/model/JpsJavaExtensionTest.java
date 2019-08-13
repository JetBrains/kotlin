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

import org.jetbrains.jps.model.java.*;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsLibraryDependency;
import org.jetbrains.jps.model.module.JpsModule;

import java.util.List;

/**
 * @author nik
 */
public class JpsJavaExtensionTest extends JpsJavaModelTestCase {
  public void testModule() {
    final JpsModule module = addModule();
    final JpsJavaModuleExtension extension = getJavaService().getOrCreateModuleExtension(module);
    extension.setOutputUrl("file://path");
    JpsJavaModuleExtension moduleExtension = getJavaService().getModuleExtension(module);
    assertNotNull(moduleExtension);
    assertEquals("file://path", moduleExtension.getOutputUrl());
  }

  public void testDependency() {
    final JpsModel model = myModel.createModifiableModel(new TestJpsEventDispatcher());
    final JpsModule module = model.getProject().addModule("m", JpsJavaModuleType.INSTANCE);
    final JpsLibrary library = model.getProject().addLibrary("l", JpsJavaLibraryType.INSTANCE);
    final JpsLibraryDependency dependency = module.getDependenciesList().addLibraryDependency(library);
    getJavaService().getOrCreateDependencyExtension(dependency).setScope(JpsJavaDependencyScope.TEST);
    getJavaService().getOrCreateDependencyExtension(dependency).setExported(true);
    model.commit();

    List<JpsDependencyElement> dependencies = assertOneElement(myProject.getModules()).getDependenciesList().getDependencies();
    assertEquals(2, dependencies.size());
    final JpsDependencyElement dep = dependencies.get(1);
    final JpsJavaDependencyExtension extension = getJavaService().getDependencyExtension(dep);
    assertNotNull(extension);
    assertTrue(extension.isExported());
    assertSame(JpsJavaDependencyScope.TEST, extension.getScope());
  }
}
