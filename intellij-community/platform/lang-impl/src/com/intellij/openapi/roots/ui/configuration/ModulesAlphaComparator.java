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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.module.Module;

import java.util.Comparator;

/**
 * @author Eugene Zhuravlev
 */
public class ModulesAlphaComparator implements Comparator<Module>{

  public static final ModulesAlphaComparator INSTANCE = new ModulesAlphaComparator();

  @Override
  public int compare(Module module1, Module module2) {
    if (module1 == null && module2 == null) return 0;
    if (module1 == null) return -1;
    if (module2 == null) return 1;
    return module1.getName().compareToIgnoreCase(module2.getName());
  }
}
