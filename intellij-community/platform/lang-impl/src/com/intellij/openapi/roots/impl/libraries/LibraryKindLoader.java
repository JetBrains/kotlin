// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl.libraries;

import com.intellij.ide.ApplicationInitializedListener;
import com.intellij.openapi.roots.libraries.LibraryType;

/**
 * @author nik
 */
public class LibraryKindLoader implements ApplicationInitializedListener {
  @Override
  public void componentsInitialized() {
    //todo[nik] this is temporary workaround for IDEA-98118: we need to initialize all library types to ensure that their kinds are created and registered in LibraryKind.ourAllKinds
    //In order to properly fix the problem we should extract all UI-related methods from LibraryType to a separate class and move LibraryType to intellij.platform.projectModel.impl module
    LibraryType.EP_NAME.getExtensionList();
  }
}
