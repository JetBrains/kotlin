// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

/**
 * [PrefixTreeMap] uses keys, that is specified by the paths.
 * @see PrefixTreeMap
 */
class PathPrefixTreeMap<V>(
  private val pathSeparator: String = "/",
  private val removeTrailingSeparator: Boolean = true
) : Map<String, V> {

  private val delegate = PrefixTreeMap<String, V>()

  override val size get() = delegate.size
  override val keys get() = delegate.keys.toPathKeys().toSet()
  override val values get() = delegate.values
  override val entries get() = delegate.entries.toPathEntries().toSet()
  override fun get(key: String) = delegate[key.toPrefixList()]
  override fun containsKey(key: String) = delegate.containsKey(key.toPrefixList())
  override fun containsValue(value: V) = delegate.containsValue(value)
  override fun isEmpty() = delegate.isEmpty()

  operator fun set(path: String, value: V) = delegate.set(path.toPrefixList(), value)
  fun remove(path: String) = delegate.remove(path.toPrefixList())

  fun getAllDescendants(path: String) = delegate.getAllDescendants(path.toPrefixList()).toPathEntries().toSet()
  fun getAllDescendantKeys(path: String) = delegate.getAllDescendantKeys(path.toPrefixList()).toPathKeys().toSet()
  fun getAllDescendantValues(path: String) = delegate.getAllDescendantValues(path.toPrefixList())
  fun getAllAncestors(path: String) = delegate.getAllAncestors(path.toPrefixList()).toPathEntries()
  fun getAllAncestorKeys(path: String) = delegate.getAllAncestorKeys(path.toPrefixList()).toPathKeys()
  fun getAllAncestorValues(path: String) = delegate.getAllAncestorValues(path.toPrefixList())

  private fun Iterable<Map.Entry<List<String>, V>>.toPathEntries() =
    map { PrefixTreeMap.Entry(it.key.joinToString(pathSeparator), it.value) }

  private fun Iterable<List<String>>.toPathKeys() = map { it.joinToString(pathSeparator) }

  private fun String.toPrefixList(): List<String> {
    val path = if (removeTrailingSeparator) removeSuffix(pathSeparator) else this
    return path.split(pathSeparator)
  }
}