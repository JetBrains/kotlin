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
package org.jetbrains.plugins.gradle.tooling.builder

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.model.DefaultIntelliJSettings
import org.jetbrains.plugins.gradle.model.IntelliJSettings
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService

/**
 * @author Vladislav.Soroka
 */
@CompileStatic
class IntelliJSettingsBuilder implements ModelBuilderService {

  @Override
  boolean canBuild(String modelName) {
    modelName == IntelliJSettings.name
  }

  @Override
  Object buildAll(String modelName, Project project) {
    ExtensionAware extensionAware = project.plugins.findPlugin(IdeaPlugin.class)?.model?.module as ExtensionAware
    if (extensionAware) {
      def object = extensionAware.extensions.findByName("settings")
      if (object) {
        return new DefaultIntelliJSettings(object.toString())
      }
    }
    return null
  }

  @Override
  ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder
      .create(project, e, "IntelliJ settings import errors")
      .withDescription("Unable to build IntelliJ project settings")
  }
}
