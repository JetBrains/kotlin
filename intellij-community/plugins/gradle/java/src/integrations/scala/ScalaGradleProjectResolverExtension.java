/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.integrations.scala;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.ScalaCompileOptionsData;
import org.jetbrains.plugins.gradle.model.data.ScalaModelData;
import org.jetbrains.plugins.gradle.model.scala.ScalaCompileOptions;
import org.jetbrains.plugins.gradle.model.scala.ScalaForkOptions;
import org.jetbrains.plugins.gradle.model.scala.ScalaModel;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collections;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public class ScalaGradleProjectResolverExtension extends AbstractProjectResolverExtension {
  private static final Logger LOG = Logger.getInstance(ScalaGradleProjectResolverExtension.class);

  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    ScalaModel scalaModel = resolverCtx.getExtraProject(gradleModule, ScalaModel.class);
    if (scalaModel != null) {
      ScalaModelData scalaModelData = create(scalaModel);
      ideModule.createChild(ScalaModelData.KEY, scalaModelData);
    }

    nextResolver.populateModuleExtraModels(gradleModule, ideModule);
  }

  @NotNull
  @Override
  public Set<Class> getExtraProjectModelClasses() {
    return Collections.singleton(ScalaModel.class);
  }


  @NotNull
  private static ScalaModelData create(@NotNull ScalaModel scalaModel) {
    ScalaModelData scalaModelData = new ScalaModelData(GradleConstants.SYSTEM_ID);
    scalaModelData.setZincClasspath(scalaModel.getZincClasspath());
    scalaModelData.setScalaClasspath(scalaModel.getScalaClasspath());
    scalaModelData.setScalaCompileOptions(create((scalaModel.getScalaCompileOptions())));
    scalaModelData.setSourceCompatibility(scalaModel.getSourceCompatibility());
    scalaModelData.setTargetCompatibility(scalaModel.getTargetCompatibility());
    return scalaModelData;
  }

  @Nullable
  @Contract("null -> null")
  private static ScalaCompileOptionsData create(@Nullable ScalaCompileOptions options) {
    if (options == null) return null;

    ScalaCompileOptionsData result = new ScalaCompileOptionsData();

    result.setAdditionalParameters(options.getAdditionalParameters());
    result.setDaemonServer(options.getDaemonServer());
    result.setDebugLevel(options.getDebugLevel());
    result.setDeprecation(options.isDeprecation());
    result.setEncoding(options.getEncoding());
    result.setFailOnError(options.isFailOnError());
    result.setForce(options.getForce());
    result.setFork(options.isFork());
    result.setListFiles(options.isListFiles());
    result.setLoggingLevel(options.getLoggingLevel());
    result.setDebugLevel(options.getDebugLevel());
    result.setLoggingPhases(options.getLoggingPhases());
    result.setOptimize(options.isOptimize());
    result.setUnchecked(options.isUnchecked());
    result.setUseAnt(options.isUseAnt());
    result.setUseCompileDaemon(options.isUseCompileDaemon());
    result.setForkOptions(create(options.getForkOptions()));
    return result;
  }

  @Nullable
  @Contract("null -> null")
  private static ScalaCompileOptionsData.ScalaForkOptions create(@Nullable ScalaForkOptions options) {
    if (options == null) return null;

    ScalaCompileOptionsData.ScalaForkOptions result = new ScalaCompileOptionsData.ScalaForkOptions();
    result.setJvmArgs(options.getJvmArgs());
    result.setMemoryInitialSize(options.getMemoryInitialSize());
    result.setMemoryMaximumSize(options.getMemoryMaximumSize());
    return result;
  }
}
