// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ReflectionUtil.getDeclaredField
import com.intellij.util.ReflectionUtil.getField
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.lang.JavaVersion
import org.gradle.initialization.BuildCancellationToken
import org.gradle.internal.classpath.ClassPath
import org.gradle.internal.classpath.DefaultClassPath
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.tooling.CancellationToken
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.internal.consumer.DefaultGradleConnector
import org.gradle.tooling.internal.consumer.Distribution
import org.gradle.tooling.internal.protocol.InternalBuildProgressListener
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.internal.daemon.GradleDaemonServices
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.service.project.DistributionFactoryExt
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.tooling.loader.rt.MarkerRt
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.Function

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Internal
@Service
class GradleConnectorService(@Suppress("UNUSED_PARAMETER") project: Project) : Disposable {

  private val connectorsMap = ConcurrentHashMap<String, GradleProjectConnection>()

  override fun dispose() {
    if (ApplicationManager.getApplication().isUnitTestMode) return
    disconnectGradleConnections()
    stopIdleDaemonsOfOldVersions()
  }

  private fun stopIdleDaemonsOfOldVersions() {
    if (DISABLE_STOP_OLD_IDLE_DAEMONS) return
    try {
      if (ProjectUtil.getOpenProjects().isEmpty()) {
        val gradleVersion_6_5 = GradleVersion.version("6.5")
        val idleDaemons = GradleDaemonServices.getDaemonsStatus().filter {
          it.status.toLowerCase() == "idle" &&
          GradleVersion.version(it.version) < gradleVersion_6_5
        }
        if (idleDaemons.isNotEmpty()) {
          GradleDaemonServices.stopDaemons(idleDaemons)
        }
      }
    }
    catch (e: Exception) {
      LOG.warn("Failed to stop Gradle daemons during project close", e)
    }
  }

  private fun disconnectGradleConnections() {
    connectorsMap.values.forEach(GradleProjectConnection::disconnect)
    connectorsMap.clear()
  }

  private fun getConnection(
    connectorParams: ConnectorParams,
    taskId: ExternalSystemTaskId?,
    listener: ExternalSystemTaskNotificationListener?,
    cancellationToken: CancellationToken?
  ): ProjectConnection {
    return connectorsMap.compute(connectorParams.projectPath) { _, conn ->
      if (connectorParams == conn?.params) {
        return@compute conn
      }
      val newConnector = createConnector(connectorParams)
      val newConnection = newConnector.connect()
      check(newConnection != null) {
        "Can't create connection to the target project via gradle tooling api. Project path: '${connectorParams.projectPath}'"
      }
      workaroundJavaVersionIssueIfNeeded(newConnection, taskId, listener, cancellationToken)

      if (conn != null && connectorParams != conn.params) {
        // close obsolete connection, can not disconnect the connector here - it may cause build cancel for the new connection operations
        val unwrappedConnection = conn.connection as WrappedConnection
        unwrappedConnection.delegate.close()
      }
      val wrappedConnection = WrappedConnection(newConnection)
      return@compute GradleProjectConnection(connectorParams, newConnector, wrappedConnection)
    }!!.connection
  }

  private class GradleProjectConnection(val params: ConnectorParams, val connector: GradleConnector, val connection: ProjectConnection) {
    fun disconnect() {
      try {
        connector.disconnect()
      }
      catch (e: Exception) {
        LOG.warn("Failed to disconnect Gradle connector during project close. Project path: '${params.projectPath}'", e)
      }
    }
  }

  private class WrappedConnection(val delegate: ProjectConnection) : ProjectConnection by delegate {
    override fun close() {
      throw IllegalStateException("This connection should not be closed explicitly.")
    }
  }

  private data class ConnectorParams(
    val projectPath: String,
    val serviceDirectory: String?,
    val distributionType: DistributionType?,
    val gradleHome: String?,
    val wrapperPropertyFile: String?,
    val verboseProcessing: Boolean?,
    val ttlMs: Int?
  )

