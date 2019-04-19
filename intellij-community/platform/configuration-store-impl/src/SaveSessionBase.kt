// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.impl.stores.FileStorageCoreUtil
import com.intellij.openapi.util.JDOMExternalizable
import com.intellij.openapi.util.WriteExternalException
import com.intellij.openapi.vfs.LargeFileWriteRequestor
import com.intellij.openapi.vfs.SafeWriteRequestor
import org.jdom.Element

abstract class SaveSessionBase : SaveSessionProducer, SafeWriteRequestor, LargeFileWriteRequestor {
  final override fun setState(component: Any?, componentName: String, state: Any?) {
    if (state == null) {
      setSerializedState(componentName, null)
      return
    }

    val element: Element?
    try {
      element = serializeState(state)
    }
    catch (e: WriteExternalException) {
      LOG.debug(e)
      return
    }
    catch (e: Throwable) {
      LOG.error("Unable to serialize $componentName state", e)
      return
    }

    setSerializedState(componentName, element)
  }

  protected abstract fun setSerializedState(componentName: String, element: Element?)
}

internal fun serializeState(state: Any): Element? {
  @Suppress("DEPRECATION")
  when (state) {
    is Element -> return state
    is JDOMExternalizable -> {
      val element = Element(FileStorageCoreUtil.COMPONENT)
      state.writeExternal(element)
      return element
    }
    else -> return state.serialize()
  }
}
