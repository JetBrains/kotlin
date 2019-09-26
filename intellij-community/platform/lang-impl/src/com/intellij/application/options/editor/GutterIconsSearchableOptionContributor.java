// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor;

import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.codeInsight.daemon.LineMarkerProviders;
import com.intellij.ide.ui.search.SearchableOptionContributor;
import com.intellij.ide.ui.search.SearchableOptionProcessor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.KeyedLazyInstance;
import org.jetbrains.annotations.NotNull;

public class GutterIconsSearchableOptionContributor extends SearchableOptionContributor {
  @Override
  public void processOptions(@NotNull SearchableOptionProcessor processor) {
    for (KeyedLazyInstance<LineMarkerProvider> extension : LineMarkerProviders.EP_NAME.getExtensions()) {
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
