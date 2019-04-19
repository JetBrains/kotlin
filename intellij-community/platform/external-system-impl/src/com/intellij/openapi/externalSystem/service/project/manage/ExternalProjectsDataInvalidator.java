// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.ide.caches.CachesInvalidator;

/**
 * @author Vladislav.Soroka
 */
public class ExternalProjectsDataInvalidator extends CachesInvalidator {
  @Override
  public void invalidateCaches() {
    ExternalProjectsDataStorage.invalidateCaches();
  }
}
