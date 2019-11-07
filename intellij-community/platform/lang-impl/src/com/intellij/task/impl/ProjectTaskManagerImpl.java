// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.internal.statistic.IdeActivity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectModelBuildableElement;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.task.*;
import com.intellij.ui.GuiUtils;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.intellij.util.containers.ContainerUtil.emptyList;
import static com.intellij.util.containers.ContainerUtil.map;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.groupingBy;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("deprecation")
public class ProjectTaskManagerImpl extends ProjectTaskManager {

  private static final Logger LOG = Logger.getInstance(ProjectTaskManager.class);
  private final ProjectTaskRunner myDummyTaskRunner = new DummyTaskRunner();
  private final ProjectTaskListener myEventPublisher;
  private final List<ProjectTaskManagerListener> myListeners = new CopyOnWriteArrayList<>();

  public ProjectTaskManagerImpl(@NotNull Project project) {
    super(project);
    myEventPublisher = project.getMessageBus().syncPublisher(ProjectTaskListener.TOPIC);
  }

  @Override
  public Promise<Result> build(@NotNull Module[] modules) {
    return run(createModulesBuildTask(modules, true, true, false));
  }

  @Override
  public Promise<Result> rebuild(@NotNull Module[] modules) {
    return run(createModulesBuildTask(modules, false, false, false));
  }

  @Override
  public Promise<Result> compile(@NotNull VirtualFile[] files) {
    List<ModuleFilesBuildTask> buildTasks = map(
      stream(files)
        .collect(groupingBy(file -> ProjectFileIndex.SERVICE.getInstance(myProject).getModuleForFile(file, false)))
        .entrySet(),
      entry -> new ModuleFilesBuildTaskImpl(entry.getKey(), false, entry.getValue())
    );
    return run(new ProjectTaskList(buildTasks));
  }

  @Override
  public Promise<Result> build(@NotNull ProjectModelBuildableElement[] buildableElements) {
    return doBuild(buildableElements, true);
  }

  @Override
  public Promise<Result> rebuild(@NotNull ProjectModelBuildableElement[] buildableElements) {
    return doBuild(buildableElements, false);
  }

  @Override
  public Promise<Result> buildAllModules() {
    return run(createAllModulesBuildTask(true, myProject));
  }

