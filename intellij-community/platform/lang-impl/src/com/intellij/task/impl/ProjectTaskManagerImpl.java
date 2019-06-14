// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectModelBuildableElement;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.task.*;
import com.intellij.ui.GuiUtils;
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.intellij.util.containers.ContainerUtil.map;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.groupingBy;

/**
 * @author Vladislav.Soroka
 */
public class ProjectTaskManagerImpl extends ProjectTaskManager {

  private static final Logger LOG = Logger.getInstance("#com.intellij.task.ProjectTaskManager");
  private final ProjectTaskRunner myDummyTaskRunner = new DummyTaskRunner();
  private final ProjectTaskListener myEventPublisher;
  private final List<ProjectTaskManagerListener> myListeners = new CopyOnWriteArrayList<>();

  public ProjectTaskManagerImpl(@NotNull Project project) {
    super(project);
    myEventPublisher = project.getMessageBus().syncPublisher(ProjectTaskListener.TOPIC);
  }

  @Override
  public void build(@NotNull Module[] modules, @Nullable ProjectTaskNotification callback) {
    run(createModulesBuildTask(modules, true, true, false), callback);
  }

  @Override
  public void rebuild(@NotNull Module[] modules, @Nullable ProjectTaskNotification callback) {
    run(createModulesBuildTask(modules, false, false, false), callback);
  }

  @Override
  public void compile(@NotNull VirtualFile[] files, @Nullable ProjectTaskNotification callback) {
    List<ModuleFilesBuildTask> buildTasks = map(stream(files)
                                                  .collect(groupingBy(
                                                    file -> ProjectFileIndex.SERVICE.getInstance(myProject)
                                                      .getModuleForFile(file, false)))
                                                  .entrySet(), entry -> new ModuleFilesBuildTaskImpl(entry.getKey(), false,
                                                                                                     entry.getValue()));

    run(new ProjectTaskList(buildTasks), callback);
  }

  @Override
  public void build(@NotNull ProjectModelBuildableElement[] buildableElements, @Nullable ProjectTaskNotification callback) {
    doBuild(buildableElements, callback, true);
  }

  @Override
  public void rebuild(@NotNull ProjectModelBuildableElement[] buildableElements, @Nullable ProjectTaskNotification callback) {
    doBuild(buildableElements, callback, false);
  }

  @Override
  public void buildAllModules(@Nullable ProjectTaskNotification callback) {
    run(createAllModulesBuildTask(true, myProject), callback);
  }

  @Override
  public void rebuildAllModules(@Nullable ProjectTaskNotification callback) {
    run(createAllModulesBuildTask(false, myProject), callback);
  }

  @Override
  public ProjectTask createAllModulesBuildTask(boolean isIncrementalBuild, Project project) {
    return createModulesBuildTask(ModuleManager.getInstance(project).getModules(), isIncrementalBuild, false, false);
  }

  @Override
  public ProjectTask createModulesBuildTask(Module module,
                                            boolean isIncrementalBuild,
                                            boolean includeDependentModules,
                                            boolean includeRuntimeDependencies) {
    return createModulesBuildTask(ContainerUtil.ar(module), isIncrementalBuild, includeDependentModules, includeRuntimeDependencies);
  }

  @Override
  public ProjectTask createModulesBuildTask(Module[] modules,
                                            boolean isIncrementalBuild,
                                            boolean includeDependentModules,
                                            boolean includeRuntimeDependencies) {
    return modules.length == 1
           ? new ModuleBuildTaskImpl(modules[0], isIncrementalBuild, includeDependentModules, includeRuntimeDependencies)
           : new ProjectTaskList(map(Arrays.asList(modules), module ->
             new ModuleBuildTaskImpl(module, isIncrementalBuild, includeDependentModules, includeRuntimeDependencies)));
  }

  @Override
  public ProjectTask createBuildTask(boolean isIncrementalBuild, ProjectModelBuildableElement... buildableElements) {
    return buildableElements.length == 1
           ? new ProjectModelBuildTaskImpl<>(buildableElements[0], isIncrementalBuild)
           : new ProjectTaskList(map(Arrays.asList(buildableElements),
                                     buildableElement -> new ProjectModelBuildTaskImpl<>(buildableElement, isIncrementalBuild)));
  }

  @Override
  public void run(@NotNull ProjectTask projectTask, @Nullable ProjectTaskNotification callback) {
    run(new ProjectTaskContext(), projectTask, callback);
  }

