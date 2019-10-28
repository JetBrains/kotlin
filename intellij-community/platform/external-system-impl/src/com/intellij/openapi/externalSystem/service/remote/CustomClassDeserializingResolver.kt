// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.remote

import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectStreamClass
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

/**
 * Resolver, that can deserialize data nodes graph using plugin classloaders.
 */
class CustomClassDeserializingResolver<S : ExternalSystemExecutionSettings>(
  private val rawResolverDelegate: RawExternalSystemProjectResolver<S>,
  private val resolverDelegate: RemoteExternalSystemProjectResolver<S>
) : RemoteExternalSystemProjectResolver<S> by resolverDelegate {

  override fun resolveProjectInfo(id: ExternalSystemTaskId,
                                  projectPath: String,
                                  isPreviewMode: Boolean,
                                  settings: S?,
                                  resolverPolicy: ProjectResolverPolicy?): DataNode<ProjectData>? {
    val rawData = rawResolverDelegate.resolveProjectInfo(id, projectPath, isPreviewMode, settings, resolverPolicy) ?: return null
    val managerClassLoaders = (ExternalSystemManager.EP_NAME.iterable.asSequence()
                               + ProjectDataService.EP_NAME.extensions.asSequence())
      .map { it.javaClass.classLoader }
      .toSet()

    return MultiLoaderObjectInputStream(rawData.inputStream(), managerClassLoaders).readObject() as DataNode<ProjectData>
  }
}

/**
 * JDK serialization input stream, that attempts to load deserialized instance's class from a number of classloaders
 */
class MultiLoaderObjectInputStream(inputStream: InputStream, val loaders: Collection<ClassLoader>) : ObjectInputStream(inputStream) {

  override fun resolveClass(desc: ObjectStreamClass): Class<*> {
    loaders.forEach {
      try {
        return Class.forName(desc.name, false, it)
      }
      catch (e: ClassNotFoundException) {
        // Ignore
      }
    }
    return super.resolveClass(desc)
  }

  override fun resolveProxyClass(interfaces: Array<String>): Class<*> {
    loaders.forEach {
      try {
        return doResolveProxyClass(interfaces, it)
      }
      catch (e: ClassNotFoundException) {
        // Ignore
      }

    }
    return super.resolveProxyClass(interfaces)
  }

  private fun doResolveProxyClass(interfaces: Array<String>, loader: ClassLoader): Class<*> {
    var nonPublicLoader: ClassLoader? = null
    var hasNonPublicInterface = false

    // define proxy in class loader of non-public interface(s), if any
    val classObjs = arrayOfNulls<Class<*>>(interfaces.size)
    for (i in interfaces.indices) {
      val cl = Class.forName(interfaces[i], false, loader)
      if ((cl.modifiers and Modifier.PUBLIC) == 0) {
        if (hasNonPublicInterface) {
          if (nonPublicLoader !== cl.classLoader) {
            throw IllegalAccessError(
              "conflicting non-public interface class loaders")
          }
        }
        else {
          nonPublicLoader = cl.classLoader
          hasNonPublicInterface = true
        }
      }
      classObjs[i] = cl
    }
    try {
      return Proxy.getProxyClass(if (hasNonPublicInterface) nonPublicLoader else loader, *classObjs)
    }
    catch (e: IllegalArgumentException) {
      throw ClassNotFoundException(null, e)
    }

  }
}