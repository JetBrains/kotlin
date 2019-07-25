// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import com.intellij.util.containers.FList
import gnu.trove.THashMap
import gnu.trove.TObjectHashingStrategy
import kotlin.collections.ArrayList

open class PrefixTreeMapImpl<K, V>(private val strategy: TObjectHashingStrategy<K>? = null) : PrefixTreeMap<K, V> {

  private val root = Node()

  override val keys: List<List<K>>
    get() = root.getKeys()

  override val values: List<V>
    get() = root.getValues()

  override operator fun get(path: List<K>) = root.get(path.toFList())?.getValue()

  override operator fun set(path: List<K>, value: V) = root.put(path.toFList(), value)

  override fun remove(path: List<K>) = root.remove(path.toFList())

  override fun contains(path: List<K>) = root.contains(path.toFList())

  override fun getAllDescendants(path: List<K>) = root.get(path.toFList())?.getAllDescendants() ?: emptyList()

  override fun getAllAncestorKeys(path: List<K>) = root.getAllAncestorKeys(path.toFList())

  private inner class Node {
    private val children = strategy?.let { THashMap<K, Node>(it) } ?: THashMap()
    private val isLeaf get() = children.isEmpty
    private val isInvalid get() = isLeaf && !isPresent
    private var isPresent: Boolean = false
    private var value: V? = null

    fun getValue() = value

    fun getKeys(): List<FList<K>> {
      val keys = children.flatMap { it.value.getKeys().map { list -> list.prepend(it.key) } }.toMutableList()
      if (isPresent) keys.add(FList.emptyList())
      return keys
    }

    fun getValues(): List<V> {
      val values = children.values.flatMap { it.getValues() }.toMutableList()
      @Suppress("UNCHECKED_CAST")
      if (isPresent) values.add(value as V)
      return values
    }

    fun put(path: FList<K>, value: V): V? {
      val (head, tail) = path
      val child = children.getOrPut(head) { Node() }
      if (tail.isEmpty()) {
        val previousValue = child.value
        child.value = value
        child.isPresent = true
        return previousValue
      }
      else return child.put(tail, value)
    }

    fun remove(path: FList<K>): V? {
      val (head, tail) = path
      val child = children[head] ?: return null
      if (tail.isEmpty()) {
        val value = child.value
        child.value = null
        child.isPresent = false
        return value
      }
      else {
        val result = child.remove(tail)
        if (child.isInvalid) {
          children.remove(head)
        }
        return result
      }
    }

    fun contains(path: FList<K>): Boolean {
      val (head, tail) = path
      val child = children[head] ?: return false
      return when {
        tail.isEmpty() -> child.isPresent
        else -> child.contains(tail)
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

    fun getAllDescendants(): List<V> {
      val result = children.flatMap { it.value.getAllDescendants() }.toMutableList()
      @Suppress("UNCHECKED_CAST")
      if (isPresent) result.add(value as V)
      return result
    }

    fun getAllAncestorKeys(path: FList<K>): List<FList<K>> {
      val result = ArrayList<FList<K>>()
      if (isPresent) result.add(FList.emptyList())
      if (path.isEmpty()) return result
      val (head, tail) = path
      val child = children[head] ?: return result
      result.addAll(child.getAllAncestorKeys(tail).map { list -> list.prepend(head) })
      return result
    }
  }

  companion object {
    private operator fun <E> FList<E>.component1() = head

    private operator fun <E> FList<E>.component2() = tail

    private fun <E> List<E>.toFList() = FList.createFromReversed(asReversed())
  }
}