  @Override
  public void run(@NotNull ProjectTaskContext context, @NotNull ProjectTask projectTask, @Nullable ProjectTaskNotification callback) {
    List<Pair<ProjectTaskRunner, Collection<? extends ProjectTask>>> toRun = new SmartList<>();

    Consumer<Collection<? extends ProjectTask>> taskClassifier = tasks -> {
      Map<ProjectTaskRunner, ? extends List<? extends ProjectTask>> toBuild = tasks.stream().collect(
        groupingBy(aTask -> stream(getTaskRunners())
          .filter(runner -> {
            try {
              return runner.canRun(myProject, aTask);
            }
            catch (Exception e) {
              LOG.error("Broken project task runner: " + runner.getClass().getName(), e);
            }
            return false;
          })
          .findFirst()
          .orElse(myDummyTaskRunner))
      );
      for (Map.Entry<ProjectTaskRunner, ? extends List<? extends ProjectTask>> entry : toBuild.entrySet()) {
        toRun.add(Pair.create(entry.getKey(), entry.getValue()));
      }
    };
    visitTasks(projectTask instanceof ProjectTaskList ? (ProjectTaskList)projectTask : Collections.singleton(projectTask), taskClassifier);

    context.putUserData(ProjectTaskScope.KEY, new ProjectTaskScope() {
      @NotNull
      @Override
      public <T extends ProjectTask> List<T> getRequestedTasks(@NotNull Class<T> instanceOf) {
        List<T> tasks = new ArrayList<>();
        //noinspection unchecked
        toRun.forEach(pair -> pair.second.stream().filter(instanceOf::isInstance).map(task -> (T)task).forEach(tasks::add));
        return tasks;
      }
    });
    myEventPublisher.started(context);

    Runnable runnable = () -> {
      for (ProjectTaskManagerListener listener : myListeners) {
        try {
          listener.beforeRun(context);
        }
        catch (ExecutionException e) {
          sendAbortedNotify(new ListenerNotificator(context, callback));
          return;
        }
      }

      if (toRun.isEmpty()) {
        sendSuccessNotify(new ListenerNotificator(context, callback));
        return;
      }

      ProjectTaskResultsAggregator callbacksCollector =
        new ProjectTaskResultsAggregator(new ListenerNotificator(context, callback), toRun.size());
      for (Pair<ProjectTaskRunner, Collection<? extends ProjectTask>> pair : toRun) {
        ProjectTaskRunnerNotification notification = new ProjectTaskRunnerNotification(pair.second, callbacksCollector);
        if (pair.second.isEmpty()) {
          sendSuccessNotify(notification);
        }
        else {
          pair.first.run(myProject, context, notification, pair.second);
        }
      }
    };
    // do not run before tasks on EDT
    if (ApplicationManager.getApplication().isDispatchThread()) {
      ApplicationManager.getApplication().executeOnPooledThread(runnable);
    }
    else {
      runnable.run();
    }
  }

  public final void addListener(@NotNull ProjectTaskManagerListener listener) {
    myListeners.add(listener);
  }

  private static void sendSuccessNotify(@Nullable ProjectTaskNotification notification) {
    if (notification != null) {
      notification.finished(new ProjectTaskResult(false, 0, 0));
    }
  }

  private static void sendAbortedNotify(@Nullable ProjectTaskNotification notification) {
    if (notification != null) {
      notification.finished(new ProjectTaskResult(true, 0, 0));
    }
  }

  private static void visitTasks(@NotNull Collection<? extends ProjectTask> tasks,
                                 @NotNull Consumer<? super Collection<? extends ProjectTask>> consumer) {
    for (ProjectTask child : tasks) {
      Collection<? extends ProjectTask> taskDependencies;
      if (child instanceof AbstractProjectTask) {
        taskDependencies = ((AbstractProjectTask)child).getDependsOn();
      }
      else if (child instanceof ProjectTaskList) {
        taskDependencies = (ProjectTaskList)child;
      }
      else {
        taskDependencies = Collections.singleton(child);
      }

      visitTasks(taskDependencies, consumer);
    }
    consumer.consume(tasks);
  }

  @NotNull
  private static ProjectTaskRunner[] getTaskRunners() {
    return ProjectTaskRunner.EP_NAME.getExtensions();
  }

  private void doBuild(@NotNull ProjectModelBuildableElement[] buildableElements,
                       @Nullable ProjectTaskNotification callback,
                       boolean isIncrementalBuild) {
    run(createBuildTask(isIncrementalBuild, buildableElements), callback);
  }

