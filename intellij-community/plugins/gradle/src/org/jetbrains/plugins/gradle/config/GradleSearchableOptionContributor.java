// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.config;

import com.intellij.ide.ui.search.SearchableOptionContributor;
import com.intellij.ide.ui.search.SearchableOptionProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.settings.GradleConfigurable;

public class GradleSearchableOptionContributor extends SearchableOptionContributor {
  @Override
  public void processOptions(@NotNull SearchableOptionProcessor processor) {
    processor.addOptions("jvm", null, "Gradle JVM:", GradleConfigurable.ID, GradleConfigurable.DISPLAY_NAME, true);
    processor.addOptions("gradle", null, "Gradle JVM:", GradleConfigurable.ID, GradleConfigurable.DISPLAY_NAME, true);
  }
}
