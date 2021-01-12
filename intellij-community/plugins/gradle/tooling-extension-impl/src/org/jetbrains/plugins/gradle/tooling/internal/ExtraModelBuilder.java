// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.internal;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.internal.project.DefaultProject;
import org.gradle.api.invocation.Gradle;
import org.gradle.internal.impldep.com.google.common.collect.Lists;
import org.gradle.internal.impldep.com.google.gson.GsonBuilder;
import org.gradle.internal.logging.progress.ProgressLogger;
import org.gradle.internal.logging.progress.ProgressLoggerFactory;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.plugins.gradle.model.internal.DummyModel;
import org.jetbrains.plugins.gradle.model.internal.TurnOffDefaultTasks;
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.Message;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.jetbrains.plugins.gradle.tooling.builder.ExternalProjectBuilderImpl;
import org.jetbrains.plugins.gradle.tooling.util.VersionMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class ExtraModelBuilder implements ToolingModelBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(ExtraModelBuilder.class);
  @ApiStatus.Internal
  public static final String MODEL_BUILDER_SERVICE_MESSAGE_PREFIX = "ModelBuilderService message: ";

  private final List<ModelBuilderService> modelBuilderServices;

  @NotNull
  private final GradleVersion myCurrentGradleVersion;
  private ModelBuilderContext myModelBuilderContext;
  @Deprecated
  public static final ThreadLocal<ModelBuilderContext> CURRENT_CONTEXT = new ThreadLocal<ModelBuilderContext>();

  public ExtraModelBuilder() {
    this(GradleVersion.current());
  }

  @TestOnly
  public ExtraModelBuilder(@NotNull GradleVersion gradleVersion) {
    myCurrentGradleVersion = gradleVersion;
    modelBuilderServices = Lists.newArrayList(ServiceLoader.load(ModelBuilderService.class, ExtraModelBuilder.class.getClassLoader()));
  }

  @Override
  public boolean canBuild(String modelName) {
    if (DummyModel.class.getName().equals(modelName)) return true;
    if (TurnOffDefaultTasks.class.getName().equals(modelName)) return true;
    for (ModelBuilderService service : modelBuilderServices) {
      if (service.canBuild(modelName) && isVersionMatch(service)) return true;
    }
    return false;
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    if (DummyModel.class.getName().equals(modelName)) {
      return new DummyModel() {
      };
    }
    if (TurnOffDefaultTasks.class.getName().equals(modelName)) {
      StartParameter startParameter = project.getGradle().getStartParameter();
      List<String> taskNames = startParameter.getTaskNames();
      if (taskNames.isEmpty()) {
        startParameter.setTaskNames(null);
        List<String> helpTask = Collections.singletonList("help");
        project.setDefaultTasks(helpTask);
        startParameter.setExcludedTaskNames(helpTask);
      }
      return null;
    }

    if (myModelBuilderContext == null) {
      Gradle rootGradle = getRootGradle(project.getGradle());
      myModelBuilderContext = new MyModelBuilderContext(rootGradle);
    }

    CURRENT_CONTEXT.set(myModelBuilderContext);
    try {
      for (ModelBuilderService service : modelBuilderServices) {
        if (service.canBuild(modelName) && isVersionMatch(service)) {
          final long startTime = System.currentTimeMillis();
          try {
            if (service instanceof AbstractModelBuilderService) {
              return ((AbstractModelBuilderService)service).buildAll(modelName, project, myModelBuilderContext);
            }
            else {
              return service.buildAll(modelName, project);
            }
          }
          catch (Exception e) {
            if (service instanceof ExternalProjectBuilderImpl) {
              if (e instanceof RuntimeException) throw (RuntimeException)e;
              throw new ExternalSystemException(e);
            }
            reportModelBuilderFailure(project, service, myModelBuilderContext, e);
          }
          finally {
            if (Boolean.getBoolean("idea.gradle.custom.tooling.perf")) {
              final long timeInMs = (System.currentTimeMillis() - startTime);
              reportPerformanceStatistic(project, service, modelName, timeInMs);
            }
          }
          return null;
        }
      }
      throw new IllegalArgumentException("Unsupported model: " + modelName);
    }
    finally {
      CURRENT_CONTEXT.remove();
    }
  }

  private static void reportPerformanceStatistic(Project project, ModelBuilderService service, String modelName, long timeInMs) {
    String msg = String.format("%s: service %s imported '%s' in %d ms", project.getDisplayName(), service.getClass().getSimpleName(), modelName, timeInMs);
    project.getLogger().error(msg);
  }

  public static void reportModelBuilderFailure(@NotNull Project project,
                                               @NotNull ModelBuilderService service,
                                               @NotNull ModelBuilderContext modelBuilderContext,
                                               @NotNull Exception modelBuilderError) {
    try {
      Message message = service.getErrorMessageBuilder(project, modelBuilderError).buildMessage();
      modelBuilderContext.report(project, message);
    }
    catch (Throwable e) {
      LOG.warn("Failed to report model builder error", e);
    }
  }

  private boolean isVersionMatch(@NotNull ModelBuilderService builderService) {
    TargetVersions targetVersions = builderService.getClass().getAnnotation(TargetVersions.class);
    return new VersionMatcher(myCurrentGradleVersion).isVersionMatch(targetVersions);
  }

  @NotNull
  private static Gradle getRootGradle(@NotNull Gradle gradle) {
    Gradle root = gradle;
    while (root.getParent() != null) {
      root = root.getParent();
    }
    return root;
  }

  private static class MyModelBuilderContext implements ModelBuilderContext {
    private final Map<DataProvider, Object> myMap = new IdentityHashMap<DataProvider, Object>();
    private final Gradle myGradle;

    private MyModelBuilderContext(Gradle gradle) {
      myGradle = gradle;
    }

    @NotNull
    @Override
    public Gradle getRootGradle() {
      return myGradle;
    }

    @NotNull
    @Override
    public <T> T getData(@NotNull DataProvider<T> provider) {
      Object data = myMap.get(provider);
      if (data == null) {
        T value = provider.create(myGradle);
        myMap.put(provider, value);
        return value;
      }
      else {
        //noinspection unchecked
        return (T)data;
      }
    }

    @ApiStatus.Experimental
    @Override
    public void report(@NotNull Project project, @NotNull Message message) {
      if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("2.14.1")) < 0) {
        return;
      }
      try {
        ProgressLoggerFactory progressLoggerFactory = ((DefaultProject)project).getServices().get(ProgressLoggerFactory.class);
        ProgressLogger operation = progressLoggerFactory.newOperation(ModelBuilderService.class);
        String jsonMessage = new GsonBuilder().create().toJson(message);
        operation.setDescription(MODEL_BUILDER_SERVICE_MESSAGE_PREFIX + jsonMessage);
        operation.started();
        operation.completed();
      }
      catch (Throwable e) {
        LOG.warn("Failed to report model builder message", e);
      }
    }
  }
}
