/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Allows to customize {@link GradleProjectResolver} processing.
 * <p/>
 * Only a single extension is expected per platform.
 *
 * @author Vladislav.Soroka
 */

public abstract class GradleImportCustomizer {

  private static final ExtensionPointName<GradleImportCustomizer> EP_NAME =
    ExtensionPointName.create("org.jetbrains.plugins.gradle.importCustomizer");

  public abstract String getPlatformPrefix();

  @Nullable
  public static GradleImportCustomizer get() {
    GradleImportCustomizer result = null;
    if (!PlatformUtils.isIntelliJ()) {
      final String platformPrefix = PlatformUtils.getPlatformPrefix();
      for (GradleImportCustomizer provider : EP_NAME.getExtensions()) {
        if (StringUtil.equals(platformPrefix, provider.getPlatformPrefix())) {
          assert result == null : "Multiple gradle import customizer extensions found";
          result = provider;
        }
      }
    }
    return result;
  }

  public abstract boolean useExtraJvmArgs();
}
