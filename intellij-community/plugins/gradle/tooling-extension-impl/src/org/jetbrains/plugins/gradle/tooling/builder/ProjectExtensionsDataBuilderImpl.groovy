/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.reflect.HasPublicType
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.model.*
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService

/**
 * @author Vladislav.Soroka
 */
@CompileStatic
class ProjectExtensionsDataBuilderImpl implements ModelBuilderService {
  private static is35_OrBetter = GradleVersion.current().baseVersion >= GradleVersion.version("3.5")

  @Override
  boolean canBuild(String modelName) {
    modelName == GradleExtensions.name
  }

  @Override
  Object buildAll(String modelName, Project project) {
    DefaultGradleExtensions result = new DefaultGradleExtensions()
    result.parentProjectPath = project.parent?.path

    for (it in project.configurations) {
      result.configurations.add(new DefaultGradleConfiguration(it.name, it.description, it.visible))
    }
    for (it in project.buildscript.configurations) {
      result.configurations.add(new DefaultGradleConfiguration(it.name, it.description, it.visible, true))
    }

    def convention = project.convention
    convention.plugins.each { key, value ->
      result.conventions.add(new DefaultGradleConvention(key, getType(value)))
    }
    def extensions = project.extensions
    extensions.extraProperties.properties.each { name, value ->
      if(name == 'extraModelBuilder' || name.contains('.')) return
      String typeFqn = getType(value)
      result.gradleProperties.add(new DefaultGradleProperty(
        name, typeFqn, value.toString()))
    }

    for (it in extensions.findAll()) {
      def extension = it as ExtensionContainer
      List<String> keyList =
        GradleVersion.current() >= GradleVersion.version("4.5")
          ? extension.extensionsSchema.collect { it["name"] as String }
          : is35_OrBetter
          ? extension.schema.keySet().asList() as List<String>
          : extractKeysViaReflection(extension)

      for (name in keyList) {
        def value = extension.findByName(name)

        if (value == null) continue
        if (name == 'idea') continue

        def rootTypeFqn = getType(value)
        def namedObjectTypeFqn = null as String
        if (value instanceof NamedDomainObjectCollection) {
          def objectCollection = (NamedDomainObjectCollection)value
          if (!objectCollection.isEmpty()) {
            namedObjectTypeFqn = getType(objectCollection.find { true })
          }
        }
        result.extensions.add(new DefaultGradleExtension(name, rootTypeFqn, namedObjectTypeFqn))
      }
    }
    return result
  }

  private List<String> extractKeysViaReflection(ExtensionContainer convention) {
    def m = convention.class.getMethod("getAsMap")
    return (m.invoke(convention) as Map<String, Object>).keySet().asList() as List<String>
  }

  @Override
  ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder.create(
      project, e, "Project extensions data import errors"
    ).withDescription(
      "Unable to resolve some context data of gradle scripts. Some codeInsight features inside *.gradle files can be unavailable.")
  }

  static String getType(object) {
    if (is35_OrBetter && object instanceof HasPublicType) {
      return object.publicType.toString()
    }
    def clazz = object?.getClass()?.canonicalName
    def decorIndex = clazz?.lastIndexOf('_Decorated')
    def result = !decorIndex || decorIndex == -1 ? clazz : clazz.substring(0, decorIndex)
    if (!result && object instanceof Closure) return "groovy.lang.Closure"
    return result
  }
}
