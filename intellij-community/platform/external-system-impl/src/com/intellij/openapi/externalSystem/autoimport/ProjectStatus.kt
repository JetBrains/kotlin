// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport


import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType.INTERNAL
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ProjectEvent.*
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ProjectState.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

class ProjectStatus(private val debugName: String? = null) {
  private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

  private var state = AtomicReference(Synchronized(-1) as ProjectState)

  fun isDirty() = state.get() is Dirty

  fun isUpToDate() = when (state.get()) {
    is Modified, is Dirty, is Broken -> false
    is Synchronized, is Reverted -> true
  }

  fun getModificationType() = when (val state = state.get()) {
    is Dirty -> state.type
    is Modified -> state.type
    else -> null
  }

  fun markBroken(stamp: Long): ProjectState {
    return update(Break(stamp))
  }

  fun markDirty(stamp: Long, type: ModificationType = INTERNAL): ProjectState {
    return update(Invalidate(stamp, type))
  }

  fun markModified(stamp: Long, type: ModificationType = INTERNAL): ProjectState {
    return update(Modify(stamp, type))
  }

  fun markReverted(stamp: Long): ProjectState {
    return update(Revert(stamp))
  }

  fun markSynchronized(stamp: Long): ProjectState {
    return update(Synchronize(stamp))
  }

  fun update(event: ProjectEvent): ProjectState {
    if (LOG.isDebugEnabled) {
      val debugPrefix = if (debugName == null) "" else "$debugName: "
      val eventName = event::class.simpleName
      val state = state.get()
      val stateName = state::class.java.simpleName
      LOG.debug("${debugPrefix}Event $eventName is happened at ${event.stamp}. Current state $stateName is changed at ${state.stamp}")
    }
    val newState = state.updateAndGet { currentState ->
      when (currentState) {
        is Synchronized -> when (event) {
          is Synchronize -> event.withFuture(currentState, ::Synchronized)
          is Invalidate -> event.ifFuture(currentState) { Dirty(it, event.type) }
          is Modify -> event.ifFuture(currentState) { Modified(it, event.type) }
          is Revert -> event.ifFuture(currentState, ::Reverted)
          is Break -> event.ifFuture(currentState, ::Broken)
        }
        is Dirty -> when (event) {
          is Synchronize -> event.ifFuture(currentState, ::Synchronized)
          is Invalidate -> event.withFuture(currentState) { Dirty(it, currentState.type.merge(event.type)) }
          is Modify -> event.withFuture(currentState) { Dirty(it, currentState.type.merge(event.type)) }
          is Revert -> event.withFuture(currentState) { Dirty(it, currentState.type) }
          is Break -> event.withFuture(currentState) { Dirty(it, currentState.type) }
        }
        is Modified -> when (event) {
          is Synchronize -> event.ifFuture(currentState, ::Synchronized)
          is Invalidate -> event.withFuture(currentState) { Dirty(it, currentState.type.merge(event.type)) }
          is Modify -> event.withFuture(currentState) { Modified(it, currentState.type.merge(event.type)) }
          is Revert -> event.ifFuture(currentState, ::Reverted)
          is Break -> event.withFuture(currentState) { Dirty(it, currentState.type) }
        }
        is Reverted -> when (event) {
          is Synchronize -> event.ifFuture(currentState, ::Synchronized)
          is Invalidate -> event.withFuture(currentState) { Dirty(it, event.type) }
          is Modify -> event.ifFuture(currentState) { Modified(it, event.type) }
          is Revert -> event.withFuture(currentState, ::Reverted)
          is Break -> event.ifFuture(currentState, ::Broken)
        }
        is Broken -> when (event) {
          is Synchronize -> event.ifFuture(currentState, ::Synchronized)
          is Invalidate -> event.withFuture(currentState) { Dirty(it, event.type) }
          is Modify -> event.withFuture(currentState) { Dirty(it, event.type) }
          is Revert -> event.withFuture(currentState, ::Broken)
          is Break -> event.withFuture(currentState, ::Broken)
        }
      }
    }
    if (LOG.isDebugEnabled) {
      val debugPrefix = if (debugName == null) "" else "$debugName: "
      val eventName = event::class.simpleName
      val state = state.get()
      val stateName = state::class.java.simpleName
      LOG.debug("${debugPrefix}State is $stateName at ${state.stamp} after event $eventName that happen at ${event.stamp}.")
    }
    return newState
  }

  private fun ProjectEvent.withFuture(state: ProjectState, action: (Long) -> ProjectState): ProjectState {
    return action(max(stamp, state.stamp))
  }

  private fun ProjectEvent.ifFuture(state: ProjectState, action: (Long) -> ProjectState): ProjectState {
    return if (stamp > state.stamp) action(stamp) else state
  }

  enum class ModificationType {
    EXTERNAL, INTERNAL;

    fun merge(other: ModificationType): ModificationType {
      return when (this) {
        INTERNAL -> INTERNAL
        EXTERNAL -> other
      }
    }
  }

  sealed class ProjectEvent(val stamp: Long) {
    class Synchronize(stamp: Long) : ProjectEvent(stamp)

    class Invalidate(stamp: Long, val type: ModificationType) : ProjectEvent(stamp)

    class Modify(stamp: Long, val type: ModificationType) : ProjectEvent(stamp)

    class Revert(stamp: Long) : ProjectEvent(stamp)

    class Break(stamp: Long) : ProjectEvent(stamp)
  }

  sealed class ProjectState(val stamp: Long) {
    class Synchronized(stamp: Long) : ProjectState(stamp)

    class Dirty(stamp: Long, val type: ModificationType) : ProjectState(stamp)

    class Modified(stamp: Long, val type: ModificationType) : ProjectState(stamp)

    class Reverted(stamp: Long) : ProjectState(stamp)

    class Broken(stamp: Long) : ProjectState(stamp)
  }
}
