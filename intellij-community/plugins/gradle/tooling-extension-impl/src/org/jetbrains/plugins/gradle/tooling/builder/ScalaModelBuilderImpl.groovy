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

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.scala.ScalaCompileOptions
import org.gradle.api.tasks.scala.ScalaForkOptions
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.gradle.model.scala.ScalaModel
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import org.jetbrains.plugins.gradle.tooling.internal.scala.ScalaCompileOptionsImpl
import org.jetbrains.plugins.gradle.tooling.internal.scala.ScalaForkOptionsImpl
import org.jetbrains.plugins.gradle.tooling.internal.scala.ScalaModelImpl

/**
 * @author Vladislav.Soroka
 */
class ScalaModelBuilderImpl implements ModelBuilderService {

  private static final String COMPILE_SCALA_TASK = "compileScala"

  @Override
  boolean canBuild(String modelName) {
    return ScalaModel.name.equals(modelName)
  }

  @Override
  Object buildAll(String modelName, Project project) {
    final ScalaPlugin scalaPlugin = project.plugins.findPlugin(ScalaPlugin)

    ScalaModel scalaModel = null
    if (scalaPlugin != null) {
      Task scalaTask = project.tasks.getByName(COMPILE_SCALA_TASK)
      scalaModel = createModel(scalaTask)
    }
    else {
      Iterator<ScalaCompile> it = project.tasks.withType(ScalaCompile).iterator()
      if (it.hasNext()) {
        scalaModel = createModel(it.next())
      }
    }

    return scalaModel
  }

  @Nullable
  private static ScalaModel createModel(@Nullable Task task) {
    if (!(task instanceof ScalaCompile)) return null

    ScalaCompile scalaCompile = (ScalaCompile)task
    ScalaModelImpl scalaModel = new ScalaModelImpl()
    scalaModel.scalaClasspath = new LinkedHashSet<>(scalaCompile.scalaClasspath.files)
    scalaModel.zincClasspath = new LinkedHashSet<>(scalaCompile.zincClasspath.files)
    scalaModel.scalaCompileOptions = create(scalaCompile.scalaCompileOptions)
    scalaModel.targetCompatibility = scalaCompile.targetCompatibility
    scalaModel.sourceCompatibility = scalaCompile.sourceCompatibility
    return scalaModel
  }

  @NotNull
  @Override
  ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder.create(
      project, e, "Scala import errors"
    ).withDescription("Unable to build Scala project configuration")
  }

  @Nullable
  @Contract("null -> null")
  private static ScalaCompileOptionsImpl create(@Nullable ScalaCompileOptions options) {
    if (options == null) return null

    ScalaCompileOptionsImpl result = new ScalaCompileOptionsImpl()
    result.additionalParameters = wrapStringList(options.additionalParameters)
    result.daemonServer = options.hasProperty('daemonServer') ? options.daemonServer : null
    result.debugLevel = options.debugLevel
    result.deprecation = options.deprecation
    result.encoding = options.encoding
    result.failOnError = options.failOnError
    result.force = String.valueOf(options.force)
    result.fork = options.hasProperty('fork') ? options.fork : false
    result.forkOptions = create(options.forkOptions)
    result.listFiles = options.listFiles
    result.loggingLevel = options.loggingLevel
    result.debugLevel = options.debugLevel
    result.loggingPhases = wrapStringList(options.loggingPhases)
    result.optimize = options.optimize
    result.unchecked = options.unchecked
    result.useAnt = options.hasProperty('useAnt') ? options.useAnt : false
    result.useCompileDaemon = options.hasProperty('useCompileDaemon') ? options.useCompileDaemon : false

    return result
  }

  @Nullable
  private static List<String> wrapStringList(@Nullable List<String> list) {
    if (list == null) return null
    // fix serialization issue if 's' is an instance of groovy.lang.GString [IDEA-125174]
    return list.collect { it.toString() }
  }

  @Nullable
  @Contract("null -> null")
  private static ScalaForkOptionsImpl create(@Nullable ScalaForkOptions forkOptions) {
    if (forkOptions == null) return null

    ScalaForkOptionsImpl result = new ScalaForkOptionsImpl()
    result.jvmArgs = wrapStringList(forkOptions.jvmArgs)
    result.memoryInitialSize = forkOptions.memoryInitialSize
    result.memoryMaximumSize = forkOptions.memoryMaximumSize
    return result
  }
}