  companion object {
    private val LOG = logger<GradleConnectorService>()

    /** disable stop IDLE Gradle daemons on IDE project close. Applicable for Gradle versions w/o disconnect support (older than 6.5). */
    private val DISABLE_STOP_OLD_IDLE_DAEMONS = java.lang.Boolean.getBoolean("idea.gradle.disableStopIdleDaemonsOnProjectClose")

    private val REPORTED_JAVA11_ISSUE = ContainerUtil.newConcurrentSet<String>()

    @JvmStatic
    private fun getInstance(projectPath: String, taskId: ExternalSystemTaskId?): GradleConnectorService? {
      var project = taskId?.findProject()
      if (project == null) {
        for (openProject in ProjectUtil.getOpenProjects()) {
          val projectBasePath = openProject.basePath ?: continue
          if (FileUtil.isAncestor(projectBasePath, projectPath, false)) {
            project = openProject
            break
          }
        }
      }
      return project?.getService(GradleConnectorService::class.java)
    }

    @JvmStatic
    fun <R : Any?> withGradleConnection(
      projectPath: String,
      taskId: ExternalSystemTaskId?,
      executionSettings: GradleExecutionSettings? = null,
      listener: ExternalSystemTaskNotificationListener? = null,
      cancellationToken: CancellationToken? = null,
      function: Function<ProjectConnection, R>
    ): R {
      val connectionParams = ConnectorParams(
        projectPath,
        executionSettings?.serviceDirectory,
        executionSettings?.distributionType,
        executionSettings?.gradleHome,
        executionSettings?.wrapperPropertyFile,
        executionSettings?.isVerboseProcessing,
        executionSettings?.remoteProcessIdleTtlInMs?.toInt()
      )
      val connectionService = getInstance(projectPath, taskId)
      if (connectionService != null) {
        val connection = connectionService.getConnection(connectionParams, taskId, listener, cancellationToken)
        return function.apply(connection)
      }
      else {
        val newConnector = createConnector(connectionParams)
        val connection = newConnector.connect()
        workaroundJavaVersionIssueIfNeeded(connection, taskId, listener, cancellationToken)
        return connection.use(function::apply)
      }
    }

    private fun createConnector(connectorParams: ConnectorParams): GradleConnector {
      val connector = GradleConnector.newConnector()
      val projectDir = File(connectorParams.projectPath)
      val gradleUserHome = if (connectorParams.serviceDirectory == null) null else File(connectorParams.serviceDirectory)

      if (connectorParams.distributionType == DistributionType.LOCAL) {
        val gradleHome = if (connectorParams.gradleHome == null) null else File(connectorParams.gradleHome)
        if (gradleHome != null) {
          connector.useInstallation(gradleHome)
        }
      }
      else if (connectorParams.distributionType == DistributionType.WRAPPED) {
        if (connectorParams.wrapperPropertyFile != null) {
          DistributionFactoryExt.setWrappedDistribution(connector, connectorParams.wrapperPropertyFile, gradleUserHome, projectDir)
        }
      }

      // Setup Grade user home if necessary
      if (gradleUserHome != null) {
        connector.useGradleUserHomeDir(gradleUserHome)
      }
      // Setup logging if necessary
      if (connectorParams.verboseProcessing == true && connector is DefaultGradleConnector) {
        connector.setVerboseLogging(true)
      }
      // do not spawn gradle daemons during test execution
      val app = ApplicationManager.getApplication()
      val ttl = if (app != null && app.isUnitTestMode) 10000 else connectorParams.ttlMs ?: -1
      if (ttl > 0 && connector is DefaultGradleConnector) {
        connector.daemonMaxIdleTime(ttl, TimeUnit.MILLISECONDS)
      }

      connector.forProjectDirectory(projectDir)
      return connector
    }

    // workaround for https://github.com/gradle/gradle/issues/8431
    // TODO should be removed when the issue will be fixed at the Gradle tooling api side
    private fun workaroundJavaVersionIssueIfNeeded(
      connection: ProjectConnection,
      taskId: ExternalSystemTaskId?,
      listener: ExternalSystemTaskNotificationListener?,
      cancellationToken: CancellationToken?
    ) {
      val unwrappedConnection = if (connection is WrappedConnection) connection.delegate else connection
      var buildRoot: String? = null
      if (Registry.`is`("gradle.java11.issue.workaround", true)
          && taskId != null && listener != null && JavaVersion.current().feature > 8) {
        try {
          val environment = GradleExecutionHelper.getBuildEnvironment(unwrappedConnection, taskId, listener, cancellationToken)
          if (environment != null) {
            try {
              buildRoot = environment.buildIdentifier.rootDir.path
            }
            catch (ignore: java.lang.Exception) {
            }
          }
          val gradleVersion = environment?.gradle?.gradleVersion
          if (gradleVersion == null || GradleVersion.version(gradleVersion).baseVersion < GradleVersion.version("4.7")) {
            val conn = getField<Any>(unwrappedConnection.javaClass, unwrappedConnection, null, "connection")
            val actionExecutor = getField<Any>(conn.javaClass, conn, null, "actionExecutor")
            val actionExecutorDelegate = getField<Any>(actionExecutor.javaClass, actionExecutor, null, "delegate")
            val delegateActionExecutor = getField<Any>(actionExecutorDelegate.javaClass, actionExecutorDelegate, null, "actionExecutor")
            val delegateActionExecutorDelegate = getField<Any>(delegateActionExecutor.javaClass, delegateActionExecutor, null, "delegate")
            val distributionField = getDeclaredField(delegateActionExecutorDelegate.javaClass, "distribution")
            distributionField!!.set(delegateActionExecutorDelegate,
                                    DistributionWrapper(distributionField.get(delegateActionExecutorDelegate) as Distribution))
          }
        }
        catch (t: Throwable) {
          val buildId = taskId.ideProjectId + StringUtil.notNullize(buildRoot)
          if (REPORTED_JAVA11_ISSUE.add(buildId)) {
            LOG.error(t)
          }
          else {
            LOG.debug(t)
          }
        }
      }
    }
  }

  /**
   * workaround for https://github.com/gradle/gradle/issues/8431
   * TODO should be removed when the issue will be fixed at the Gradle tooling api side
   */
  private class DistributionWrapper(private val myDistribution: Distribution) : Distribution {
    private val myRtJarFile: File = File(FileUtil.toCanonicalPath(PathManager.getJarPathForClass(MarkerRt::class.java)))

    override fun getDisplayName(): String = myDistribution.displayName

    override fun getToolingImplementationClasspath(
      factory: ProgressLoggerFactory?,
      listener: InternalBuildProgressListener?,
      file: File?,
      token: BuildCancellationToken?
    ): ClassPath {
      val classpath = myDistribution.getToolingImplementationClasspath(factory, listener, file, token)
      return DefaultClassPath.of(myRtJarFile).plus(classpath)
    }

  }
}