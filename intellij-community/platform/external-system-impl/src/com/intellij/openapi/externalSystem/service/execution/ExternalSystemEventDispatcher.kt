// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.build.output.BuildOutputInstantReaderImpl
import com.intellij.build.output.BuildOutputParser
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.util.SmartList
import org.jetbrains.annotations.ApiStatus
import java.io.Closeable

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class ExternalSystemEventDispatcher(task: ExternalSystemTask,
                                    progressListener: BuildProgressListener?) : Closeable, Appendable, BuildProgressListener {
  private var outputMessageDispatcher: ExternalSystemOutputMessageDispatcher? = null
  var stdOut: Boolean = true
    set(value) {
      field = value
      outputMessageDispatcher?.stdOut = value
    }

  init {
    val buildOutputParsers = SmartList<BuildOutputParser>()
    if (progressListener != null) {
      ExternalSystemOutputParserProvider.EP_NAME.extensions.forEach {
        if (task.id.projectSystemId == it.externalSystemId) {
          buildOutputParsers.addAll(it.getBuildOutputParsers(task))
        }
      }

      var foundFactory: ExternalSystemOutputDispatcherFactory? = null
      EP_NAME.extensions.forEach {
        if (task.id.projectSystemId == it.externalSystemId) {
          if (foundFactory != null) {
            throw RuntimeException("'" + EP_NAME.name + "' extension should be one per external system")
          }
          foundFactory = it
        }
      }
      outputMessageDispatcher = foundFactory?.create(task.id, progressListener, buildOutputParsers)
                                ?: DefaultOutputMessageDispatcher(task.id, progressListener, buildOutputParsers)
    }
  }

  override fun onEvent(event: BuildEvent) {
    outputMessageDispatcher?.onEvent(event)
  }

  override fun append(csq: CharSequence): Appendable? {
    outputMessageDispatcher?.append(csq)
    return this
  }

  override fun append(csq: CharSequence, start: Int, end: Int): Appendable? {
    outputMessageDispatcher?.append(csq, start, end)
    return this
  }

  override fun append(c: Char): Appendable? {
    outputMessageDispatcher?.append(c)
    return this
  }

  override fun close() {
    outputMessageDispatcher?.close()
  }

  companion object {
    private val EP_NAME = ExtensionPointName.create<ExternalSystemOutputDispatcherFactory>("com.intellij.externalSystemOutputDispatcher")
  }
}

private class DefaultOutputMessageDispatcher(buildId: Any,
                                             private val buildProgressListener: BuildProgressListener,
                                             parsers: List<BuildOutputParser>) :
  BuildOutputInstantReaderImpl(buildId, buildProgressListener, parsers), ExternalSystemOutputMessageDispatcher {
  override var stdOut: Boolean = true

  override fun onEvent(event: BuildEvent) = buildProgressListener.onEvent(event)
}

interface ExternalSystemOutputDispatcherFactory {
  val externalSystemId: Any?
  fun create(buildId: Any,
             buildProgressListener: BuildProgressListener,
             parsers: List<BuildOutputParser>): ExternalSystemOutputMessageDispatcher
}

interface ExternalSystemOutputMessageDispatcher : Closeable, Appendable, BuildProgressListener {
  var stdOut: Boolean
}
