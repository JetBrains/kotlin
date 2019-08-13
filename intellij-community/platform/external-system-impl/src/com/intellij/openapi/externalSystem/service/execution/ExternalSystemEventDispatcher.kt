// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.build.BuildEventDispatcher
import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.build.output.BuildOutputInstantReaderImpl
import com.intellij.build.output.BuildOutputParser
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.util.SmartList
import org.jetbrains.annotations.ApiStatus
import java.io.Closeable

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class ExternalSystemEventDispatcher(taskId: ExternalSystemTaskId,
                                    progressListener: BuildProgressListener?,
                                    appendOutputToMainConsole: Boolean) : BuildEventDispatcher {
  constructor(taskId: ExternalSystemTaskId, progressListener: BuildProgressListener?) : this(taskId, progressListener, true)

  private var outputMessageDispatcher: ExternalSystemOutputMessageDispatcher? = null
  private var isStdOut: Boolean = true

  override fun setStdOut(stdOut: Boolean) {
    this.isStdOut = stdOut
    outputMessageDispatcher?.stdOut = stdOut
  }

  init {
    val buildOutputParsers = SmartList<BuildOutputParser>()
    if (progressListener != null) {
      ExternalSystemOutputParserProvider.EP_NAME.extensions.forEach {
        if (taskId.projectSystemId == it.externalSystemId) {
          buildOutputParsers.addAll(it.getBuildOutputParsers(taskId))
        }
      }

      var foundFactory: ExternalSystemOutputDispatcherFactory? = null
      EP_NAME.extensions.forEach {
        if (taskId.projectSystemId == it.externalSystemId) {
          if (foundFactory != null) {
            throw RuntimeException("'" + EP_NAME.name + "' extension should be one per external system")
          }
          foundFactory = it
        }
      }
      outputMessageDispatcher = foundFactory?.create(taskId, progressListener, appendOutputToMainConsole, buildOutputParsers)
                                ?: DefaultOutputMessageDispatcher(taskId, progressListener, buildOutputParsers)
    }
  }

  override fun onEvent(buildId: Any, event: BuildEvent) {
    outputMessageDispatcher?.onEvent(buildId, event)
  }

  override fun append(csq: CharSequence): BuildEventDispatcher? {
    outputMessageDispatcher?.append(csq)
    return this
  }

  override fun append(csq: CharSequence, start: Int, end: Int): BuildEventDispatcher? {
    outputMessageDispatcher?.append(csq, start, end)
    return this
  }

  override fun append(c: Char): BuildEventDispatcher? {
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
  BuildOutputInstantReaderImpl(buildId, buildId, buildProgressListener, parsers), ExternalSystemOutputMessageDispatcher {
  override var stdOut: Boolean = true

  override fun onEvent(buildId: Any, event: BuildEvent) = buildProgressListener.onEvent(buildId, event)
}

interface ExternalSystemOutputDispatcherFactory {
  val externalSystemId: Any?
  fun create(buildId: Any,
             buildProgressListener: BuildProgressListener,
             appendOutputToMainConsole: Boolean,
             parsers: List<BuildOutputParser>): ExternalSystemOutputMessageDispatcher
}

interface ExternalSystemOutputMessageDispatcher : Closeable, Appendable, BuildProgressListener {
  var stdOut: Boolean
}
