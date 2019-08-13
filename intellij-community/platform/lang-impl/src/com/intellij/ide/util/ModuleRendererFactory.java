/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.util;

import com.intellij.openapi.extensions.ExtensionPointName;

import javax.swing.*;

/**
 * @author yole
 */
public abstract class ModuleRendererFactory {
  private static final ExtensionPointName<ModuleRendererFactory> EP_NAME = ExtensionPointName.create("com.intellij.moduleRendererFactory");

  public static ModuleRendererFactory findInstance(Object element) {
    for (ModuleRendererFactory factory : EP_NAME.getExtensions()) {
      if (factory.handles(element)) {
        return factory;
      }
    }
    assert false : "No factory found for " + element;
    return null;
  }

  protected boolean handles(final Object element) {
    return true;
  }

  public abstract DefaultListCellRenderer getModuleRenderer();

  public boolean rendersLocationString() {
    return false;
  }
}
