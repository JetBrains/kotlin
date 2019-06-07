// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.build.output

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.DuplicateMessageAware
import com.intellij.build.events.StartEvent
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.output.BuildOutputInstantReaderImpl
import com.intellij.build.output.BuildOutputParser
import com.intellij.build.output.LineProcessor
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemOutputDispatcherFactory
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemOutputMessageDispatcher
import org.apache.commons.lang.ClassUtils
import org.gradle.api.logging.LogLevel
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class GradleOutputDispatcherFactory : ExternalSystemOutputDispatcherFactory {
  override val externalSystemId: Any? = GradleConstants.SYSTEM_ID

  override fun create(buildId: Any,
                      buildProgressListener: BuildProgressListener,
                      appendOutputToMainConsole: Boolean,
                      parsers: List<BuildOutputParser>): ExternalSystemOutputMessageDispatcher {
    return GradleOutputMessageDispatcher(buildId, buildProgressListener, appendOutputToMainConsole, parsers)
  }

  private class GradleOutputMessageDispatcher(private val buildId: Any,
                                              private val myBuildProgressListener: BuildProgressListener,
                                              private val appendOutputToMainConsole: Boolean,
                                              private val parsers: List<BuildOutputParser>) : ExternalSystemOutputMessageDispatcher {
    override var stdOut: Boolean = true
    private val lineProcessor: LineProcessor
    private val myRootReader: BuildOutputInstantReaderImpl
    private var myCurrentReader: BuildOutputInstantReaderImpl
    private val tasksOutputReaders = mutableMapOf<String, BuildOutputInstantReaderImpl>()
    private val tasksEventIds = mutableMapOf<String, Any>()

    init {
      val deferredRootEvents = mutableListOf<BuildEvent>()
      myRootReader = object : BuildOutputInstantReaderImpl(buildId, buildId, BuildProgressListener { _: Any, event: BuildEvent ->
        var buildEvent = event
        val parentId = buildEvent.parentId
        if (parentId != buildId && parentId is String) {
          val taskEventId = tasksEventIds[parentId]
          if (taskEventId != null) {
            buildEvent = BuildEventInvocationHandler.wrap(event, taskEventId)
          }
        }
        if (buildEvent is DuplicateMessageAware) {
          deferredRootEvents += buildEvent
        }
        else {
          myBuildProgressListener.onEvent(buildId, buildEvent)
        }
      }, parsers) {
        override fun close() {
          closeAndGetFuture().whenComplete { _, _ -> deferredRootEvents.forEach { myBuildProgressListener.onEvent(buildId, it) } }
        }
      }
      var isBuildException = false
      myCurrentReader = myRootReader
      lineProcessor = object : LineProcessor() {
        override fun process(line: String) {
          val cleanLine = removeLoggerPrefix(line)
          if (cleanLine.startsWith("> Task :")) {
            isBuildException = false
            val taskName = cleanLine.removePrefix("> Task ").substringBefore(' ')
            myCurrentReader = tasksOutputReaders[taskName] ?: myRootReader
          }
          else if (cleanLine.startsWith("> Configure") ||
                   cleanLine.startsWith("FAILURE: Build failed") ||
                   cleanLine.startsWith("CONFIGURE SUCCESSFUL") ||
                   cleanLine.startsWith("BUILD SUCCESSFUL")) {
            isBuildException = false
            myCurrentReader = myRootReader
          }
          if (cleanLine == "* Exception is:") isBuildException = true
          if (isBuildException && myCurrentReader == myRootReader) return

          myCurrentReader.appendln(cleanLine)
          if (myCurrentReader != myRootReader) {
            val parentEventId = myCurrentReader.parentEventId
            myBuildProgressListener.onEvent(buildId, OutputBuildEventImpl(parentEventId, line + '\n', stdOut))
          }
        }
      }
    }

    override fun onEvent(buildId: Any, event: BuildEvent) {
      myBuildProgressListener.onEvent(buildId, event)
      if (event is StartEvent && event.parentId == buildId) {
        tasksOutputReaders[event.message]?.close() // multiple invocations of the same task during the build session

        val parentEventId = event.id
        tasksOutputReaders[event.message] = BuildOutputInstantReaderImpl(buildId, parentEventId, myBuildProgressListener, parsers)
        tasksEventIds[event.message] = parentEventId
      }
    }

    override fun close() {
      lineProcessor.close()
      tasksOutputReaders.forEach { (_, reader) -> reader.close() }
      myRootReader.close()
      tasksOutputReaders.clear()
    }

    override fun append(csq: CharSequence): Appendable {
      if (appendOutputToMainConsole) {
        myBuildProgressListener.onEvent(buildId, OutputBuildEventImpl(buildId, csq.toString(), stdOut))
      }
      lineProcessor.append(csq)
      return this
    }

    override fun append(csq: CharSequence, start: Int, end: Int): Appendable {
      if (appendOutputToMainConsole) {
        myBuildProgressListener.onEvent(buildId, OutputBuildEventImpl(buildId, csq.subSequence(start, end).toString(), stdOut))
      }
      lineProcessor.append(csq, start, end)
      return this
    }

    override fun append(c: Char): Appendable {
      if (appendOutputToMainConsole) {
        myBuildProgressListener.onEvent(buildId, OutputBuildEventImpl(buildId, c.toString(), stdOut))
      }
      lineProcessor.append(c)
      return this
    }

    private fun removeLoggerPrefix(line: String): String {
      val list = mutableListOf<String>()
      list += line.split(' ', limit = 3)
      if (list.size < 3) return line
      if (!list[1].startsWith('[') || !list[1].endsWith(']')) return line
      if (!list[2].startsWith('[')) return line
      if (!list[2].endsWith(']')) {
        val i = list[2].indexOf(']')
        if (i == -1) return line
        list[2] = list[2].substring(0, i + 1)
        if (!list[2].endsWith(']')) return line
      }

      try {
        LogLevel.valueOf(list[1].drop(1).dropLast(1))
      }
      catch (e: Exception) {
        return line
      }
      return line.drop(list.sumBy { it.length } + 2).trimStart()
    }

    private class BuildEventInvocationHandler(private val buildEvent: BuildEvent,
                                              private val parentEventId: Any) : InvocationHandler {
      override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        if (method?.name.equals("getParentId")) return parentEventId
        return method?.invoke(buildEvent, *args ?: arrayOfNulls<Any>(0))
      }

      companion object {
        fun wrap(buildEvent: BuildEvent, parentEventId: Any): BuildEvent {
          val classLoader = buildEvent.javaClass.classLoader
          val interfaces = ClassUtils.getAllInterfaces(buildEvent.javaClass)
            .filterIsInstance(Class::class.java)
            .toTypedArray()
          val invocationHandler = BuildEventInvocationHandler(buildEvent, parentEventId)
          return Proxy.newProxyInstance(classLoader, interfaces, invocationHandler) as BuildEvent
        }
      }
    }
  }
}

