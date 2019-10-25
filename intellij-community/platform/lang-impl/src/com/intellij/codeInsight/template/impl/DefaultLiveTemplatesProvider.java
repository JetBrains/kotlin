// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.Nullable;

/**
 * Provides bundled live templates.
 * <p/>
 * See <a href="http://www.jetbrains.org/intellij/sdk/docs/tutorials/live_templates.html">Live Templates Tutorial</a>.
 *
 * @author yole
 * @see TemplateContextType
 * @deprecated Use {@link DefaultLiveTemplateEP} instead
 */
@Deprecated
public interface DefaultLiveTemplatesProvider {

  ExtensionPointName<DefaultLiveTemplatesProvider> EP_NAME = ExtensionPointName.create("com.intellij.defaultLiveTemplatesProvider");

  /**
   * User-visible and editable templates.
   *
   * @return paths to resources, without {@code .xml} extension (e.g. {@code /templates/foo})
   */
  String[] getDefaultLiveTemplateFiles();

  /**
   * Templates for programmatic use only.
   *
   * @return paths to resources, without {@code .xml} extension (e.g. {@code /templates/foo})
   */
  @Nullable
  String[] getHiddenLiveTemplateFiles();
}
