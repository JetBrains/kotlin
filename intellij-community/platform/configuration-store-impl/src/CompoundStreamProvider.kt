// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.RoamingType
import com.intellij.util.containers.ContainerUtil
import java.io.InputStream

class CompoundStreamProvider : StreamProvider {
  val providers = ContainerUtil.createConcurrentList<StreamProvider>()

  override val enabled: Boolean
    get() = providers.any { it.enabled }

  override val isExclusive: Boolean
    get() = providers.any { it.isExclusive }

  val isExclusivelyEnabled: Boolean
    get() = enabled && isExclusive

  override fun isApplicable(fileSpec: String, roamingType: RoamingType) = providers.any { it.isApplicable(fileSpec, roamingType) }

  override fun read(fileSpec: String, roamingType: RoamingType, consumer: (InputStream?) -> Unit) = providers.any { it.read(fileSpec, roamingType, consumer) }

  override fun processChildren(path: String,
                               roamingType: RoamingType,
                               filter: Function1<String, Boolean>,
                               processor: Function3<String, InputStream, Boolean, Boolean>): Boolean {
    return providers.any { it.processChildren(path, roamingType, filter, processor) }
  }

  override fun write(fileSpec: String, content: ByteArray, size: Int, roamingType: RoamingType) {
    providers.forEach {
      if (it.isApplicable(fileSpec, roamingType)) {
        it.write(fileSpec, content, size, roamingType)
      }
    }
  }

  override fun delete(fileSpec: String, roamingType: RoamingType) = providers.any { it.delete(fileSpec, roamingType) }
}