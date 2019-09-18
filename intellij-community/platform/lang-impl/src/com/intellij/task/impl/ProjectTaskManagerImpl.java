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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectModelBuildableElement;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.task.*;
import com.intellij.ui.GuiUtils;
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

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
          sendAbortedNotify(context, new ListenerNotificator(callback));
          activity.finished();
          return;
        }
      }

      if (toRun.isEmpty()) {
        sendSuccessNotify(context, new ListenerNotificator(callback));
        activity.finished();
        return;
      }

      ProjectTaskResultsAggregator callbacksCollector =
        new ProjectTaskResultsAggregator(context, new ListenerNotificator(callback), toRun.size(), activity);
      for (Pair<ProjectTaskRunner, Collection<? extends ProjectTask>> pair : toRun) {
        ProjectTaskRunnerNotification notification = new ProjectTaskRunnerNotification(pair.second, callbacksCollector);
        if (pair.second.isEmpty()) {
          sendSuccessNotify(context, notification);
        }
        else {
          ProjectTaskRunner runner = pair.first;
          if (context.isCollectionOfGeneratedFilesEnabled() && !runner.isFileGeneratedEventsSupported()) {
            pair.second.stream()
              .filter(ModuleBuildTask.class::isInstance)
              .map(task -> ((ModuleBuildTask)task).getModule())
              .forEach(module -> context.addDirtyOutputPathsProvider(moduleOutputPathsProvider(module)));
          }
          runner.run(myProject, context, notification, pair.second);
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

  @NotNull
  private static Supplier<List<String>> moduleOutputPathsProvider(@NotNull Module module) {
    return () -> ReadAction.compute(() -> {
      return JBIterable.of(OrderEnumerator.orderEntries(module).getClassesRoots())
        .filterMap(file -> file.isDirectory() && !file.getFileSystem().isReadOnly() ? file.getPath() : null).toList();
    });
  }

  public final void addListener(@NotNull ProjectTaskManagerListener listener) {
    myListeners.add(listener);
  }

  private static void sendSuccessNotify(@NotNull ProjectTaskContext context, @Nullable ProjectTaskNotification notification) {
    if (notification != null) {
      notification.finished(context, new ProjectTaskResult(false, 0, 0));
    }
  }

  private static void sendAbortedNotify(@NotNull ProjectTaskContext context, @Nullable ProjectTaskNotification notification) {
    if (notification != null) {
      notification.finished(context, new ProjectTaskResult(true, 0, 0));
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
      sendSuccessNotify(context, callback);
    }

    @Override
    public boolean canRun(@NotNull ProjectTask projectTask) {
      return true;
    }
  }

  private class ListenerNotificator implements ProjectTaskNotification {
    @Nullable private final ProjectTaskNotification myDelegate;

    private ListenerNotificator(@Nullable ProjectTaskNotification delegate) {
      myDelegate = delegate;
    }

    @Override
    public void finished(@NotNull ProjectTaskContext context, @NotNull ProjectTaskResult executionResult) {
      if (!executionResult.isAborted() && executionResult.getErrors() == 0) {
        // do not run after tasks on EDT
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          try {
            for (ProjectTaskManagerListener listener : myListeners) {
              listener.afterRun(context, executionResult);
            }
            notify(context, executionResult);
          }
          catch (ExecutionException e) {
            LOG.debug(e);
            notify(context, new ProjectTaskResult(
              false, executionResult.getErrors() + 1, executionResult.getWarnings(), executionResult.getTasksState()));
          }
        });
      }
      else {
        notify(context, executionResult);
      }
    }

    private void notify(@NotNull ProjectTaskContext context, @NotNull ProjectTaskResult executionResult) {
      GuiUtils.invokeLaterIfNeeded(() -> {
        if (!myProject.isDisposed()) {
          myEventPublisher.finished(context, executionResult);
        }
        if (myDelegate != null) {
          myDelegate.finished(context, executionResult);
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

    // support ProjectTaskRunners which still uses deprecated method
    @Override
    public void finished(@NotNull ProjectTaskResult executionResult) {
      finished(myAggregator.myContext, executionResult);
    }

    @Override
    public void finished(@NotNull ProjectTaskContext context, @NotNull ProjectTaskResult result) {
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
    private final ProjectTaskContext myContext;
    private final ProjectTaskNotification myDelegate;
    private final AtomicInteger myProgressCounter;
    private final IdeActivity myActivity;
    private final AtomicInteger myErrorsCounter;
    private final AtomicInteger myWarningsCounter;
    private final AtomicBoolean myAbortedFlag;
    private final Map<ProjectTask, ProjectTaskState> myTasksState = ContainerUtil.newConcurrentMap();

    private ProjectTaskResultsAggregator(@NotNull ProjectTaskContext context,
                                         @NotNull ProjectTaskNotification delegate,
                                         int expectedResults,
                                         @NotNull IdeActivity activity) {
      myContext = context;
      myDelegate = delegate;
      myProgressCounter = new AtomicInteger(expectedResults);
      myActivity = activity;
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
        try {
          ProjectTaskResult result = new ProjectTaskResult(myAbortedFlag.get(), allErrors, allWarnings, myTasksState);
          myDelegate.finished(myContext, result);
        }
        finally {
          myActivity.finished();
        }
      }
    }
  }
}
