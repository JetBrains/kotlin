// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project

import com.intellij.openapi.progress.ProcessCanceledException
import org.gradle.tooling.*
import org.gradle.tooling.model.BuildModel
import org.gradle.tooling.model.ProjectModel
import org.gradle.tooling.model.idea.BasicIdeaProject
import org.gradle.tooling.model.idea.IdeaProject
import org.jetbrains.plugins.gradle.model.ModelsHolder
import org.jetbrains.plugins.gradle.model.ProjectImportAction
import org.jetbrains.plugins.gradle.model.ProjectImportAction.AllModels
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * This class handles setting up and running the [BuildActionExecuter] it deals with calling the correct APIs based on the version of
 * Gradle that is present.
 *
 * In order to do this we require the current [resolverCtx], the [buildAction] that should be run and whether or not
 * to initialize task running for the Gradle operation ([initializeTaskExecution]).
 *
 * We also require the [helper] which will be used to set up each [BuildActionExecuter]. We may need to set up more than one
 * of these if the version of Gradle that we are connecting to doesn't support the certain features.
 *
 * We have three different cases which will be handled in [fetchModels] we will try the most recent first, falling back to the older
 * ones if a [GradleConnectionException] is thrown.
 * These three cases are as follows:
 *   (Gradle 4.8 and above) Using the PhasedBuildActionExecuter, this allows us to inject build actions into different parts of the
 *                          Gradle build. It also allows us to run schedule tasks to be run after fetching the models.
 *   (Gradle 1.8 and above) Using the [BuildActionExecuter]
 *   (Gradle 1.7 and below) Using [GradleExecutionHelper.getModelBuilder]
 */
