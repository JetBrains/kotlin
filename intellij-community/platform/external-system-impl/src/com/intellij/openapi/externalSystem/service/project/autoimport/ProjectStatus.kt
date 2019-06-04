// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport


import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.project.autoimport.ProjectStatus.ProjectEvent.*
import com.intellij.openapi.externalSystem.service.project.autoimport.ProjectStatus.ProjectState.*
import java.lang.Long.max
import java.util.concurrent.atomic.AtomicReference

class ProjectStatus {
  private val LOG = Logger.getInstance(ProjectStatus::class.java)

  private var state = AtomicReference(Synchronized(-1) as ProjectState)

  fun isUpToDate() = when (state.get()) {
    is Modified, is Dirty -> false
    is Synchronized, is Reverted -> true
  }

  fun markDirty(stamp: Long): ProjectState {
    return update(Invalidate(stamp))
  }

  fun markModified(stamp: Long): ProjectState {
    return update(Modify(stamp))
  }

  fun markReverted(stamp: Long): ProjectState {
    return update(Revert(stamp))
  }

  fun markSynchronized(stamp: Long): ProjectState {
    return update(Synchronize(stamp))
  }

  fun update(event: ProjectEvent): ProjectState {
    if (LOG.isDebugEnabled) {
      LOG.debug("Event ${event::class.simpleName} is happened at ${event.stamp}")
    }
    return state.updateAndGet { currentState ->
      when (currentState) {
        is Synchronized -> when (event) {
          is Synchronize -> event.withFuture(currentState, ::Synchronized)
          is Invalidate -> event.ifFuture(currentState, ::Dirty)
          is Modify -> event.ifFuture(currentState, ::Modified)
          is Revert -> event.ifFuture(currentState, ::Reverted)
        }
        is Dirty -> when (event) {
          is Synchronize -> event.ifFuture(currentState, ::Synchronized)
          is Invalidate -> event.withFuture(currentState, ::Dirty)
          is Modify -> event.withFuture(currentState, ::Dirty)
          is Revert -> event.withFuture(currentState, ::Dirty)
        }
        is Modified -> when (event) {
          is Synchronize -> event.ifFuture(currentState, ::Synchronized)
          is Invalidate -> event.withFuture(currentState, ::Dirty)
          is Modify -> event.withFuture(currentState, ::Modified)
          is Revert -> event.ifFuture(currentState, ::Reverted)
        }
        is Reverted -> when (event) {
          is Synchronize -> event.ifFuture(currentState, ::Synchronized)
          is Invalidate -> event.withFuture(currentState, ::Dirty)
          is Modify -> event.ifFuture(currentState, ::Modified)
          is Revert -> event.withFuture(currentState, ::Reverted)
        }
      }
    }
  }

  private fun ProjectEvent.withFuture(state: ProjectState, action: (Long) -> ProjectState): ProjectState {
    return action(max(stamp, state.stamp))
  }

  private fun ProjectEvent.ifFuture(state: ProjectState, action: (Long) -> ProjectState): ProjectState {
    return if (stamp > state.stamp) action(stamp) else state
  }

  sealed class ProjectEvent(val stamp: Long) {
    class Synchronize(stamp: Long) : ProjectEvent(stamp)

    class Invalidate(stamp: Long) : ProjectEvent(stamp)

    class Modify(stamp: Long) : ProjectEvent(stamp)

    class Revert(stamp: Long) : ProjectEvent(stamp)
  }

  sealed class ProjectState(val stamp: Long) {
    class Synchronized(stamp: Long) : ProjectState(stamp)

    class Dirty(stamp: Long) : ProjectState(stamp)

    class Modified(stamp: Long) : ProjectState(stamp)

    class Reverted(stamp: Long) : ProjectState(stamp)
  }
}
