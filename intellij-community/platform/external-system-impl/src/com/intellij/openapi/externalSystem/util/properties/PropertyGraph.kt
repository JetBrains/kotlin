// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.properties

import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class PropertyGraph {
  private val inPropagation = AtomicBoolean(false)
  private val propagationListeners = CopyOnWriteArrayList<() -> Unit>()
  private val properties = ConcurrentHashMap<ObservableClearableProperty<*>, PropertyNode>()
  private val dependencies = ConcurrentHashMap<PropertyNode, CopyOnWriteArrayList<Dependency<*>>>()

  fun <T> dependsOn(child: ObservableClearableProperty<T>, parent: ObservableClearableProperty<*>, update: () -> T) {
    val childNode = properties[child] ?: throw IllegalArgumentException("Unregistered child property")
    val parentNode = properties[parent] ?: throw IllegalArgumentException("Unregistered parent property")
    dependencies.putIfAbsent(parentNode, CopyOnWriteArrayList())
    val children = dependencies.getValue(parentNode)
    children.add(Dependency(childNode, child, update))
  }

  fun afterPropagation(listener: () -> Unit) {
    propagationListeners.add(listener)
  }

  fun register(property: ObservableClearableProperty<*>) {
    val node = PropertyNode()
    properties[property] = node
    property.afterChange {
      if (!inPropagation.get()) {
        node.isPropagationBlocked = true
      }
    }
    property.afterReset {
      node.isPropagationBlocked = false
    }
    property.afterChange {
      inPropagation.withLockIfCan {
        node.inPropagation.withLockIfCan {
          propagateChange(node)
        }
        propagationListeners.forEach { it() }
      }
    }
  }

  private fun propagateChange(parent: PropertyNode) {
    val dependencies = dependencies[parent] ?: return
    for (dependency in dependencies) {
      val child = dependency.node
      if (child.isPropagationBlocked) continue
      child.inPropagation.withLockIfCan {
        dependency.applyUpdate()
        propagateChange(child)
      }
    }
  }

  @TestOnly
  fun isPropagationBlocked(property: ObservableClearableProperty<*>) =
    properties.getValue(property).isPropagationBlocked

  private inner class PropertyNode {
    @Volatile
    var isPropagationBlocked = false
    val inPropagation = AtomicBoolean(false)
  }

  private class Dependency<T>(val node: PropertyNode, private val property: ObservableClearableProperty<T>, private val update: () -> T) {
    fun applyUpdate() {
      property.set(update())
    }
  }

  companion object {
    private inline fun AtomicBoolean.withLockIfCan(action: () -> Unit) {
      if (!compareAndSet(false, true)) return
      try {
        action()
      }
      finally {
        set(false)
      }
    }
  }
}