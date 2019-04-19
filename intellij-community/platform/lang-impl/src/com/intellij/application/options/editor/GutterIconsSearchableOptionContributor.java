/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.editor;

import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.codeInsight.daemon.LineMarkerProviders;
import com.intellij.ide.ui.search.SearchableOptionContributor;
import com.intellij.ide.ui.search.SearchableOptionProcessor;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public class GutterIconsSearchableOptionContributor extends SearchableOptionContributor {
  @Override
  public void processOptions(@NotNull SearchableOptionProcessor processor) {
    ExtensionPoint<LineMarkerProvider> point = Extensions.getRootArea().getExtensionPoint(LineMarkerProviders.EP_NAME);
    //noinspection unchecked
    LanguageExtensionPoint<LineMarkerProvider>[] extensions = (LanguageExtensionPoint<LineMarkerProvider>[])point.getExtensions();
    for (LanguageExtensionPoint<LineMarkerProvider> extension : extensions) {
      LineMarkerProvider instance = extension.getInstance();
      if (instance instanceof LineMarkerProviderDescriptor) {
        String name = ((LineMarkerProviderDescriptor)instance).getName();
        if (StringUtil.isNotEmpty(name)) {
          processor.addOptions(name, null, name, GutterIconsConfigurable.ID, GutterIconsConfigurable.DISPLAY_NAME, true);
        }
      }
    }
  }
}
