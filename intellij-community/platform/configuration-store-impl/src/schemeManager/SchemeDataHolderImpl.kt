// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.schemeManager

import com.intellij.configurationStore.LOG
import com.intellij.configurationStore.SchemeDataHolder
import com.intellij.configurationStore.digest
import com.intellij.openapi.options.SchemeProcessor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.WriteExternalException
import com.intellij.util.ArrayUtilRt
import org.jdom.Element

internal class SchemeDataHolderImpl<out T : Any, in MUTABLE_SCHEME : T>(private val processor: SchemeProcessor<T, MUTABLE_SCHEME>,
                                                                        private val bytes: ByteArray,
                                                                        private val externalInfo: ExternalInfo) : SchemeDataHolder<MUTABLE_SCHEME> {
  override fun read(): Element {
    try {
      return JDOMUtil.load(bytes.inputStream())
    }
    catch (e: ProcessCanceledException) {
      throw e
    }
    catch (e: Exception) {
      throw RuntimeException("Cannot read ${externalInfo.fileName}", e)
    }
  }

  override fun updateDigest(scheme: MUTABLE_SCHEME) {
    try {
      updateDigest(processor.writeScheme(scheme) as Element)
    }
    catch (e: WriteExternalException) {
      LOG.error("Cannot update digest for ${externalInfo.fileName}", e)
    }
  }

  override fun updateDigest(data: Element?) {
    externalInfo.digest = data?.digest() ?: ArrayUtilRt.EMPTY_BYTE_ARRAY
  }
}