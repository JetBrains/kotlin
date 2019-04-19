// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

/**
 * Map for fast finding values by the prefix of them keys
 */
interface PrefixTreeMap<K, V> {

  /**
   * Returns all keys for contained values
   */
  val keys: List<List<K>>

  /**
   * Returns all contained values
   */
  val values: List<V>

  /**
   * Gets value from the storage by the specified [path]
   *
   * @param path is key identificator
   * @return stored value with the specified [path], null if [path] not found
   */
  operator fun get(path: List<K>): V?

  /**
   * Sets value in the storage by the specified [path]
   *
   * @param path is key identificator
   * @param value is value to store
   * @return previous stored value with the specified [path], null if previous [path] not found
   */
  operator fun set(path: List<K>, value: V): V?

  /**
   * Removes value from the storage by the specified [path]
   *
   * @param path is key identificator
   * @return stored value with the specified [path], null if stored [path] not found
   */
  fun remove(path: List<K>): V?

  /**
   * Checks existence of [path] in the storage
   *
   * @param path is key identificator
   * @return result of existence checking
   */
  fun contains(path: List<K>): Boolean

  /**
   * Gets all values with a key, for which [path] is an ancestor
   *
   * @param path is key identificator
   * @return list of stored values, [emptyList] if [path] not found
   */
  fun getAllDescendants(path: List<K>): List<V>

  /**
   * Gets all contained ancestors
   *
   * @param path is key identificator
   * @return list of contained ancestors, `null` if [path] not found
   */
  fun getAllAncestorKeys(path: List<K>): List<List<K>>
}