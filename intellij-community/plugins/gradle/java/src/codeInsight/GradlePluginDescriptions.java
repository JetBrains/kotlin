/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.codeInsight;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleDocumentationBundle;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GradlePluginDescriptions implements GradlePluginDescriptionsExtension {
  @NotNull
  @Override
  public Map<String, String> getPluginDescriptions() {
    final Collection<String> plugins = StringUtil.split(
      "java,groovy,idea,eclipse,scala,antlr,application,ear,jetty,maven,osgi,war,announce," +
      "build-announcements,checkstyle,codenarc,eclipse-wtp,findbugs,jdepend,pmd,project-report,signing,sonar", ",");

    Map<String, String> descriptions = new HashMap<>(plugins.size());
    for (String plugin : plugins) {
      descriptions.put(plugin, getDescription(plugin));
    }
    return descriptions;
  }

  @NotNull
  private static String getDescription(@NotNull String pluginName) {
    return GradleDocumentationBundle
      .messageOrDefault(String.format("gradle.documentation.org.gradle.api.Project.apply.plugin.%s.non-html", pluginName), "");
  }
}
