// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model;


import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * @author Vladislav.Soroka
 */
public interface CppProject extends Serializable {
  @Nullable
  CppComponent getMainComponent();

  @Nullable
  CppTestSuite getTestComponent();
}
