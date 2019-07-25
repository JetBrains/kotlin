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

package com.intellij.ide.hierarchy;

import com.intellij.openapi.actionSystem.DataContext;

import javax.swing.*;

/**
 * @author cdr
 */
abstract class ChangeViewTypeActionBase extends ChangeHierarchyViewActionBase {
  ChangeViewTypeActionBase(final String shortDescription, final String longDescription, final Icon icon) {
    super(shortDescription, longDescription, icon);
  }

  @Override
  protected TypeHierarchyBrowserBase getHierarchyBrowser(final DataContext context) {
    return getTypeHierarchyBrowser(context);
  }

  static TypeHierarchyBrowserBase getTypeHierarchyBrowser(final DataContext context) {
    return TypeHierarchyBrowserBase.DATA_KEY.getData(context);
  }
}
