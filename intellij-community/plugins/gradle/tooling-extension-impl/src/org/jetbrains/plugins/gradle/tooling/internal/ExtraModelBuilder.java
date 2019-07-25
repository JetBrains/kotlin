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
package org.jetbrains.plugins.gradle.tooling.internal;

import org.gradle.api.Project;
import org.gradle.internal.impldep.com.google.common.collect.Lists;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.jetbrains.plugins.gradle.tooling.util.VersionMatcher;

import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("UnusedDeclaration")
public class ExtraModelBuilder implements ToolingModelBuilder {
  private final List<ModelBuilderService> modelBuilderServices;

  @NotNull
  private final GradleVersion myCurrentGradleVersion;

  public ExtraModelBuilder() {
    this(GradleVersion.current());
  }

  @TestOnly
  public ExtraModelBuilder(@NotNull GradleVersion gradleVersion) {
    this.myCurrentGradleVersion = gradleVersion;
    this.modelBuilderServices = Lists.newArrayList(ServiceLoader.load(ModelBuilderService.class, ExtraModelBuilder.class.getClassLoader()));
  }

  @Override
  public boolean canBuild(String modelName) {
    for (ModelBuilderService service : modelBuilderServices) {
      if (service.canBuild(modelName) && isVersionMatch(service)) return true;
    }
    return false;
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    for (ModelBuilderService service : modelBuilderServices) {
      if (service.canBuild(modelName) && isVersionMatch(service)) {
        final long startTime = System.currentTimeMillis();
        try {
          return service.buildAll(modelName, project);
        }
        catch (Exception e) {
          ErrorMessageBuilder builderError = service.getErrorMessageBuilder(project, e);
          project.getLogger().error(builderError.build());
        } finally {
          if(Boolean.getBoolean("idea.gradle.custom.tooling.perf")) {
            final long timeInMs = (System.currentTimeMillis() - startTime);
            project.getLogger().error(ErrorMessageBuilder.create(
              project, null, "Performance statistics"
            ).withDescription(String.format("service %s imported data in %d ms", service.getClass().getSimpleName(), timeInMs)).build());
          }
        }
        return null;
      }
    }
    throw new IllegalArgumentException("Unsupported model: " + modelName);
  }

  private boolean isVersionMatch(@NotNull ModelBuilderService builderService) {
    TargetVersions targetVersions = builderService.getClass().getAnnotation(TargetVersions.class);
    return new VersionMatcher(myCurrentGradleVersion).isVersionMatch(targetVersions);
  }
}
