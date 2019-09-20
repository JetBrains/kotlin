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

import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.java.JpsJavaLibraryType;
import org.jetbrains.jps.model.java.JpsJavaModuleType;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.module.JpsModule;

/**
 * @author nik
 */
public abstract class JpsJavaModelTestCase extends JpsModelTestCase  {
  protected JpsModule addModule() {
    return addModule("m");
  }

  protected JpsModule addModule(final String name) {
    return myProject.addModule(name, JpsJavaModuleType.INSTANCE);
  }

  protected JpsLibrary addLibrary() {
    return addLibrary("l");
  }

  protected JpsLibrary addLibrary(final String name) {
    return myProject.addLibrary(name, JpsJavaLibraryType.INSTANCE);
  }

  protected JpsJavaExtensionService getJavaService() {
    return JpsJavaExtensionService.getInstance();
  }
}