  private static class DummyTaskRunner extends ProjectTaskRunner {
    @Override
    public void run(@NotNull Project project,
                    @NotNull ProjectTaskContext context,
                    @Nullable ProjectTaskNotification callback,
                    @NotNull Collection<? extends ProjectTask> tasks) {
      sendSuccessNotify(callback);
    }

    @Override
    public boolean canRun(@NotNull ProjectTask projectTask) {
      return true;
    }
  }

  private class ListenerNotificator implements ProjectTaskNotification {
    @Nullable private final ProjectTaskNotification myDelegate;
    @NotNull private final ProjectTaskContext myContext;

    private ListenerNotificator(@NotNull ProjectTaskContext context,
                                @Nullable ProjectTaskNotification delegate) {
      myContext = context;
      myDelegate = delegate;
    }

    @Override
    public void finished(@NotNull ProjectTaskResult executionResult) {
      if (!executionResult.isAborted() && executionResult.getErrors() == 0) {
        // do not run after tasks on EDT
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          try {
            for (ProjectTaskManagerListener listener : myListeners) {
              listener.afterRun(myContext, executionResult);
            }
            notify(myContext, executionResult);
          }
          catch (ExecutionException e) {
            LOG.debug(e);
            notify(myContext, new ProjectTaskResult(
              false, executionResult.getErrors() + 1, executionResult.getWarnings(), executionResult.getTasksState()));
          }
        });
      }
      else {
        notify(myContext, executionResult);
      }
    }

    private void notify(@NotNull ProjectTaskContext context, @NotNull ProjectTaskResult executionResult) {
      GuiUtils.invokeLaterIfNeeded(() -> {
        if (!myProject.isDisposed()) {
          myEventPublisher.finished(context, executionResult);
        }
        if (myDelegate != null) {
          myDelegate.finished(executionResult);
        }
      }, ModalityState.defaultModalityState());
    }
  }

  private static class ProjectTaskRunnerNotification implements ProjectTaskNotification {
    private final ProjectTaskResultsAggregator myAggregator;
    private final Collection<? extends ProjectTask> myTasks;

    private ProjectTaskRunnerNotification(@NotNull Collection<? extends ProjectTask> tasks,
                                          @NotNull ProjectTaskResultsAggregator aggregator) {
      myTasks = tasks;
      myAggregator = aggregator;
    }

    @Override
    public void finished(@NotNull ProjectTaskResult result) {
      if (result.getTasksState().isEmpty()) {
        final boolean aborted = result.isAborted();
        final int errors = result.getErrors();
        ProjectTaskState state = new ProjectTaskState() {
          @Override
          public boolean isSkipped() {
            return aborted;
          }

          @Override
          public boolean isFailed() {
            return errors > 0;
          }
        };
        Map<ProjectTask, ProjectTaskState> tasksState = StreamEx.of(myTasks).toMap(Function.identity(), task -> state);
        result = new ProjectTaskResult(aborted, errors, result.getWarnings(), tasksState);
      }
      myAggregator.add(result);
    }
  }

  private static class ProjectTaskResultsAggregator {
    private final ProjectTaskNotification myDelegate;
    private final AtomicInteger myProgressCounter;
    private final AtomicInteger myErrorsCounter;
    private final AtomicInteger myWarningsCounter;
    private final AtomicBoolean myAbortedFlag;
    private final Map<ProjectTask, ProjectTaskState> myTasksState = ContainerUtil.newConcurrentMap();

    private ProjectTaskResultsAggregator(@NotNull ProjectTaskNotification delegate, int expectedResults) {
      myDelegate = delegate;
      myProgressCounter = new AtomicInteger(expectedResults);
      myErrorsCounter = new AtomicInteger();
      myWarningsCounter = new AtomicInteger();
      myAbortedFlag = new AtomicBoolean(false);
    }

    public void add(@NotNull ProjectTaskResult executionResult) {
      int inProgress = myProgressCounter.decrementAndGet();
      int allErrors = myErrorsCounter.addAndGet(executionResult.getErrors());
      int allWarnings = myWarningsCounter.addAndGet(executionResult.getWarnings());
      myTasksState.putAll(executionResult.getTasksState());
      if (executionResult.isAborted()) {
        myAbortedFlag.set(true);
      }
      if (inProgress <= 0) {
        ProjectTaskResult result = new ProjectTaskResult(myAbortedFlag.get(), allErrors, allWarnings, myTasksState);
        myDelegate.finished(result);
      }
    }
  }
}