class BuildActionRunner(
  private val resolverCtx: ProjectResolverContext,
  private val buildAction: ProjectImportAction,
  private val initializeTaskExecution: Boolean,
  private val settings: GradleExecutionSettings,
  private val helper: GradleExecutionHelper
) {
  // This queue is used to synchronize the result of the models from the result handler passed to the projectsLoaded()
  // method of the BuildActionExecutor. Either the result or an exception will be added to the queue in the handler.
  // This will then be picked up and handled by the thread that is handling sync.
  private val resultQueue = ArrayBlockingQueue<Any>(1)
  private val modelsHandler: IntermediateResultHandler<AllModels?> = IntermediateResultHandler { allModels ->
    if (allModels == null) {
      resultQueue.add(IllegalStateException("Unable to get project model for the project: " + resolverCtx.projectPath))
    }
    else {
      resultQueue.add(allModels)
    }
  }

  /**
   * Fetches the [AllModels] that have been populated as a result of running the [ProjectImportAction] against the Gradle tooling API.
   *
   * This method returns as soon as the all models have been obtained.
   *
   * The [projectsLoadedCallBack] will be run as soon as the models available when Gradle loaded projects before the build has finished.
   * The [buildFinishedCallBack] will be run when the complete Gradle operation has finished (including any tasks that need to be run).
   *
   * For Gradle versions below 1.8 we fall back to the old [org.gradle.tooling.ModelBuilder] api, using the [helper] and
   * [settings].
   */
  fun fetchModels(projectsLoadedCallBack: Consumer<ModelsHolder<BuildModel, ProjectModel>>,
                  buildFinishedCallBack: Consumer<GradleConnectionException?>): AllModels {
    // First try with the phased build executor
    createPhasedExecuter(projectsLoadedCallBack).run(BuildActionResultHandler(buildFinishedCallBack))

    val phasedResult = takeQueueResultBlocking()
    // If we have a non-unsupported version exception pass the failure up to be dealt with by the ExternalSystem
    if (phasedResult is Throwable && phasedResult !is UnsupportedVersionException) throw phasedResult
    // If we have a result, return it
    if (phasedResult !is Throwable) return phasedResult as AllModels

    resolverCtx.checkCancelled()

    // Otherwise we have a GradleConnectionException, try using the non-phased build action executer.
    // If the handler fails
    createDefaultExecuter().run(BuildActionResultHandler(buildFinishedCallBack))

    val result = takeQueueResultBlocking()
    // If we have a non-unsupported version exception pass the failure up to be dealt with by the ExternalSystem
    if (result is Throwable && result !is UnsupportedVersionException) throw result
    // If we have a result, return it
    if (result !is Throwable) return result as AllModels

    // BuildActions are unsupported in this version of Gradle, try the model builder API.
    resolverCtx.checkCancelled()

    // Old gradle distribution version used (before ver. 1.8)
    // fallback to use ModelBuilder gradle tooling API
    val aClass = if (resolverCtx.isPreviewMode) BasicIdeaProject::class.java else IdeaProject::class.java
    val modelBuilder = helper.getModelBuilder(
      aClass,
      resolverCtx.externalSystemTaskId,
      settings,
      resolverCtx.connection,
      resolverCtx.listener)

    buildFinishedCallBack.accept(null)

    return AllModels(modelBuilder.get())
  }

  private fun takeQueueResultBlocking(): Any {
    var obtainedResult: Any? = null
    // Every second check to ensure the user didn't cancel the operation. If something goes really wrong with the Gradle connection
    // threads then at least the user should be able to cancel the refresh process.
    while (obtainedResult == null) {
      resolverCtx.checkCancelled()
      obtainedResult = resultQueue.poll(1, TimeUnit.SECONDS)
    }

    return obtainedResult
  }

  /**
   * Creates the [BuildActionExecuter] to be used to run the [ProjectImportAction].
   */
  private fun createPhasedExecuter(projectsLoadedCallBack: Consumer<ModelsHolder<BuildModel, ProjectModel>>): BuildActionExecuter<Void> {
    buildAction.prepareForPhasedExecuter()
    val executer = resolverCtx.connection.action()
      .projectsLoaded(buildAction, IntermediateResultHandler {
        try {
          projectsLoadedCallBack.accept(it)
        } catch (e: ProcessCanceledException) {
          resolverCtx.cancellationTokenSource?.cancel()
        }
      })
      .buildFinished(buildAction, modelsHandler)
      .build()
    executer.prepare()
    executer.setCancellationToken(resolverCtx)
    if (initializeTaskExecution) executer.forTasks(emptyList())
    return executer
  }

  private fun createDefaultExecuter(): BuildActionExecuter<AllModels> {
    buildAction.prepareForNonPhasedExecuter()
    val executer = resolverCtx.connection.action(buildAction)
    executer.prepare()
    executer.setCancellationToken(resolverCtx)
    return executer
  }

  private fun BuildActionExecuter<*>.prepare() = GradleExecutionHelper.prepare(this, resolverCtx.externalSystemTaskId, settings,
                                                                               resolverCtx.listener, resolverCtx.connection)

  private inner class BuildActionResultHandler(
    val buildFinishedCallBack: Consumer<GradleConnectionException?>
  ): ResultHandler<Any> {
    override fun onFailure(connectionException: GradleConnectionException?) {
      resultQueue.add(connectionException)
      buildFinishedCallBack.accept(connectionException)
    }

    /**
     * The parameter [allModels] will be null if running from the Phased executer as to obtain the models via [modelsHandler].
     * However if it is not null then we must be running from the normal build action excuter and thus the [allModels] must be
     * added to the queue to unblock the main thread.
     */
    override fun onComplete(allModels: Any?) {
      if (allModels != null) {
        resultQueue.add(allModels)
      }
      buildFinishedCallBack.accept(null)
    }
  }
}

private fun BuildActionExecuter<*>.setCancellationToken(resolverCtx: ProjectResolverContext) {
  resolverCtx.cancellationTokenSource?.token()?.let { token ->
    withCancellationToken(token)
  }
}
