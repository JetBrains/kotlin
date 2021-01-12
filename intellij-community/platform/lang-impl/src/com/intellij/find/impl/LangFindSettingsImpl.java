// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl;

import com.intellij.lang.IdeLanguageCustomization;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class LangFindSettingsImpl extends FindSettingsImpl {
  public LangFindSettingsImpl() {
    Set<String> extensions = JBIterable.from(IdeLanguageCustomization.getInstance().getPrimaryIdeLanguages())
      .filterMap(Language::getAssociatedFileType)
      .flatten(o -> JBIterable.of(o.getDefaultExtension())
        .append(JBIterable.from(FileTypeManager.getInstance().getAssociations(o))
                  .filter(ExtensionFileNameMatcher.class)
                  .filterMap(ExtensionFileNameMatcher::getExtension)))
      .addAllTo(new LinkedHashSet<>());
    if (extensions.contains("java")) {
      extensions.add("properties");
      extensions.add("jsp");
    }
    if (!extensions.contains("sql")) {
      extensions.add("xml");
      extensions.add("html");
      extensions.add("css");
    }
    for (String ext : ContainerUtil.reverse(new ArrayList<>(extensions))) {
      recentFileMasks.add("*." + ext);
    }
  }
}
