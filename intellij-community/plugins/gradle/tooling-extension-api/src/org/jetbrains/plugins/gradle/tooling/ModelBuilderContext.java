// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling;

import org.gradle.api.invocation.Gradle;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public interface ModelBuilderContext {
  /**
   * @return root Gradle instance
   */
  @NotNull
  Gradle getRootGradle();

  /**
   * @return cached data if it's already created, newly created data otherwise
   */
  @NotNull
  <T> T getData(@NotNull DataProvider<T> provider);

  interface DataProvider<T> {
    @NotNull
    T create(@NotNull Gradle gradle);
  }
}
