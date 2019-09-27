// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import com.intellij.util.containers.FList

/**
 * Map for fast finding values by the prefix of thier keys
 */
class PrefixTreeMap<K, V> : Map<List<K>, V> {

  /**
   * Gets all descendant entries for [key]
   *
   * @return descendant entries, [emptyList] if [key] not found
   */
  fun getAllDescendants(key: List<K>) = root.getAllDescendants(key.toFList()).toSet()

  /**
   * Gets all ancestor entries for [key]
   *
   * @return ancestor entries, [emptyList] if [key] not found
   */
  fun getAllAncestors(key: List<K>) = root.getAllAncestors(key.toFList()).toList()

  fun getAllDescendantKeys(key: List<K>) = root.getAllDescendants(key.toFList()).map { it.key }.toSet()
  fun getAllDescendantValues(key: List<K>) = root.getAllDescendants(key.toFList()).map { it.value }.toSet()
  fun getAllAncestorKeys(key: List<K>) = root.getAllAncestors(key.toFList()).map { it.key }.toList()
  fun getAllAncestorValues(key: List<K>) = root.getAllAncestors(key.toFList()).map { it.value }.toList()

  private val root = Node()

  override val size get() = root.size
  override val keys get() = root.getEntries().map { it.key }.toSet()
  override val values get() = root.getEntries().map { it.value }.toList()
  override val entries get() = root.getEntries().toSet()
  override fun get(key: List<K>) = root.get(key.toFList())?.value?.getOrNull()
  override fun containsKey(key: List<K>) = root.containsKey(key.toFList())
  override fun containsValue(value: V) = values.any { it == value }
  override fun isEmpty() = root.isEmpty

  operator fun set(path: List<K>, value: V) = root.put(path.toFList(), value).getOrNull()
  fun remove(path: List<K>) = root.remove(path.toFList()).getOrNull()

  private inner class Node {
    private val children = LinkedHashMap<K, Node>()

    val isLeaf get() = children.isEmpty()
    val isEmpty get() = isLeaf && !value.isPresent

    var size: Int = 0
      private set

    var value = Value.empty<V>()
      private set

    private fun calculateCurrentSize() =
      children.values.sumBy { it.size } + if (value.isPresent) 1 else 0

    private fun getAndSet(value: Value<V>) =
      this.value.also {
        this.value = value
        size = calculateCurrentSize()
      }

    fun put(path: FList<K>, value: V): Value<V> {
      val (head, tail) = path
      val child = children.getOrPut(head) { Node() }
      val previousValue = when {
        tail.isEmpty() -> child.getAndSet(Value.of(value))
        else -> child.put(tail, value)
      }
      size = calculateCurrentSize()
      return previousValue
    }

    fun remove(path: FList<K>): Value<V> {
      val (head, tail) = path
      val child = children[head] ?: return Value.EMPTY
      val value = when {
        tail.isEmpty() -> child.getAndSet(Value.EMPTY)
        else -> child.remove(tail)
      }
      if (child.isEmpty) children.remove(head)
      size = calculateCurrentSize()
      return value
    }

    fun containsKey(path: FList<K>): Boolean {
      val (head, tail) = path
      val child = children[head] ?: return false
      return when {
        tail.isEmpty() -> child.value.isPresent
        else -> child.containsKey(tail)
      }
    }

    fun get(path: FList<K>): Node? {
      val (head, tail) = path
      val child = children[head] ?: return null
      return when {
        tail.isEmpty() -> child
        else -> child.get(tail)
      }
    }

    fun getEntries(): Sequence<Map.Entry<FList<K>, V>> {
      return sequence {
        if (value.isPresent) {
          yield(Entry(FList.emptyList(), value.get()))
        }
        for ((key, child) in children) {
          for ((path, value) in child.getEntries()) {
            yield(Entry(path.prepend(key), value))
          }
        }
      }
    }

    fun getAllAncestors(path: FList<K>): Sequence<Map.Entry<FList<K>, V>> {
      return sequence {
        if (value.isPresent) {
          yield(Entry(FList.emptyList(), value.get()))
        }
        if (path.isEmpty()) return@sequence
        val (head, tail) = path
        val child = children[head] ?: return@sequence
        for ((relative, value) in child.getAllAncestors(tail)) {
          yield(Entry(relative.prepend(head), value))
        }
      }
    }

    fun getAllDescendants(path: FList<K>) =
      root.get(path)?.getEntries()?.map { Entry(path + it.key, it.value) } ?: emptySequence()
  }

  data class Entry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

  private class Value<out T> private constructor(val isPresent: Boolean, private val value: Any?) {

    fun get(): T {
      @Suppress("UNCHECKED_CAST")
      if (isPresent) return value as T
      throw NoSuchElementException("No value present")
    }

    fun getOrNull() = if (isPresent) get() else null

    companion object {
      val EMPTY = Value<Nothing>(false, null)

      fun <T> empty() = EMPTY as Value<T>
      fun <T> of(value: T) = Value<T>(true, value)
    }
  }

  companion object {
    private operator fun <E> FList<E>.component1() = head

    private operator fun <E> FList<E>.component2() = tail

    private fun <E> List<E>.toFList() = FList.createFromReversed(asReversed())
  }
}