  @Override
  public Promise<Result> rebuildAllModules() {
    return run(createAllModulesBuildTask(false, myProject));
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
  public Promise<Result> run(@NotNull ProjectTask projectTask) {
    return run(new ProjectTaskContext(), projectTask);
  }

  @Override
  public Promise<Result> run(@NotNull ProjectTaskContext context, @NotNull ProjectTask projectTask) {
    AsyncPromise<Result> promiseResult = new AsyncPromise<>();
    List<Pair<ProjectTaskRunner, Collection<? extends ProjectTask>>> toRun = new SmartList<>();

    Consumer<Collection<? extends ProjectTask>> taskClassifier = tasks -> {
      Map<ProjectTaskRunner, ? extends List<? extends ProjectTask>> toBuild = tasks.stream().collect(
        groupingBy(aTask -> stream(getTaskRunners())
          .filter(runner -> {
            try {
              return runner.canRun(myProject, aTask);
            }
            catch (ProcessCanceledException e) {
              throw e;
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


    IdeActivity activity = new IdeActivity(myProject, "build").startedWithData(data -> {
      data.addData("task_runner_class", map(toRun, it -> it.first.getClass().getName()));
    });

    myEventPublisher.started(context);

    Runnable runnable = () -> {
      for (ProjectTaskManagerListener listener : myListeners) {
        try {
          listener.beforeRun(context);
        }
        catch (ExecutionException e) {
          sendAbortedEmptyResult(context, new ResultConsumer(promiseResult));
          activity.finished();
          return;
        }
      }

      if (toRun.isEmpty()) {
        sendSuccessEmptyResult(context, new ResultConsumer(promiseResult));
        activity.finished();
        return;
      }

      ProjectTaskResultsAggregator resultsCollector =
        new ProjectTaskResultsAggregator(context, new ResultConsumer(promiseResult), toRun.size(), activity);
      for (Pair<ProjectTaskRunner, Collection<? extends ProjectTask>> pair : toRun) {
        Consumer<ProjectTaskRunner.Result> runnerResultConsumer = result -> resultsCollector.add(result, pair.second);
        if (pair.second.isEmpty()) {
          runnerResultConsumer.accept(TaskRunnerResults.SUCCESS);
        }
        else {
          ProjectTaskRunner runner = pair.first;
          if (context.isCollectionOfGeneratedFilesEnabled() && !runner.isFileGeneratedEventsSupported()) {
            pair.second.stream()
              .filter(ModuleBuildTask.class::isInstance)
              .map(task -> ((ModuleBuildTask)task).getModule())
              .forEach(module -> context.addDirtyOutputPathsProvider(moduleOutputPathsProvider(module)));
          }
          runner
            .run(myProject, context, pair.second.toArray(EMPTY_TASKS_ARRAY))
            .onSuccess(runnerResultConsumer)
            .onError(throwable -> runnerResultConsumer.accept(TaskRunnerResults.ABORTED));
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
    return promiseResult;
  }

  @Nullable
  @ApiStatus.Experimental
  public static <T> T waitForPromise(@NotNull Promise<T> promise) {
    while (true) {
      try {
        return promise.blockingGet(10, TimeUnit.MILLISECONDS);
      }
      catch (TimeoutException ignore) {
      }
      catch (java.util.concurrent.ExecutionException e) {
        ExceptionUtil.rethrow(e);
      }
      ProgressManager.checkCanceled();
    }
  }

  @NotNull
  private static Supplier<List<String>> moduleOutputPathsProvider(@NotNull Module module) {
    return () -> ReadAction.compute(() -> {
      return JBIterable.of(OrderEnumerator.orderEntries(module).withoutSdk().withoutLibraries().getClassesRoots())
        .filterMap(file -> file.isDirectory() && !file.getFileSystem().isReadOnly() ? file.getPath() : null).toList();
    });
  }

  public final void addListener(@NotNull ProjectTaskManagerListener listener) {
    myListeners.add(listener);
  }

  private static void sendSuccessEmptyResult(@NotNull ProjectTaskContext context, @NotNull Consumer<Result> resultConsumer) {
    resultConsumer.accept(new MyResult(context, Collections.emptyMap(), false, false));
  }

  private static void sendAbortedEmptyResult(@NotNull ProjectTaskContext context, @NotNull Consumer<Result> resultConsumer) {
    resultConsumer.accept(new MyResult(context, Collections.emptyMap(), true, false));
  }

  private static void visitTasks(@NotNull Collection<? extends ProjectTask> tasks,
                                 @NotNull Consumer<? super Collection<? extends ProjectTask>> consumer) {
    if (tasks.isEmpty()) return;

    for (ProjectTask child : tasks) {
      Collection<? extends ProjectTask> taskDependencies;
      if (child instanceof AbstractProjectTask) {
        taskDependencies = ((AbstractProjectTask)child).getDependsOn();
      }
      else if (child instanceof ProjectTaskList) {
        taskDependencies = (ProjectTaskList)child;
      }
      else {
        taskDependencies = emptyList();
      }

      visitTasks(taskDependencies, consumer);
    }
    consumer.accept(tasks);
  }

  @NotNull
  private static ProjectTaskRunner[] getTaskRunners() {
    return ProjectTaskRunner.EP_NAME.getExtensions();
  }

  private Promise<Result> doBuild(@NotNull ProjectModelBuildableElement[] buildableElements, boolean isIncrementalBuild) {
    return run(createBuildTask(isIncrementalBuild, buildableElements));
  }

  private static class DummyTaskRunner extends ProjectTaskRunner {
    @Override
    public Promise<Result> run(@NotNull Project project,
                               @NotNull ProjectTaskContext context,
                               @NotNull ProjectTask... tasks) {
      return Promises.resolvedPromise(TaskRunnerResults.SUCCESS);
    }

    @Override
    public boolean canRun(@NotNull ProjectTask projectTask) {
      return true;
    }
  }

  private class ResultConsumer implements Consumer<Result> {
    @NotNull private final AsyncPromise<Result> myPromise;

    private ResultConsumer(@NotNull AsyncPromise<Result> promise) {
      myPromise = promise;
    }

    @Override
    public void accept(@NotNull Result result) {
      if (!result.isAborted() && !result.hasErrors()) {
        // do not run after tasks on EDT
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          try {
            for (ProjectTaskManagerListener listener : myListeners) {
              listener.afterRun(result);
            }
            notify(result);
          }
          catch (ExecutionException e) {
            LOG.debug(e);
            notify(new ResultWrapper(result) {
              @Override
              public boolean hasErrors() {
                return true;
              }
            });
          }
        });
      }
      else {
        notify(result);
      }
    }

    private void notify(@NotNull Result result) {
      GuiUtils.invokeLaterIfNeeded(() -> {
        if (!myProject.isDisposed()) {
          myEventPublisher.finished(result);
        }
        myPromise.setResult(result);
      }, ModalityState.defaultModalityState());
    }
  }

  private static class ProjectTaskResultsAggregator {
    private final ProjectTaskContext myContext;
    private final ResultConsumer myResultConsumer;
    private final AtomicInteger myProgressCounter;
    private final IdeActivity myActivity;
    private final AtomicBoolean myErrorsFlag;
    private final AtomicBoolean myAbortedFlag;
    private final Map<ProjectTask, ProjectTaskState> myTasksState = ContainerUtil.newConcurrentMap();

    private ProjectTaskResultsAggregator(@NotNull ProjectTaskContext context,
                                         @NotNull ResultConsumer resultConsumer,
                                         int expectedResults,
                                         @NotNull IdeActivity activity) {
      myContext = context;
      myResultConsumer = resultConsumer;
      myProgressCounter = new AtomicInteger(expectedResults);
      myActivity = activity;
      myErrorsFlag = new AtomicBoolean(false);
      myAbortedFlag = new AtomicBoolean(false);
    }

    public void add(@NotNull ProjectTaskRunner.Result result, @NotNull Collection<? extends ProjectTask> tasks) {
      int inProgress = myProgressCounter.decrementAndGet();
      ProjectTaskState state = new ProjectTaskState() {
        @Override
        public boolean isSkipped() {
          return result.isAborted();
        }

        @Override
        public boolean isFailed() {
          return result.hasErrors();
        }
      };
      for (ProjectTask task : tasks) {
        myTasksState.put(task, state);
      }

      if (result.isAborted()) {
        myAbortedFlag.set(true);
      }
      if (result.hasErrors()) {
        myErrorsFlag.set(true);
      }
      if (inProgress <= 0) {
        try {
          myResultConsumer.accept(new MyResult(myContext, myTasksState, myAbortedFlag.get(), myErrorsFlag.get()));
        }
        finally {
          myActivity.finished();
        }
      }
    }
  }

  private static class MyResult implements Result {
    private final ProjectTaskContext myContext;
    private final boolean myAborted;
    private final boolean myErrors;
    private final Map<ProjectTask, ProjectTaskState> myTasksState;

    private MyResult(@NotNull ProjectTaskContext context,
                     @NotNull Map<ProjectTask, ProjectTaskState> tasksState,
                     boolean isAborted,
                     boolean hasErrors) {
      myContext = context;
      myTasksState = tasksState;
      myAborted = isAborted;
      myErrors = hasErrors;
    }

    @NotNull
    @Override
    public ProjectTaskContext getContext() {
      return myContext;
    }

    @Override
    public boolean isAborted() {
      return myAborted;
    }

    @Override
    public boolean hasErrors() {
      return myErrors;
    }

    @Override
    public boolean anyTaskMatches(@NotNull BiPredicate<? super ProjectTask, ? super ProjectTaskState> predicate) {
      return myTasksState.entrySet().stream().anyMatch(entry -> predicate.test(entry.getKey(), entry.getValue()));
    }
  }

  private static class ResultWrapper implements Result {
    private final Result myResult;

    private ResultWrapper(Result result) {myResult = result;}

    @NotNull
    @Override
    public ProjectTaskContext getContext() {
      return myResult.getContext();
    }

    @Override
    public boolean isAborted() {
      return myResult.isAborted();
    }

    @Override
    public boolean hasErrors() {
      return myResult.hasErrors();
    }

    @Override
    public boolean anyTaskMatches(@NotNull BiPredicate<? super ProjectTask, ? super ProjectTaskState> predicate) {
      return myResult.anyTaskMatches(predicate);
    }
  }
  //<editor-fold desc="Deprecated methods. To be removed in 2020.1">

  /**
   * @deprecated use {@link #run(ProjectTask)}
   */
  @Override
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public void run(@NotNull ProjectTask projectTask, @Nullable ProjectTaskNotification callback) {
    assertUnsupportedOperation(callback);
    notifyIfNeeded(run(projectTask), callback);
  }

  /**
   * @deprecated use {@link #run(ProjectTaskContext, ProjectTask)}
   */
  @Override
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public void run(@NotNull ProjectTaskContext context,
                  @NotNull ProjectTask projectTask,
                  @Nullable ProjectTaskNotification callback) {
    assertUnsupportedOperation(callback);
    notifyIfNeeded(run(context, projectTask), callback);
  }

  /**
   * @deprecated use {@link #buildAllModules()}
   */
  @Override
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public void buildAllModules(@Nullable ProjectTaskNotification callback) {
    assertUnsupportedOperation(callback);
    notifyIfNeeded(buildAllModules(), callback);
  }

  /**
   * @deprecated use {@link #rebuildAllModules()}
   */
  @Override
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public void rebuildAllModules(@Nullable ProjectTaskNotification callback) {
    assertUnsupportedOperation(callback);
    notifyIfNeeded(rebuildAllModules(), callback);
  }

  /**
   * @deprecated use {@link #build(Module[])}
   */
  @Override
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public void build(@NotNull Module[] modules, @Nullable ProjectTaskNotification callback) {
    assertUnsupportedOperation(callback);
    notifyIfNeeded(build(modules), callback);
  }

  /**
   * @deprecated use {@link #rebuild(Module[])}
   */
  @Override
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public void rebuild(@NotNull Module[] modules, @Nullable ProjectTaskNotification callback) {
    assertUnsupportedOperation(callback);
    notifyIfNeeded(rebuild(modules), callback);
  }

  /**
   * @deprecated use {@link #compile(VirtualFile[])}
   */
  @Override
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public void compile(@NotNull VirtualFile[] files, @Nullable ProjectTaskNotification callback) {
    assertUnsupportedOperation(callback);
    notifyIfNeeded(compile(files), callback);
  }

  /**
   * @deprecated use {@link #build(ProjectModelBuildableElement[])}
   */
  @Override
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public void build(@NotNull ProjectModelBuildableElement[] buildableElements, @Nullable ProjectTaskNotification callback) {
    assertUnsupportedOperation(callback);
    notifyIfNeeded(build(buildableElements), callback);
  }

  /**
   * @deprecated use {@link #rebuild(ProjectModelBuildableElement[])}
   */
  @Override
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public void rebuild(@NotNull ProjectModelBuildableElement[] buildableElements, @Nullable ProjectTaskNotification callback) {
    assertUnsupportedOperation(callback);
    notifyIfNeeded(rebuild(buildableElements), callback);
  }

  private static void notifyIfNeeded(@NotNull Promise<Result> promise, @Nullable ProjectTaskNotification callback) {
    if (callback != null) {
      promise
        .onSuccess(
          result -> callback.finished(result.getContext(), new ProjectTaskResult(result.isAborted(), result.hasErrors() ? 1 : 0, 0)))
        .onError(throwable -> callback.finished(new ProjectTaskContext(), new ProjectTaskResult(true, 0, 0)));
    }
  }

  private static void assertUnsupportedOperation(@Nullable ProjectTaskNotification callback) {
    if (callback instanceof ProjectTaskNotificationAdapter) {
      throw new UnsupportedOperationException("Please, provide implementation for non-deprecated methods");
    }
  }

  private static class ProjectTaskNotificationAdapter implements ProjectTaskNotification {
    private final AsyncPromise<Result> myPromise;
    private final ProjectTaskContext myContext;

    private ProjectTaskNotificationAdapter(@NotNull AsyncPromise<Result> promise, @NotNull ProjectTaskContext context) {
      myPromise = promise;
      myContext = context;
    }

    @Override
    public void finished(@NotNull ProjectTaskResult executionResult) {
      myPromise.setResult(new Result() {
        @NotNull
        @Override
        public ProjectTaskContext getContext() {
          return myContext;
        }

        @Override
        public boolean isAborted() {
          return executionResult.isAborted();
        }

        @Override
        public boolean hasErrors() {
          return executionResult.getErrors() > 0;
        }

        @Override
        public boolean anyTaskMatches(@NotNull BiPredicate<? super ProjectTask, ? super ProjectTaskState> predicate) {
          return executionResult.anyMatch(predicate);
        }
      });
    }
  }
  //</editor-fold>
}
