// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.builder;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.AnnotationProcessingConfig;
import org.jetbrains.plugins.gradle.model.AnnotationProcessingModel;
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;
import org.jetbrains.plugins.gradle.tooling.internal.AnnotationProcessingConfigImpl;
import org.jetbrains.plugins.gradle.tooling.internal.AnnotationProcessingModelImpl;
import org.jetbrains.plugins.gradle.tooling.util.JavaPluginUtil;

import java.io.File;
import java.util.*;

public class AnnotationProcessingModelBuilder extends AbstractModelBuilderService {

  private static final boolean isAtLeastGradle3_4 = GradleVersion.current().compareTo(GradleVersion.version("3.4")) >= 0;
  private static final boolean isAtLeastGradle4_5 = isAtLeastGradle3_4 && GradleVersion.current().compareTo(GradleVersion.version("4.5")) >= 0;

  @Override
  public Object buildAll(@NotNull String modelName, @NotNull Project project, @NotNull ModelBuilderContext context) {
    if (!canBuild(modelName)) {
      return null;
    }

    if (!isAtLeastGradle3_4) {
      return null;
    }

    final SourceSetContainer container = JavaPluginUtil.getSourceSetContainer(project);
    if (container == null) {
      return null;
    }

    Map<String, AnnotationProcessingConfig> sourceSetConfigs = new HashMap<String, AnnotationProcessingConfig>();

    for (final SourceSet sourceSet : container) {
      String compileTaskName = sourceSet.getCompileJavaTaskName();
      Task compileTask = project.getTasks().findByName(compileTaskName);
      if (compileTask instanceof JavaCompile) {
        CompileOptions options = ((JavaCompile)compileTask).getOptions();
        FileCollection path = options.getAnnotationProcessorPath();
        if (path != null) {
          final Set<File> files = path.getFiles();
          if (!files.isEmpty()) {
            List<String> annotationProcessorArgs = new ArrayList<String>();
            List<String> args = isAtLeastGradle4_5 ? options.getAllCompilerArgs() : options.getCompilerArgs();
            for (String arg : args) {
              if (arg.startsWith("-A")) {
                annotationProcessorArgs.add(arg);
              }
            }
            Set<String> paths = new LinkedHashSet<String>(files.size());
            for (File file : files) {
              paths.add(file.getAbsolutePath());
            }
            sourceSetConfigs.put(sourceSet.getName(), new AnnotationProcessingConfigImpl(paths, annotationProcessorArgs));
          }
        }
      }
    }

    if (!sourceSetConfigs.isEmpty()) {
      return new AnnotationProcessingModelImpl(sourceSetConfigs);
    }

    return null;
  }

  @Override
  public boolean canBuild(String modelName) {
    return AnnotationProcessingModel.class.getName().equals(modelName);
  }

  @NotNull
  @Override
  public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder.create(
      project, e, "Project annotation processor import errors"
    ).withDescription(
      "Unable to create annotation processors model");
  }
}
