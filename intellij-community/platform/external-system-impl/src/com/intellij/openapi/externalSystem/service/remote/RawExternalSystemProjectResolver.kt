// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.remote

import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemService
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.rmi.RemoteException

/**
 * Interface for wrappers of RemoteExternalSystemProjectResolver, that operate on results as byte arrays.
 */
interface RawExternalSystemProjectResolver<S : ExternalSystemExecutionSettings>
  : RemoteExternalSystemService<S> {

  @Throws(RemoteException::class)
  fun resolveProjectInfo(id: ExternalSystemTaskId,
                         projectPath: String,
                         isPreviewMode: Boolean,
                         settings: S?,
                         resolverPolicy: ProjectResolverPolicy?): ByteArray?

  companion object {
    val NULL_OBJECT = object: RawExternalSystemProjectResolver<ExternalSystemExecutionSettings> {
      override fun resolveProjectInfo(id: ExternalSystemTaskId,
                                      projectPath: String,
                                      isPreviewMode: Boolean,
                                      settings: ExternalSystemExecutionSettings?,
                                      resolverPolicy: ProjectResolverPolicy?): ByteArray? = null

      override fun getTasksInProgress(): MutableMap<ExternalSystemTaskType, MutableSet<ExternalSystemTaskId>> = mutableMapOf()


      override fun setNotificationListener(notificationListener: ExternalSystemTaskNotificationListener) {
      }

      override fun isTaskInProgress(id: ExternalSystemTaskId): Boolean = false

      override fun cancelTask(id: ExternalSystemTaskId): Boolean = true

      override fun setSettings(settings: ExternalSystemExecutionSettings) {
      }

    }
  }
}

/**
 * Wrapper that allows caller to manually deserialize call result (potentially remote) of
 * RemoteExternalSystemProjectResolver.resolveProjectInfo
 */
class RawExternalSystemProjectResolverImpl<S: ExternalSystemExecutionSettings>(
  private val resolverDelegate: RemoteExternalSystemProjectResolver<S>
)
  : AbstractRemoteExternalSystemService<S>(), RawExternalSystemProjectResolver<S> {
  override fun cancelTask(id: ExternalSystemTaskId): Boolean = resolverDelegate.cancelTask(id)

  override fun resolveProjectInfo(id: ExternalSystemTaskId,
                                  projectPath: String,
                                  isPreviewMode: Boolean,
                                  settings: S?,
                                  resolverPolicy: ProjectResolverPolicy?): ByteArray? {
    val result = resolverDelegate.resolveProjectInfo(id, projectPath, isPreviewMode, settings, resolverPolicy) ?: return null
    val outputStream = ByteArrayOutputStream()
    ObjectOutputStream(outputStream).writeObject(result)
    return outputStream.toByteArray()
  }

}